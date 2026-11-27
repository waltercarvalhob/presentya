package br.com.presentya.pagamento;

import br.com.presentya.domain.Pedido;
import br.com.presentya.domain.StatusPedido;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.net.ssl.SSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Pix pela conta PJ da Cora (Integração Direta: certificado + chave privada + client_id, via mTLS).
 * O dinheiro cai direto na conta Cora. A Cora não cobra cartão pela API: cartão continua no Mercado Pago.
 * Documentação: https://developers.cora.com.br
 */
public class CoraGateway implements GatewayPagamento {
    private static final Logger log = LoggerFactory.getLogger(CoraGateway.class);
    private static final ZoneId FUSO = ZoneId.of("America/Fortaleza");
    private static final BigDecimal MINIMO = new BigDecimal("5.00");

    private final RestClient http;
    private final String clientId;
    private final String apiUrl;
    private String token;
    private Instant tokenVence = Instant.EPOCH;

    public CoraGateway(String clientId, String certificado, String chavePrivada, String baseUrl, String apiUrl) {
        SSLContext ssl = SslBundle.of(new PemSslStoreBundle(
                PemSslStoreDetails.forCertificate(pem(certificado)).withPrivateKey(pem(chavePrivada)), null))
                .createSslContext();
        HttpClient cliente = HttpClient.newBuilder().sslContext(ssl).connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
        fabrica.setReadTimeout(Duration.ofSeconds(20));
        this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(fabrica).build();
        this.clientId = clientId;
        this.apiUrl = apiUrl;
    }

