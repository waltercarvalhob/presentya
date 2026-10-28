package br.com.presentya.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "item_pedido")
public class ItemPedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;
    private Long produtoId;
    @Column(nullable = false, length = 140)
    private String nomeProduto;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;
    private int quantidade;
    /** Nome, data ou frase pedida pelo cliente para produtos personalizáveis. */
    @Column(length = 500)
    private String personalizacao;

    public BigDecimal total() { return precoUnitario.multiply(BigDecimal.valueOf(quantidade)); }

    public Long getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
    public Long getProdutoId() { return produtoId; }
    public void setProdutoId(Long produtoId) { this.produtoId = produtoId; }
    public String getNomeProduto() { return nomeProduto; }
    public void setNomeProduto(String nomeProduto) { this.nomeProduto = nomeProduto; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public String getPersonalizacao() { return personalizacao; }
    public void setPersonalizacao(String personalizacao) { this.personalizacao = personalizacao; }
}
