package br.com.presentya.pagamento;

import br.com.presentya.domain.FormaPagamento;
import br.com.presentya.domain.Pedido;
import br.com.presentya.domain.StatusPedido;
import java.util.Optional;

/** Pix vai para a conta Cora; crédito e débito continuam no Mercado Pago (ou no modo simulado). */
public class GatewayRoteado implements GatewayPagamento {
    private final CoraGateway pix;
    private final GatewayPagamento cartao;

    public GatewayRoteado(CoraGateway pix, GatewayPagamento cartao) {
        this.pix = pix;
        this.cartao = cartao;
    }

    private GatewayPagamento de(Pedido pedido) {
        return pedido.getFormaPagamento() == FormaPagamento.PIX ? pix : cartao;
    }

    public void registrarAvisoCora() {
        pix.registrarAviso();
    }

    @Override
    public boolean simulado() {
        return cartao.simulado();
    }

    @Override
    public boolean simulado(Pedido pedido) {
        return de(pedido).simulado();
    }

    @Override
    public void iniciar(Pedido pedido) {
        de(pedido).iniciar(pedido);
    }

    /** Pedidos de Pix feitos antes da Cora (id do Mercado Pago) continuam sendo consultados no Mercado Pago. */
    @Override
    public Optional<StatusPedido> consultar(Pedido pedido) {
        String id = pedido.getGatewayPagamentoId();
        return (id != null && id.startsWith("inv_") ? pix : cartao).consultar(pedido);
    }

    @Override
    public Optional<Notificacao> lerNotificacao(String pagamentoId) {
        return (pagamentoId.startsWith("inv_") ? pix : cartao).lerNotificacao(pagamentoId);
    }
}
