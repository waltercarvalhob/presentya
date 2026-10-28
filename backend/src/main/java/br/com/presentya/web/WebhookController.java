package br.com.presentya.web;

import br.com.presentya.service.PedidoService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Avisos de pagamento do Mercado Pago (cadastre esta URL no painel do Mercado Pago). */
@RestController
@RequestMapping("/api/pagamentos/webhook")
public class WebhookController {
    private final PedidoService pedidos;

    public WebhookController(PedidoService pedidos) {
        this.pedidos = pedidos;
    }

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> mercadoPago(@RequestParam Map<String, String> query,
                                            @RequestBody(required = false) Map<String, Object> corpo) {
        String tipo = query.getOrDefault("type", query.get("topic"));
        String id = query.getOrDefault("data.id", query.get("id"));
        if (corpo != null) {
            if (corpo.get("type") instanceof String t) tipo = t;
            if (corpo.get("data") instanceof Map<?, ?> d && d.get("id") != null) id = String.valueOf(d.get("id"));
        }
        if ("payment".equals(tipo) && id != null && id.matches("\\d{1,30}")) {
            pedidos.processarNotificacao(id);
        }
        return ResponseEntity.ok().build();
    }
}
