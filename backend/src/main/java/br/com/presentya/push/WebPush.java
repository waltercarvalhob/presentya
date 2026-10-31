package br.com.presentya.push;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Notificação push do navegador feita só com o JDK: criptografia da mensagem (RFC 8291, "aes128gcm")
 * e assinatura do servidor (VAPID, RFC 8292). Chaves públicas trafegam como ponto P-256 sem compressão (65 bytes).
 */
public final class WebPush {
    private WebPush() {}

    private static final SecureRandom RANDOM = new SecureRandom();
    static final ECParameterSpec P256 = parametrosP256();

    private static ECParameterSpec parametrosP256() {
        return ((ECPublicKey) novoParDeChaves().getPublic()).getParams();
    }

    public static KeyPair novoParDeChaves() {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
            g.initialize(new ECGenParameterSpec("secp256r1"), RANDOM);
            return g.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] pontoBruto(ECPublicKey k) {
        byte[] r = new byte[65];
        r[0] = 4;
        System.arraycopy(fixo32(k.getW().getAffineX()), 0, r, 1, 32);
        System.arraycopy(fixo32(k.getW().getAffineY()), 0, r, 33, 32);
        return r;
    }

    public static ECPublicKey chavePublica(byte[] bruto) {
        if (bruto.length != 65 || bruto[0] != 4) throw new IllegalArgumentException("Chave pública inválida");
        byte[] x = new byte[32], y = new byte[32];
        System.arraycopy(bruto, 1, x, 0, 32);
        System.arraycopy(bruto, 33, y, 0, 32);
        try {
            ECPoint p = new ECPoint(new BigInteger(1, x), new BigInteger(1, y));
            return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(p, P256));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Chave pública inválida", e);
        }
    }

    public static ECPrivateKey chavePrivada(byte[] pkcs8) {
        try {
            return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Corpo da requisição para o serviço de push: cabeçalho aes128gcm + um único registro cifrado. */
    public static byte[] cifrar(byte[] mensagem, byte[] uaPublica, byte[] authSecret) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return cifrar(mensagem, uaPublica, authSecret, novoParDeChaves(), salt);
    }

    static byte[] cifrar(byte[] mensagem, byte[] uaPublica, byte[] authSecret, KeyPair servidor, byte[] salt) {
        try {
            byte[] asPublica = pontoBruto((ECPublicKey) servidor.getPublic());
            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(servidor.getPrivate());
            ka.doPhase(chavePublica(uaPublica), true);
            byte[] ecdh = ka.generateSecret();

            byte[] prkKey = hmac(authSecret, ecdh);
            byte[] ikm = hmac(prkKey, ascii("WebPush: info\0"), uaPublica, asPublica, new byte[] {1});
            byte[] prk = hmac(salt, ikm);
            byte[] cek = corta(hmac(prk, ascii("Content-Encoding: aes128gcm\0"), new byte[] {1}), 16);
            byte[] nonce = corta(hmac(prk, ascii("Content-Encoding: nonce\0"), new byte[] {1}), 12);

            byte[] claro = new byte[mensagem.length + 1];
            System.arraycopy(mensagem, 0, claro, 0, mensagem.length);
            claro[mensagem.length] = 2; // delimitador do último (e único) registro
            Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
            aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
            byte[] cifrado = aes.doFinal(claro);

            return ByteBuffer.allocate(16 + 4 + 1 + asPublica.length + cifrado.length)
                    .put(salt).putInt(4096).put((byte) asPublica.length).put(asPublica).put(cifrado).array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** JWT ES256 que identifica o servidor para o serviço de push do navegador. */
    public static String jwtVapid(String endpoint, String contato, ECPrivateKey privada, long expiraEm) {
        URI u = URI.create(endpoint);
        String aud = u.getScheme() + "://" + u.getHost() + (u.getPort() > 0 ? ":" + u.getPort() : "");
        String cabecalho = b64(ascii("{\"typ\":\"JWT\",\"alg\":\"ES256\"}"));
        String corpo = b64(ascii("{\"aud\":\"" + aud + "\",\"exp\":" + expiraEm + ",\"sub\":\"" + contato + "\"}"));
        try {
            Signature s = Signature.getInstance("SHA256withECDSAinP1363Format");
            s.initSign(privada);
            s.update(ascii(cabecalho + "." + corpo));
            return cabecalho + "." + corpo + "." + b64(s.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    static byte[] hmac(byte[] chave, byte[]... partes) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(chave, "HmacSHA256"));
        for (byte[] p : partes) mac.update(p);
        return mac.doFinal();
    }

    public static String b64(byte[] b) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    /** Aceita base64 "url" ou padrão, com ou sem "=" (cada navegador manda de um jeito). */
    public static byte[] b64(String s) {
        return Base64.getUrlDecoder().decode(s.trim().replace('+', '-').replace('/', '_').replace("=", ""));
    }

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] corta(byte[] b, int n) {
        byte[] r = new byte[n];
        System.arraycopy(b, 0, r, 0, n);
        return r;
    }

    private static byte[] fixo32(BigInteger n) {
        byte[] b = n.toByteArray();
        if (b.length == 32) return b;
        ByteArrayOutputStream r = new ByteArrayOutputStream(32);
        if (b.length > 32) r.write(b, b.length - 32, 32);
        else { r.writeBytes(new byte[32 - b.length]); r.writeBytes(b); }
        return r.toByteArray();
    }
}
