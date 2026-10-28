package br.com.presentya.service;

import br.com.presentya.domain.ItemPedido;
import br.com.presentya.domain.Pedido;
import br.com.presentya.domain.Perfil;
import br.com.presentya.domain.Produto;
import br.com.presentya.domain.StatusPedido;
import br.com.presentya.domain.TipoEntrega;
import br.com.presentya.domain.Usuario;
import br.com.presentya.dto.AdminDtos.DashboardDTO;
import br.com.presentya.dto.PedidoDtos.ItemRequest;
import br.com.presentya.dto.PedidoDtos.NovoPedidoRequest;
import br.com.presentya.dto.PedidoDtos.PedidoDTO;
import br.com.presentya.pagamento.GatewayPagamento;
import br.com.presentya.repository.PedidoRepository;
import br.com.presentya.repository.ProdutoRepository;
import br.com.presentya.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PedidoService {
    private static final EnumSet<StatusPedido> FATURADOS =
            EnumSet.of(StatusPedido.PAGO, StatusPedido.EM_PRODUCAO, StatusPedido.ENVIADO, StatusPedido.ENTREGUE);
    private static final String LETRAS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PedidoRepository pedidos;
    private final ProdutoRepository produtos;
    private final UsuarioRepository usuarios;
    private final ConfiguracaoService config;
    private final GatewayPagamento gateway;

    public PedidoService(PedidoRepository pedidos, ProdutoRepository produtos, UsuarioRepository usuarios,
                         ConfiguracaoService config, GatewayPagamento gateway) {
        this.pedidos = pedidos;
        this.produtos = produtos;
        this.usuarios = usuarios;
        this.config = config;
        this.gateway = gateway;
    }

    /** Preços, frete e total são sempre calculados aqui, nunca aceitos do navegador. */
    @Transactional
    public PedidoDTO criar(Usuario cliente, NovoPedidoRequest req) {
        Pedido p = new Pedido();
        p.setCodigo(novoCodigo());
        p.setUsuario(cliente);
        p.setFormaPagamento(req.formaPagamento());
        p.setTipoEntrega(req.tipoEntrega());
        p.setNomeDestinatario(req.nomeDestinatario().trim());
        p.setTelefone(req.telefone().trim());
        p.setMensagemCartao(req.mensagemCartao());
        p.setObservacoes(req.observacoes());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest ir : req.itens()) {
            Produto prod = produtos.findById(ir.produtoId())
                    .filter(Produto::isAtivo)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Um dos produtos do carrinho não está mais disponível"));
            if (prod.getEstoque() != null) {
                if (prod.getEstoque() < ir.quantidade()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Estoque insuficiente para " + prod.getNome() + " (disponível: " + prod.getEstoque() + ")");
                }
                prod.setEstoque(prod.getEstoque() - ir.quantidade());
            }
            ItemPedido item = new ItemPedido();
            item.setProdutoId(prod.getId());
            item.setNomeProduto(prod.getNome());
            item.setPrecoUnitario(prod.precoAtual());
            item.setQuantidade(ir.quantidade());
            item.setPersonalizacao(prod.isPersonalizavel() ? vazioParaNull(ir.personalizacao()) : null);
            p.adicionarItem(item);
            subtotal = subtotal.add(item.total());
        }

        if (req.tipoEntrega() == TipoEntrega.RETIRADA) {
            if (vazio(req.lojaRetirada())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha a loja para retirada");
            }
            p.setLojaRetirada(req.lojaRetirada().trim());
            p.setFrete(BigDecimal.ZERO);
        } else {
            if (vazio(req.cep()) || vazio(req.endereco()) || vazio(req.numero()) || vazio(req.bairro())
                    || vazio(req.cidade()) || vazio(req.uf())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preencha o endereço completo de entrega");
            }
            p.setCep(req.cep().trim());
            p.setEndereco(req.endereco().trim());
            p.setNumero(req.numero().trim());
            p.setComplemento(vazioParaNull(req.complemento()));
            p.setBairro(req.bairro().trim());
            p.setCidade(req.cidade().trim());
            p.setUf(req.uf().trim().toUpperCase());
            p.setFrete(freteDaCidade(req.cidade()));
        }
        p.setSubtotal(subtotal);
        p.setTotal(subtotal.add(p.getFrete()));

        pedidos.save(p);
        gateway.iniciar(p);
        return PedidoDTO.de(p, gateway.simulado());
    }

    /** Frete da cidade da loja (São Luís) ou frete para o restante do Brasil. */
    public BigDecimal freteDaCidade(String cidade) {
        boolean local = Slugs.de(cidade).equals(Slugs.de(config.valor("cidade_entrega")));
        return config.decimal(local ? "frete_cidade" : "frete_brasil");
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> doCliente(Long usuarioId) {
        return pedidos.findByUsuarioIdOrderByCriadoEmDesc(usuarioId).stream()
                .map(p -> PedidoDTO.de(p, gateway.simulado())).toList();
    }

    @Transactional(readOnly = true)
    public PedidoDTO detalhe(String codigo, Usuario quem) {
        return PedidoDTO.de(doDono(codigo, quem), gateway.simulado());
    }

    /** Pergunta ao gateway se o pagamento já caiu (útil quando o webhook não alcança o servidor). */
    @Transactional
    public PedidoDTO sincronizar(String codigo, Usuario quem) {
        Pedido p = doDono(codigo, quem);
        if (p.getStatus() == StatusPedido.AGUARDANDO_PAGAMENTO) {
            gateway.consultar(p).ifPresent(s -> aplicarPagamento(p, s));
        }
        return PedidoDTO.de(p, gateway.simulado());
    }

    @Transactional
    public PedidoDTO simularPagamento(String codigo, Usuario quem) {
        if (!gateway.simulado()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso indisponível");
        }
        Pedido p = doDono(codigo, quem);
        aplicarPagamento(p, StatusPedido.PAGO);
        return PedidoDTO.de(p, true);
    }

    @Transactional
    public void processarNotificacao(String pagamentoId) {
        gateway.lerNotificacao(pagamentoId).ifPresent(n ->
                pedidos.findByCodigo(n.codigoPedido()).ifPresent(p -> aplicarPagamento(p, n.status())));
    }

    /** Idempotente: o mesmo aviso de pagamento pode chegar várias vezes. */
    private void aplicarPagamento(Pedido p, StatusPedido novo) {
        if (p.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) return;
        if (novo == StatusPedido.PAGO) {
            p.setStatus(StatusPedido.PAGO);
            Usuario u = p.getUsuario();
            u.setPontos(u.getPontos() + p.getTotal().intValue());
            u.setComprasPagas(u.getComprasPagas() + 1);
            usuarios.save(u);
            p.getItens().forEach(i -> produtos.findById(i.getProdutoId())
                    .ifPresent(prod -> prod.setVendidos(prod.getVendidos() + i.getQuantidade())));
        } else if (novo == StatusPedido.CANCELADO) {
            cancelar(p);
        }
    }

    private void cancelar(Pedido p) {
        p.setStatus(StatusPedido.CANCELADO);
        p.getItens().forEach(i -> produtos.findById(i.getProdutoId())
                .filter(prod -> prod.getEstoque() != null)
                .ifPresent(prod -> prod.setEstoque(prod.getEstoque() + i.getQuantidade())));
    }

    // ---------- área administrativa ----------

    @Transactional(readOnly = true)
    public List<PedidoDTO> listar(StatusPedido status) {
        List<Pedido> lista = status == null ? pedidos.findTop200ByOrderByCriadoEmDesc()
                : pedidos.findTop200ByStatusOrderByCriadoEmDesc(status);
        return lista.stream().map(p -> PedidoDTO.de(p, gateway.simulado())).toList();
    }

    @Transactional
    public PedidoDTO alterarStatus(String codigo, StatusPedido novo) {
        Pedido p = pedidos.findByCodigo(codigo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));
        if (p.getStatus() == StatusPedido.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pedido cancelado não pode mudar de status");
        }
        if (p.getStatus() == StatusPedido.AGUARDANDO_PAGAMENTO && novo == StatusPedido.PAGO) {
            aplicarPagamento(p, StatusPedido.PAGO); // pagamento confirmado manualmente (ex.: Pix na loja)
        } else if (novo == StatusPedido.CANCELADO) {
            cancelar(p);
        } else {
            p.setStatus(novo);
        }
        return PedidoDTO.de(p, gateway.simulado());
    }

    @Transactional(readOnly = true)
    public DashboardDTO painel() {
        return new DashboardDTO(
                produtos.countByAtivoTrue(),
                pedidos.countByStatus(StatusPedido.AGUARDANDO_PAGAMENTO),
                pedidos.countByStatus(StatusPedido.PAGO),
                pedidos.countByStatus(StatusPedido.EM_PRODUCAO),
                pedidos.countByStatus(StatusPedido.ENVIADO),
                usuarios.count(),
                pedidos.somarTotal(FATURADOS),
                pedidos.findTop200ByOrderByCriadoEmDesc().stream().limit(6)
                        .map(p -> PedidoDTO.de(p, gateway.simulado())).toList());
    }

    // ---------- apoio ----------

    private Pedido doDono(String codigo, Usuario quem) {
        Pedido p = pedidos.findByCodigo(codigo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));
        boolean equipe = quem.getPerfil() == Perfil.ADMIN || quem.getPerfil() == Perfil.TECNICO;
        if (!equipe && !p.getUsuario().getId().equals(quem.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado");
        }
        return p;
    }

    private String novoCodigo() {
        String data = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String codigo;
        do {
            StringBuilder sb = new StringBuilder("PY").append(data);
            for (int i = 0; i < 4; i++) sb.append(LETRAS.charAt(RANDOM.nextInt(LETRAS.length())));
            codigo = sb.toString();
        } while (pedidos.existsByCodigo(codigo));
        return codigo;
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    private static String vazioParaNull(String s) {
        return vazio(s) ? null : s.trim();
    }
}
