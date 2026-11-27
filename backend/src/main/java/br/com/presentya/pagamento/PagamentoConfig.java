package br.com.presentya.pagamento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PagamentoConfig {
    private static final Logger log = LoggerFactory.getLogger(PagamentoConfig.class);

    @Bean
    GatewayPagamento gatewayPagamento(@Value("${pagamento.mercadopago.access-token:}") String token,
                                      @Value("${pagamento.cora.client-id:}") String coraClientId,
                                      @Value("${pagamento.cora.certificado:}") String coraCertificado,
                                      @Value("${pagamento.cora.chave-privada:}") String coraChave,
                                      @Value("${pagamento.cora.url:https://matls-clients.api.cora.com.br}") String coraUrl,
                                      @Value("${app.frontend-url}") String frontendUrl,
                                      @Value("${app.api-url}") String apiUrl) {
        String front = frontendUrl.split(",")[0].trim();
        GatewayPagamento cartao;
        if (token == null || token.isBlank()) {
            log.warn("MP_ACCESS_TOKEN não configurado: cartão em MODO SIMULADO (nenhuma cobrança real).");
            cartao = new SimuladoGateway(front);
        } else {
            log.info("Cartão via Mercado Pago ativado.");
            cartao = new MercadoPagoGateway(token.trim(), front, apiUrl);
        }
        if (coraClientId.isBlank()) {
            return cartao;
        }
        if (coraCertificado.isBlank() || coraChave.isBlank()) {
            log.error("CORA_CLIENT_ID configurado, mas faltam CORA_CERTIFICADO e/ou CORA_CHAVE_PRIVADA: Pix pela Cora desligado.");
            return cartao;
        }
        try {
            CoraGateway cora = new CoraGateway(coraClientId.trim(), coraCertificado, coraChave, coraUrl.trim(), apiUrl);
            log.info("Pix via Cora ativado.");
            return new GatewayRoteado(cora, cartao);
        } catch (RuntimeException e) {
            log.error("Certificado ou chave da Cora inválidos: Pix pela Cora desligado. {}", e.getMessage());
            return cartao;
        }
    }

    /** Cadastra o aviso de pagamento na Cora depois que a API sobe (sem atrasar a inicialização). */
    @Bean
    ApplicationListener<ApplicationReadyEvent> avisoCora(GatewayPagamento gateway) {
        return evento -> {
            if (gateway instanceof GatewayRoteado r) {
                Thread.ofVirtual().name("aviso-cora").start(r::registrarAvisoCora);
            }
        };
    }
}
