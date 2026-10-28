package br.com.presentya.pagamento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PagamentoConfig {
    private static final Logger log = LoggerFactory.getLogger(PagamentoConfig.class);

    @Bean
    GatewayPagamento gatewayPagamento(@Value("${pagamento.mercadopago.access-token:}") String token,
                                      @Value("${app.frontend-url}") String frontendUrl,
                                      @Value("${app.api-url}") String apiUrl) {
        String front = frontendUrl.split(",")[0].trim();
        if (token == null || token.isBlank()) {
            log.warn("MP_ACCESS_TOKEN não configurado: pagamentos em MODO SIMULADO (nenhuma cobrança real).");
            return new SimuladoGateway(front);
        }
        log.info("Pagamentos via Mercado Pago ativados.");
        return new MercadoPagoGateway(token.trim(), front, apiUrl);
    }
}
