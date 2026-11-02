package br.com.presentya.web;

import br.com.presentya.domain.ContatoMensageiro.Canal;
import br.com.presentya.mensageiro.MensageiroService;
import br.com.presentya.mensageiro.TelegramService;
import br.com.presentya.mensageiro.WhatsappService;
import br.com.presentya.service.ConfiguracaoService;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Avisos (webhooks) do Telegram e do WhatsApp quando alguém manda mensagem para a loja. */
@RestController
@RequestMapping("/api")
public class MensageiroController {
    private final MensageiroService mensageiro;
    private final TelegramService telegram;
    private final WhatsappService whatsapp;
    private final ConfiguracaoService config;

    public MensageiroController(MensageiroService mensageiro, TelegramService telegram, WhatsappService whatsapp,
                                ConfiguracaoService config) {
        this.mensageiro = mensageiro;
        this.telegram = telegram;
        this.whatsapp = whatsapp;
        this.config = config;
    }

    /** Botões "Receber no WhatsApp/Telegram" da página de ofertas. */
    @GetMapping("/canais")
    public Map<String, String> canais() {
        return mensageiro.links(config.valor("whatsapp"));
    }

    @PostMapping("/telegram/webhook")
    public ResponseEntity<Void> telegram(@RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String segredo,
                                         @RequestBody Map<String, Object> aviso) {
        if (!telegram.ativo() || segredo == null || !MessageDigest.isEqual(segredo.getBytes(StandardCharsets.UTF_8),
                telegram.segredoWebhook().getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (aviso.get("message") instanceof Map<?, ?> msg && msg.get("chat") instanceof Map<?, ?> chat && chat.get("id") != null) {
            String texto = msg.get("text") == null ? "" : msg.get("text").toString().strip();
            String nome = msg.get("from") instanceof Map<?, ?> de && de.get("first_name") != null ? de.get("first_name").toString() : null;
            // "/start qrcode" = veio do QR Code da vitrine; outras mensagens também reenviam as ofertas.
            String origem = texto.startsWith("/start") ? (texto.contains("qrcode") ? "qrcode" : "telegram") : "telegram";
            String comando = texto.startsWith("/start") ? "" : texto;
            mensageiro.recebido(Canal.TELEGRAM, new java.math.BigDecimal(chat.get("id").toString()).toPlainString(), nome, comando, origem);
        }
        return ResponseEntity.ok().build();
    }

    /** Verificação que a Meta faz ao cadastrar o webhook (devolve o "challenge"). */
    @GetMapping(value = "/whatsapp/webhook", produces = "text/plain")
    public ResponseEntity<String> verificarWhatsapp(@RequestParam(value = "hub.mode", required = false) String modo,
                                                    @RequestParam(value = "hub.verify_token", required = false) String token,
                                                    @RequestParam(value = "hub.challenge", required = false) String desafio) {
        return whatsapp.verificacaoConfere(modo, token) ? ResponseEntity.ok(desafio) : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping("/whatsapp/webhook")
    public ResponseEntity<Void> whatsapp(@RequestHeader(value = "X-Hub-Signature-256", required = false) String assinatura,
                                         @RequestBody String corpo) {
        if (!whatsapp.ativo() || !whatsapp.assinaturaConfere(corpo, assinatura)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Map<String, Object> aviso = JsonParserFactory.getJsonParser().parseMap(corpo);
        for (Object e : lista(aviso.get("entry"))) {
            for (Object mudanca : lista(mapa(e).get("changes"))) {
                Map<?, ?> valor = mapa(mapa(mudanca).get("value"));
                String nome = null;
                for (Object c : lista(valor.get("contacts"))) {
                    Object n = mapa(mapa(c).get("profile")).get("name");
                    if (n != null) nome = n.toString();
                }
                for (Object m : lista(valor.get("messages"))) {
                    Map<?, ?> msg = mapa(m);
                    if (msg.get("from") == null) continue;
                    String texto = textoDa(msg);
                    String origem = texto.toLowerCase().contains("vitrine") ? "qrcode" : "whatsapp";
                    mensageiro.recebido(Canal.WHATSAPP, msg.get("from").toString().replaceAll("\\D", ""), nome,
                            texto.toLowerCase().contains("quero receber as ofertas") ? "" : texto, origem);
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    private static String textoDa(Map<?, ?> msg) {
        Object t = mapa(msg.get("text")).get("body");
        if (t == null) t = mapa(msg.get("button")).get("text");
        if (t == null) t = mapa(mapa(msg.get("interactive")).get("button_reply")).get("title");
        return t == null ? "" : t.toString().strip();
    }

    private static Map<?, ?> mapa(Object o) {
        return o instanceof Map<?, ?> m ? m : Map.of();
    }

    private static List<?> lista(Object o) {
        return o instanceof List<?> l ? l : List.of();
    }
}
