package br.com.presentya.dto;

import br.com.presentya.domain.Perfil;
import br.com.presentya.dto.PedidoDtos.PedidoDTO;
import java.math.BigDecimal;
import java.util.List;

public final class AdminDtos {
    private AdminDtos() {}

    public record AlterarUsuarioRequest(Perfil perfil, Boolean ativo) {}

    public record DashboardDTO(long produtosAtivos, long aguardandoPagamento, long pagos, long emProducao,
                               long enviados, long clientes, BigDecimal faturamento, List<PedidoDTO> ultimosPedidos) {}
}