    /** Aceita o conteúdo PEM (também com "\n" escrito numa linha só) ou o caminho do arquivo (Secret File do Render). */
    static String pem(String valor) {
        String v = valor.trim();
        if (v.contains("-----BEGIN")) return v.replace("\\n", "\n");
        try {
            return Files.readString(Path.of(v), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler o arquivo da Cora: " + v, e);
        }
    }

    @Override
    public boolean simulado() {
        return false;
    }

    @Override
    public void iniciar(Pedido pedido) {
        String doc = pedido.getDocumentoPagador();
        if (!documentoValido(doc)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um CPF válido para pagar com Pix");
        }
        if (pedido.getTotal().compareTo(MINIMO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pix a partir de R$ 5,00. Para valores menores, escolha cartão.");
        }
        long centavos = pedido.getTotal().movePointRight(2).longValueExact();

        Map<String, Object> cliente = new LinkedHashMap<>();
        cliente.put("name", limitar(nomeCliente(pedido), 60));
        String email = emailCliente(pedido);
        if (email != null) cliente.put("email", email);
        cliente.put("document", Map.of("identity", doc, "type", doc.length() == 11 ? "CPF" : "CNPJ"));

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("code", pedido.getCodigo());
        corpo.put("customer", cliente);
        corpo.put("services", List.of(Map.of("name", "Pedido " + pedido.getCodigo(),
                "description", "Presentes PresentYá", "amount", centavos)));
        corpo.put("payment_terms", Map.of("due_date", LocalDate.now(FUSO).plusDays(1).toString()));
        corpo.put("payment_forms", List.of("PIX"));

        try {
            Map<?, ?> r = http.post().uri("/v2/invoices/")
                    .header("Authorization", "Bearer " + token())
                    .header("Idempotency-Key", UUID.nameUUIDFromBytes(("pix-" + pedido.getCodigo())
                            .getBytes(StandardCharsets.UTF_8)).toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve().body(Map.class);
            Object emv = r.get("pix") instanceof Map<?, ?> pix ? pix.get("emv") : null;
            if (!(emv instanceof String codigo) || codigo.isBlank()) {
                throw new IllegalStateException("A Cora não devolveu o código Pix (confira se a conta tem chave Pix)");
            }
            pedido.setGatewayPagamentoId(String.valueOf(r.get("id")));
            pedido.setPixCopiaCola(codigo);
        } catch (RestClientException | IllegalStateException e) {
            log.error("Cora recusou o Pix do pedido {}: {}", pedido.getCodigo(), detalhe(e));
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível gerar o Pix agora. Tente novamente em instantes ou escolha cartão.");
        }
    }

    @Override
    public Optional<StatusPedido> consultar(Pedido pedido) {
        String id = pedido.getGatewayPagamentoId();
        if (id == null || !id.startsWith("inv_")) return Optional.empty();
        return cobranca(id).flatMap(c -> traduzir(c.get("status")));
    }

    /** O aviso da Cora chega sem corpo: buscamos a cobrança direto na Cora para saber se foi paga. */
    @Override
    public Optional<Notificacao> lerNotificacao(String cobrancaId) {
        return cobranca(cobrancaId).flatMap(c -> c.get("code") instanceof String codigo
                ? traduzir(c.get("status")).map(s -> new Notificacao(codigo, s))
                : Optional.empty());
    }

    private Optional<Map<?, ?>> cobranca(String id) {
        if (!id.matches("inv_[A-Za-z0-9_-]{1,80}")) return Optional.empty();
        try {
            return Optional.ofNullable(http.get().uri("/v2/invoices/{id}", id)
                    .header("Authorization", "Bearer " + token())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().body(Map.class));
        } catch (RestClientException e) {
            log.warn("Falha ao consultar a cobrança {} na Cora: {}", id, detalhe(e));
            return Optional.empty();
        }
    }

    private static Optional<StatusPedido> traduzir(Object status) {
        if (!(status instanceof String s)) return Optional.empty();
        return switch (s) {
            case "PAID" -> Optional.of(StatusPedido.PAGO);
            case "CANCELLED", "CANCELED" -> Optional.of(StatusPedido.CANCELADO);
            default -> Optional.empty(); // OPEN, LATE, IN_PAYMENT, DRAFT
        };
    }

    /** Cadastra na Cora o aviso de "cobrança paga" para esta API (só se ainda não existir). */
    public void registrarAviso() {
        if (apiUrl == null || !apiUrl.startsWith("https://")) return;
        String url = apiUrl + "/api/pagamentos/webhook/cora";
        try {
            List<?> existentes = http.get().uri("/endpoints/")
                    .header("Authorization", "Bearer " + token())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().body(List.class);
            boolean ja = existentes != null && existentes.stream()
                    .anyMatch(e -> e instanceof Map<?, ?> m && url.equals(m.get("url")));
            if (ja) return;
            http.post().uri("/endpoints/")
                    .header("Authorization", "Bearer " + token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("url", url, "resource", "invoice", "trigger", "paid"))
                    .retrieve().toBodilessEntity();
            log.info("Aviso de pagamento da Cora cadastrado: {}", url);
        } catch (RestClientException e) {
            log.error("Não foi possível cadastrar o aviso de pagamento na Cora: {}", detalhe(e));
        }
    }

    /** O token vale 24 h; renovamos um pouco antes de vencer. */
    private synchronized String token() {
        if (token != null && Instant.now().isBefore(tokenVence)) return token;
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        Map<?, ?> r = http.post().uri("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve().body(Map.class);
        token = (String) r.get("access_token");
        long segundos = r.get("expires_in") instanceof Number n ? n.longValue() : 3600;
        tokenVence = Instant.now().plusSeconds(Math.max(60, segundos - 300));
        return token;
    }

    /** CPF (11 dígitos) ou CNPJ (14) com dígitos verificadores corretos. */
    public static boolean documentoValido(String doc) {
        if (doc == null || !doc.matches("\\d{11}|\\d{14}") || doc.chars().distinct().count() == 1) return false;
        if (doc.length() == 11) {
            return digito(doc, 9, new int[] {10, 9, 8, 7, 6, 5, 4, 3, 2})
                    && digito(doc, 10, new int[] {11, 10, 9, 8, 7, 6, 5, 4, 3, 2});
        }
        return digito(doc, 12, new int[] {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2})
                && digito(doc, 13, new int[] {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
    }

    private static boolean digito(String doc, int pos, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) soma += (doc.charAt(i) - '0') * pesos[i];
        int resto = soma % 11;
        return doc.charAt(pos) - '0' == (resto < 2 ? 0 : 11 - resto);
    }

    private static String detalhe(Exception e) {
        return e instanceof RestClientResponseException r
                ? r.getStatusCode() + " " + limitar(r.getResponseBodyAsString(), 500) : e.getMessage();
    }

    private static String limitar(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    private static String emailCliente(Pedido pedido) {
        return pedido.getUsuario() != null ? pedido.getUsuario().getEmail() : pedido.getEmailConvidado();
    }

    private static String nomeCliente(Pedido pedido) {
        return pedido.getUsuario() != null ? pedido.getUsuario().getNome() : pedido.getNomeDestinatario();
    }
}
