package br.com.presentya.mensageiro;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * WhatsApp Cloud API (Meta). Resposta automática a quem manda mensagem pelo QR Code; fora da janela de
 * 24 h desde a última mensagem da pessoa, a Meta só aceita modelo aprovado (WHATSAPP_MODELO).
 */
@Service
public class WhatsappService {
    private final String token;
    private final String numeroId;
    private final String verificacao;
    private final String segredoApp;
    private final String numero;
    private final String modelo;
    private final String modeloIdioma;
    private final RestClient http;

    public WhatsappService(@Value("${app.whatsapp.token:}") String token,
                           @Value("${app.whatsapp.phone-number-id:}") String numeroId,
                           @Value("${app.whatsapp.verify-token:}") String verificacao,
                           @Value("${app.whatsapp.app-secret:}") String segredoApp,
                           @Value("${app.whatsapp.numero:}") String numero,
                           @Value("${app.whatsapp.modelo:}") String modelo,
                           @Value("${app.whatsapp.modelo-idioma:pt_BR}") String modeloIdioma,
                           @Value("${app.whatsapp.versao:v23.0}") String versao,
                           @Value("${app.whatsapp.api-base:https://graph.facebook.com}") String base) {
        this.token = limpo(token);
        this.numeroId = limpo(numeroId);
        this.verificacao = limpo(verificacao);
        this.segredoApp = limpo(segredoApp);
        this.numero = limpo(numero).replaceAll("\\D", "");
        this.modelo = limpo(modelo);
        this.modeloIdioma = limpo(modeloIdioma);
        this.http = RestClient.builder().baseUrl(base + "/" + limpo(versao) + "/" + this.numeroId)
                .defaultHeader("Authorization", "Bearer " + this.token).build();
    }

    public boolean ativo() {
        return !token.isEmpty() && !numeroId.isEmpty();
    }

    public boolean temModelo() {
        return !modelo.isEmpty();
    }

    /** Número da loja para o link wa.me (o da API, se informado). */
    public String numero(String padrao) {
        return numero.isEmpty() ? padrao : numero;
    }

    /** Passo de verificação que a Meta faz ao cadastrar o webhook. */
    public boolean verificacaoConfere(String modo, String recebido) {
        return "subscribe".equals(modo) && !verificacao.isEmpty() && MessageDigest.isEqual(
                verificacao.getBytes(StandardCharsets.UTF_8), (recebido == null ? "" : recebido).getBytes(StandardCharsets.UTF_8));
    }

    /** Confere a assinatura X-Hub-Signature-256 (HMAC do corpo com o segredo do app). */
    public boolean assinaturaConfere(String corpo, String cabecalho) {
        if (segredoApp.isEmpty()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredoApp.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String esperado = "sha256=" + HexFormat.of().formatHex(mac.doFinal(corpo.getBytes(StandardCharsets.UTF_8)));
            return cabecalho != null && MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8), cabecalho.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return false;
        }
    }

    public String enviarTexto(String para, String texto) {
        return enviar(para, "text", Map.of("body", texto, "preview_url", false));
    }

    public String enviarFoto(String para, String url, String legenda) {
        return enviar(para, "image", Map.of("link", url, "caption", legenda));
    }

    public String enviarVideo(String para, String url, String legenda) {
        return enviar(para, "video", Map.of("link", url, "caption", legenda));
    }

    /** Modelo aprovado na Meta com uma variável {{1}} no corpo (o título da oferta). */
    public String enviarModelo(String para, String titulo) {
        return enviar(para, "template", Map.of("name", modelo, "language", Map.of("code", modeloIdioma),
                "components", List.of(Map.of("type", "body", "parameters", List.of(Map.of("type", "text", "text", titulo))))));
    }

    private String enviar(String para, String tipo, Map<String, Object> conteudo) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("messaging_product", "whatsapp");
        corpo.put("to", para);
        corpo.put("type", tipo);
        corpo.put(tipo, conteudo);
        try {
            http.post().uri("/messages").contentType(MediaType.APPLICATION_JSON).body(corpo).retrieve().toBodilessEntity();
            return null;
        } catch (RestClientResponseException e) {
            return "WhatsApp recusou (" + e.getStatusCode().value() + "): " + e.getResponseBodyAsString();
        } catch (RestClientException e) {
            return "WhatsApp fora do ar: " + e.getMessage();
        }
    }

    private static String limpo(String s) {
        return s == null ? "" : s.trim();
    }
}
