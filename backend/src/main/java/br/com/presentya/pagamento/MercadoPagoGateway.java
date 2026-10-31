package br.com.presentya.pagamento;

import br.com.presentya.domain.FormaPagamento;
import br.com.presentya.domain.Pedido;
import br.com.presentya.domain.StatusPedido;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Mercado Pago: Pix pela API de pagamentos (QR Code na própria loja) e cartão de crédito/débito
 * pelo Checkout Pro (página segura do Mercado Pago). Documentação: https://www.mercadopago.com.br/developers
 */
public class MercadoPagoGateway implements GatewayPagamento {
    private static final Logger log = LoggerFactory.getLogger(MercadoPagoGateway.class);

    private final RestClient http;
    private final String frontendUrl;
    private final String apiUrl;

    public MercadoPagoGateway(String accessToken, String frontendUrl, String apiUrl) {
        this.http = RestClient.builder()
                .baseUrl("https://api.mercadopago.com")
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();
        this.frontendUrl = frontendUrl;
        this.apiUrl = apiUrl;
    }

    @Override
    public boolean simulado() {
        return false;
    }

    @Override
    public void iniciar(Pedido pedido) {
        try {
            if (pedido.getFormaPagamento() == FormaPagamento.PIX) iniciarPix(pedido);
            else iniciarCartao(pedido);
        } catch (RestClientException e) {
            log.error("Mercado Pago recusou o pedido {}: {}", pedido.getCodigo(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível iniciar o pagamento agora. Tente novamente em instantes.");
        }
    }

    private void iniciarPix(Pedido pedido) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("transaction_amount", pedido.getTotal());
        corpo.put("description", "Pedido " + pedido.getCodigo() + " - PresentYá");
        corpo.put("payment_method_id", "pix");
        corpo.put("external_reference", pedido.getCodigo());
        corpo.put("payer", Map.of("email", emailCliente(pedido), "first_name", nomeCliente(pedido)));
        if (publico(apiUrl)) corpo.put("notification_url", apiUrl + "/api/pagamentos/webhook/mercadopago");

        Map<?, ?> r = http.post().uri("/v1/payments")
                .header("X-Idempotency-Key", "pix-" + pedido.getCodigo())
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve().body(Map.class);
        Map<?, ?> dados = (Map<?, ?>) ((Map<?, ?>) r.get("point_of_interaction")).get("transaction_data");
        pedido.setGatewayPagamentoId(String.valueOf(r.get("id")));
        pedido.setPixCopiaCola((String) dados.get("qr_code"));
        pedido.setPixQrBase64((String) dados.get("qr_code_base64"));
    }

    private void iniciarCartao(Pedido pedido) {
        boolean credito = pedido.getFormaPagamento() == FormaPagamento.CREDITO;
        List<String> excluidos = credito
                ? List.of("debit_card", "prepaid_card", "ticket", "bank_transfer", "atm")
                : List.of("credit_card", "ticket", "bank_transfer", "atm");
        String retorno = frontendUrl + "/pedido/" + pedido.getCodigo();

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("items", List.of(Map.of(
                "id", pedido.getCodigo(),
                "title", "Pedido " + pedido.getCodigo() + " - PresentYá",
                "quantity", 1,
                "currency_id", "BRL",
                "unit_price", pedido.getTotal())));
        corpo.put("payer", Map.of("email", emailCliente(pedido), "name", nomeCliente(pedido)));
        corpo.put("external_reference", pedido.getCodigo());
        corpo.put("statement_descriptor", "PRESENTYA");
        corpo.put("back_urls", Map.of("success", retorno, "failure", retorno, "pending", retorno));
        corpo.put("payment_methods", Map.of(
                "excluded_payment_types", excluidos.stream().map(id -> Map.of("id", id)).toList(),
                "installments", credito ? 12 : 1));
        if (publico(frontendUrl)) corpo.put("auto_return", "approved");
        if (publico(apiUrl)) corpo.put("notification_url", apiUrl + "/api/pagamentos/webhook/mercadopago");

        Map<?, ?> r = http.post().uri("/checkout/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve().body(Map.class);
        pedido.setGatewayPagamentoId(String.valueOf(r.get("id")));
        pedido.setLinkPagamento((String) r.get("init_point"));
    }

    @Override
    public Optional<StatusPedido> consultar(Pedido pedido) {
        try {
            Map<?, ?> pagamento;
            if (pedido.getFormaPagamento() == FormaPagamento.PIX && pedido.getGatewayPagamentoId() != null) {
                pagamento = http.get().uri("/v1/payments/{id}", pedido.getGatewayPagamentoId())
                        .retrieve().body(Map.class);
            } else {
                Map<?, ?> busca = http.get()
                        .uri("/v1/payments/search?external_reference={ref}&sort=date_created&criteria=desc",
                                pedido.getCodigo())
                        .retrieve().body(Map.class);
                List<?> resultados = (List<?>) busca.get("results");
                if (resultados == null || resultados.isEmpty()) return Optional.empty();
                pagamento = (Map<?, ?>) resultados.get(0);
            }
            return traduzir((String) pagamento.get("status"));
        } catch (RestClientException e) {
            log.warn("Falha ao consultar pagamento do pedido {}: {}", pedido.getCodigo(), e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<Notificacao> lerNotificacao(String pagamentoId) {
        // Não confiamos no corpo do webhook: buscamos o pagamento direto no Mercado Pago.
        Map<?, ?> pagamento = http.get().uri("/v1/payments/{id}", pagamentoId).retrieve().body(Map.class);
        String referencia = (String) pagamento.get("external_reference");
        if (referencia == null) return Optional.empty();
        return traduzir((String) pagamento.get("status")).map(s -> new Notificacao(referencia, s));
    }

    private static Optional<StatusPedido> traduzir(String status) {
        if (status == null) return Optional.empty();
        return switch (status) {
            case "approved" -> Optional.of(StatusPedido.PAGO);
            case "cancelled", "refunded", "charged_back" -> Optional.of(StatusPedido.CANCELADO);
            default -> Optional.empty(); // pending, in_process, rejected (o cliente pode tentar de novo)
        };
    }

    /** O Mercado Pago só aceita URLs de retorno/aviso públicas (https). */
    private static boolean publico(String url) {
        return url != null && url.startsWith("https://");
    }

    /** Pedido de convidado não tem conta: usa os dados informados no checkout. */
    private static String emailCliente(Pedido pedido) {
        return pedido.getUsuario() != null ? pedido.getUsuario().getEmail() : pedido.getEmailConvidado();
    }

    private static String nomeCliente(Pedido pedido) {
        return pedido.getUsuario() != null ? pedido.getUsuario().getNome() : pedido.getNomeDestinatario();
    }
}
