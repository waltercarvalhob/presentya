package br.com.presentya.pagamento;

import br.com.presentya.domain.Pedido;
import br.com.presentya.domain.StatusPedido;
import java.util.Optional;

/**
 * Ponto único de integração com o meio de pagamento. A loja nunca recebe nem guarda dados de cartão:
 * crédito e débito são pagos na página segura do gateway; Pix gera QR Code e "copia e cola".
 */
public interface GatewayPagamento {

    /** true quando não há credenciais configuradas e os pagamentos são apenas de teste. */
    boolean simulado();

    /** Prepara a cobrança e preenche no pedido o link de pagamento ou os dados do Pix. */
    void iniciar(Pedido pedido);

    /** Consulta o gateway: PAGO, CANCELADO ou vazio enquanto estiver pendente. */
    Optional<StatusPedido> consultar(Pedido pedido);

    /** Lê um pagamento avisado por webhook e diz a qual pedido ele pertence. */
    Optional<Notificacao> lerNotificacao(String pagamentoId);

    record Notificacao(String codigoPedido, StatusPedido status) {}
}
