package br.com.presentya.pagamento;

import br.com.presentya.domain.FormaPagamento;
import br.com.presentya.domain.Pedido;
import br.com.presentya.domain.StatusPedido;
import java.util.Optional;

/** Usado no desenvolvimento, sem MP_ACCESS_TOKEN. Nenhum dinheiro é movimentado. */
public class SimuladoGateway implements GatewayPagamento {
    private final String frontendUrl;

    public SimuladoGateway(String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Override
    public boolean simulado() {
        return true;
    }

    @Override
    public void iniciar(Pedido pedido) {
        pedido.setGatewayPagamentoId("SIM-" + pedido.getCodigo());
        if (pedido.getFormaPagamento() == FormaPagamento.PIX) {
            pedido.setPixCopiaCola("PIX-DE-TESTE-" + pedido.getCodigo() + "-NAO-PAGUE");
        } else {
            pedido.setLinkPagamento(frontendUrl + "/pagamento-simulado/" + pedido.getCodigo());
        }
    }

    @Override
    public Optional<StatusPedido> consultar(Pedido pedido) {
        return Optional.empty();
    }

    @Override
    public Optional<Notificacao> lerNotificacao(String pagamentoId) {
        return Optional.empty();
    }
}
