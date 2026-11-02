package br.com.presentya.mensageiro;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Robô do Telegram (gratuito): basta criar o bot no @BotFather e colocar o token em TELEGRAM_BOT_TOKEN.
 * Na subida o servidor registra sozinho o webhook, e o Telegram passa a avisar cada mensagem recebida.
 */
@Service
public class TelegramService {
    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);
    /** Resposta de envio quando a pessoa bloqueou o robô: o contato sai da lista. */
    public static final String BLOQUEADO = "bloqueado";

    private final String token;
    private final String apiUrl;
    private final RestClient http;
    private volatile String usuarioBot;

    public TelegramService(@Value("${app.telegram.token:}") String token, @Value("${app.api-url}") String apiUrl,
                           @Value("${app.telegram.api-base:https://api.telegram.org}") String base) {
        this.token = token == null ? "" : token.trim();
        this.apiUrl = apiUrl.replaceAll("/+$", "");
        this.http = RestClient.builder().baseUrl(base + "/bot" + this.token).build();
    }

    public boolean ativo() {
        return !token.isEmpty();
    }

    /** Segredo que o Telegram devolve no cabeçalho de cada aviso, para ninguém se passar por ele. */
    public String segredoWebhook() {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(("presentya-webhook:" + token).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h, 0, 24);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void configurar() {
        if (!ativo()) return;
        try {
            Map<?, ?> eu = http.get().uri("/getMe").retrieve().body(Map.class);
            usuarioBot = eu != null && eu.get("result") instanceof Map<?, ?> r ? String.valueOf(r.get("username")) : null;
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("url", apiUrl + "/api/telegram/webhook");
            corpo.put("secret_token", segredoWebhook());
            corpo.put("allowed_updates", List.of("message"));
            http.post().uri("/setWebhook").contentType(MediaType.APPLICATION_JSON).body(corpo).retrieve().toBodilessEntity();
            log.info("Robô do Telegram @{} pronto, recebendo mensagens em {}/api/telegram/webhook", usuarioBot, apiUrl);
        } catch (RestClientException e) {
            log.warn("Não foi possível configurar o robô do Telegram (confira TELEGRAM_BOT_TOKEN): {}", e.getMessage());
        }
    }

    public String usuarioBot() {
        return usuarioBot;
    }

    /** Link do QR Code: abre o robô e, ao tocar em "Iniciar", ele recebe "/start origem". */
    public String link(String origem) {
        return usuarioBot == null ? null : "https://t.me/" + usuarioBot + "?start=" + origem;
    }

    public String enviarTexto(String chatId, String html, String botao, String url) {
        Map<String, Object> corpo = base(chatId, botao, url);
        corpo.put("text", html);
        corpo.put("link_preview_options", Map.of("is_disabled", true));
        return chamar("/sendMessage", corpo);
    }

    public String enviarFoto(String chatId, String fotoUrl, String legenda, String botao, String url) {
        Map<String, Object> corpo = base(chatId, botao, url);
        corpo.put("photo", fotoUrl);
        corpo.put("caption", legenda);
        return chamar("/sendPhoto", corpo);
    }

    public String enviarVideo(String chatId, String videoUrl, String legenda, String botao, String url) {
        Map<String, Object> corpo = base(chatId, botao, url);
        corpo.put("video", videoUrl);
        corpo.put("caption", legenda);
        corpo.put("supports_streaming", true);
        return chamar("/sendVideo", corpo);
    }

    private static Map<String, Object> base(String chatId, String botao, String url) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("chat_id", chatId);
        corpo.put("parse_mode", "HTML");
        if (botao != null && url != null && url.startsWith("https://")) {
            corpo.put("reply_markup", Map.of("inline_keyboard", List.of(List.of(Map.of("text", botao, "url", url)))));
        }
        return corpo;
    }

    /** null = entregue; senão o motivo (ou BLOQUEADO). */
    private String chamar(String metodo, Map<String, Object> corpo) {
        try {
            http.post().uri(metodo).contentType(MediaType.APPLICATION_JSON).body(corpo).retrieve().toBodilessEntity();
            return null;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 403) return BLOQUEADO;
            return "Telegram recusou (" + e.getStatusCode().value() + "): " + e.getResponseBodyAsString();
        } catch (RestClientException e) {
            return "Telegram fora do ar: " + e.getMessage();
        }
    }

    /** O Telegram interpreta HTML nas mensagens: o texto da loja precisa ser escapado. */
    public static String html(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
