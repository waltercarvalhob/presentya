package br.com.presentya.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido")
public class Pedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 20)
    private String codigo;
    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private StatusPedido status = StatusPedido.AGUARDANDO_PAGAMENTO;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    private FormaPagamento formaPagamento;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    private TipoEntrega tipoEntrega;
    @Column(length = 120)
    private String lojaRetirada;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal frete;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;
    @Column(length = 120) private String nomeDestinatario;
    @Column(length = 30) private String telefone;
    @Column(length = 10) private String cep;
    @Column(length = 200) private String endereco;
    @Column(length = 20) private String numero;
    @Column(length = 120) private String complemento;
    @Column(length = 120) private String bairro;
    @Column(length = 120) private String cidade;
    @Column(length = 2) private String uf;
    @Column(length = 500) private String mensagemCartao;
    @Column(length = 1000) private String observacoes;
    /** Id do pagamento ou da preferência no gateway (Mercado Pago). */
    @Column(length = 80) private String gatewayPagamentoId;
    /** Página de pagamento (cartão de crédito/débito). */
    @Column(length = 600) private String linkPagamento;
    @Column(length = 1000) private String pixCopiaCola;
    @Column(length = 20000) private String pixQrBase64;
    private Instant criadoEm = Instant.now();
    private Instant atualizadoEm = Instant.now();
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ItemPedido> itens = new ArrayList<>();

    @PreUpdate
    void tocar() { atualizadoEm = Instant.now(); }

    public void adicionarItem(ItemPedido item) { item.setPedido(this); itens.add(item); }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public StatusPedido getStatus() { return status; }
    public void setStatus(StatusPedido status) { this.status = status; }
    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(FormaPagamento formaPagamento) { this.formaPagamento = formaPagamento; }
    public TipoEntrega getTipoEntrega() { return tipoEntrega; }
    public void setTipoEntrega(TipoEntrega tipoEntrega) { this.tipoEntrega = tipoEntrega; }
    public String getLojaRetirada() { return lojaRetirada; }
    public void setLojaRetirada(String lojaRetirada) { this.lojaRetirada = lojaRetirada; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getFrete() { return frete; }
    public void setFrete(BigDecimal frete) { this.frete = frete; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getNomeDestinatario() { return nomeDestinatario; }
    public void setNomeDestinatario(String nomeDestinatario) { this.nomeDestinatario = nomeDestinatario; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public String getMensagemCartao() { return mensagemCartao; }
    public void setMensagemCartao(String mensagemCartao) { this.mensagemCartao = mensagemCartao; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public String getGatewayPagamentoId() { return gatewayPagamentoId; }
    public void setGatewayPagamentoId(String gatewayPagamentoId) { this.gatewayPagamentoId = gatewayPagamentoId; }
    public String getLinkPagamento() { return linkPagamento; }
    public void setLinkPagamento(String linkPagamento) { this.linkPagamento = linkPagamento; }
    public String getPixCopiaCola() { return pixCopiaCola; }
    public void setPixCopiaCola(String pixCopiaCola) { this.pixCopiaCola = pixCopiaCola; }
    public String getPixQrBase64() { return pixQrBase64; }
    public void setPixQrBase64(String pixQrBase64) { this.pixQrBase64 = pixQrBase64; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public List<ItemPedido> getItens() { return itens; }
}
