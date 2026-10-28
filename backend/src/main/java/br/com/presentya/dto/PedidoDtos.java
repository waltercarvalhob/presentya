package br.com.presentya.dto;

import br.com.presentya.domain.FormaPagamento;
import br.com.presentya.domain.ItemPedido;
import br.com.presentya.domain.Pedido;
import br.com.presentya.domain.StatusPedido;
import br.com.presentya.domain.TipoEntrega;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class PedidoDtos {
    private PedidoDtos() {}

    public record ItemRequest(
            @NotNull Long produtoId,
            @NotNull @Min(1) @Max(value = 50, message = "Máximo de 50 unidades por item") Integer quantidade,
            @Size(max = 500) String personalizacao) {}

    public record NovoPedidoRequest(
            @NotEmpty(message = "Seu carrinho está vazio") @Size(max = 30) List<@Valid ItemRequest> itens,
            @NotNull(message = "Escolha a forma de pagamento") FormaPagamento formaPagamento,
            @NotNull(message = "Escolha entrega ou retirada") TipoEntrega tipoEntrega,
            @Size(max = 120) String lojaRetirada,
            @NotBlank(message = "Informe o nome de quem recebe") @Size(max = 120) String nomeDestinatario,
            @NotBlank(message = "Informe um telefone") @Size(max = 30) String telefone,
            @Size(max = 10) String cep,
            @Size(max = 200) String endereco,
            @Size(max = 20) String numero,
            @Size(max = 120) String complemento,
            @Size(max = 120) String bairro,
            @Size(max = 120) String cidade,
            @Size(max = 2) String uf,
            @Size(max = 500) String mensagemCartao,
            @Size(max = 1000) String observacoes) {}

    public record ItemDTO(Long produtoId, String nomeProduto, BigDecimal precoUnitario, int quantidade,
                          String personalizacao, BigDecimal total) {
        static ItemDTO de(ItemPedido i) {
            return new ItemDTO(i.getProdutoId(), i.getNomeProduto(), i.getPrecoUnitario(), i.getQuantidade(),
                    i.getPersonalizacao(), i.total());
        }
    }

    public record PedidoDTO(Long id, String codigo, StatusPedido status, FormaPagamento formaPagamento,
                            TipoEntrega tipoEntrega, String lojaRetirada, BigDecimal subtotal, BigDecimal frete,
                            BigDecimal total, String nomeDestinatario, String telefone, String cep, String endereco,
                            String numero, String complemento, String bairro, String cidade, String uf,
                            String mensagemCartao, String observacoes, String linkPagamento, String pixCopiaCola,
                            String pixQrBase64, Instant criadoEm, Instant atualizadoEm, String clienteNome,
                            String clienteEmail, List<ItemDTO> itens, boolean pagamentoSimulado) {
        public static PedidoDTO de(Pedido p, boolean simulado) {
            return new PedidoDTO(p.getId(), p.getCodigo(), p.getStatus(), p.getFormaPagamento(), p.getTipoEntrega(),
                    p.getLojaRetirada(), p.getSubtotal(), p.getFrete(), p.getTotal(), p.getNomeDestinatario(),
                    p.getTelefone(), p.getCep(), p.getEndereco(), p.getNumero(), p.getComplemento(), p.getBairro(),
                    p.getCidade(), p.getUf(), p.getMensagemCartao(), p.getObservacoes(), p.getLinkPagamento(),
                    p.getPixCopiaCola(), p.getPixQrBase64(), p.getCriadoEm(), p.getAtualizadoEm(),
                    p.getUsuario().getNome(), p.getUsuario().getEmail(),
                    p.getItens().stream().map(ItemDTO::de).toList(), simulado);
        }
    }

    public record AlterarStatusRequest(@NotNull(message = "Informe o status") StatusPedido status) {}
}
