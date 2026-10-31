package br.com.presentya;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.presentya.push.WebPush;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPrivateKeySpec;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/** Criptografia das notificações: vetor oficial do RFC 8291 e decifragem como o navegador faz. */
class WebPushTests {

    @Test
    void bateComOExemploDoRfc8291() throws Exception {
        byte[] uaPublica = WebPush.b64("BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4");
        byte[] auth = WebPush.b64("BTBZMqHH6r4Tts7J_aSIgg");
        byte[] salt = WebPush.b64("DGv6ra1nlYgDCS1FRnbzlw");
        byte[] asPublica = WebPush.b64("BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8");
        KeyPair servidor = new KeyPair(WebPush.chavePublica(asPublica),
                privada(WebPush.b64("yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw")));

        byte[] corpo = cifrar("When I grow up, I want to be a watermelon".getBytes(StandardCharsets.UTF_8), uaPublica, auth, servidor, salt);

        assertEquals("DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPTpK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN",
                WebPush.b64(corpo));
    }

    @Test
    void navegadorConsegueDecifrar() throws Exception {
        KeyPair celular = WebPush.novoParDeChaves();
        byte[] uaPublica = WebPush.pontoBruto((ECPublicKey) celular.getPublic());
        byte[] auth = new byte[16];
        Arrays.fill(auth, (byte) 7);
        byte[] mensagem = "{\"title\":\"🎁 Oferta\",\"body\":\"Caneca 20% off\"}".getBytes(StandardCharsets.UTF_8);

        byte[] corpo = WebPush.cifrar(mensagem, uaPublica, auth);

        // Lado do navegador (RFC 8291 seção 3.4): lê o cabeçalho e refaz as chaves com a chave privada do celular.
        ByteBuffer bb = ByteBuffer.wrap(corpo);
        byte[] salt = new byte[16];
        bb.get(salt);
        assertEquals(4096, bb.getInt());
        byte[] asPublica = new byte[bb.get()];
        bb.get(asPublica);
        byte[] cifrado = new byte[bb.remaining()];
        bb.get(cifrado);

        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(celular.getPrivate());
        ka.doPhase(WebPush.chavePublica(asPublica), true);
        byte[] ikm = hmac(hmac(auth, ka.generateSecret()), cat("WebPush: info\0".getBytes(), uaPublica, asPublica, new byte[] {1}));
        byte[] prk = hmac(salt, ikm);
        byte[] cek = Arrays.copyOf(hmac(prk, cat("Content-Encoding: aes128gcm\0".getBytes(), new byte[] {1})), 16);
        byte[] nonce = Arrays.copyOf(hmac(prk, cat("Content-Encoding: nonce\0".getBytes(), new byte[] {1})), 12);
        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] claro = aes.doFinal(cifrado);

        assertEquals(2, claro[claro.length - 1]);
        assertArrayEquals(mensagem, Arrays.copyOf(claro, claro.length - 1));
    }

    @Test
    void assinaturaVapidConfere() throws Exception {
        KeyPair servidor = WebPush.novoParDeChaves();
        String jwt = WebPush.jwtVapid("https://fcm.googleapis.com/fcm/send/abc", "mailto:loja@teste.com",
                (ECPrivateKey) servidor.getPrivate(), 1_900_000_000L);
        String[] partes = jwt.split("\\.");
        String corpo = new String(WebPush.b64(partes[1]), StandardCharsets.UTF_8);
        assertTrue(corpo.contains("\"aud\":\"https://fcm.googleapis.com\""), corpo);

        Signature s = Signature.getInstance("SHA256withECDSAinP1363Format");
        s.initVerify(servidor.getPublic());
        s.update((partes[0] + "." + partes[1]).getBytes(StandardCharsets.US_ASCII));
        assertTrue(s.verify(WebPush.b64(partes[2])));
    }

    private static byte[] cifrar(byte[] m, byte[] ua, byte[] auth, KeyPair servidor, byte[] salt) throws Exception {
        Method metodo = WebPush.class.getDeclaredMethod("cifrar", byte[].class, byte[].class, byte[].class, KeyPair.class, byte[].class);
        metodo.setAccessible(true);
        return (byte[]) metodo.invoke(null, m, ua, auth, servidor, salt);
    }

    private static ECPrivateKey privada(byte[] escalar) throws Exception {
        java.security.spec.ECParameterSpec p256 = ((ECPublicKey) WebPush.novoParDeChaves().getPublic()).getParams();
        return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new ECPrivateKeySpec(new BigInteger(1, escalar), p256));
    }

    private static byte[] hmac(byte[] chave, byte[] dados) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(chave, "HmacSHA256"));
        return mac.doFinal(dados);
    }

    private static byte[] cat(byte[]... partes) {
        ByteBuffer b = ByteBuffer.allocate(Arrays.stream(partes).mapToInt(p -> p.length).sum());
        for (byte[] p : partes) b.put(p);
        return b.array();
    }
}
