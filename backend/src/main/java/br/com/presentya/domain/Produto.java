package br.com.presentya.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "produto")
public class Produto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 140)
    private String nome;
    @Column(length = 4000)
    private String descricao;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;
    /** Quando preenchido (e menor que o preço), o produto aparece como "Oferta". */
    @Column(precision = 10, scale = 2)
    private BigDecimal precoPromocional;
    /** null = sem controle de estoque (feito sob encomenda). */
    private Integer estoque;
    private boolean personalizavel;
    private boolean novidade;
    private boolean maisVendido;
    private boolean ativo = true;
    private Long imagemId;
    /** Ilustração usada quando não há foto: box, tall, mug, frame, basket, lamp, bear, spa, key, tree, choc, card. */
    @Column(length = 20)
    private String ilustracao = "box";
    @Column(length = 40)
    private String corFundo = "var(--rose-soft)";
    @Column(length = 40)
    private String corA = "var(--rose)";
    @Column(length = 40)
    private String corB = "var(--gold-bright)";
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "produto_ocasiao", joinColumns = @JoinColumn(name = "produto_id"))
    @Column(name = "ocasiao", length = 30)
    private Set<String> ocasioes = new HashSet<>();
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "produto_publico", joinColumns = @JoinColumn(name = "produto_id"))
    @Column(name = "publico", length = 30)
    private Set<String> publicos = new HashSet<>();
    private int vendidos;
    private Instant criadoEm = Instant.now();

    /** Preço cobrado agora: o promocional, se for válido. */
    public BigDecimal precoAtual() {
        return precoPromocional != null && precoPromocional.compareTo(preco) < 0 ? precoPromocional : preco;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }
    public BigDecimal getPrecoPromocional() { return precoPromocional; }
    public void setPrecoPromocional(BigDecimal precoPromocional) { this.precoPromocional = precoPromocional; }
    public Integer getEstoque() { return estoque; }
    public void setEstoque(Integer estoque) { this.estoque = estoque; }
    public boolean isPersonalizavel() { return personalizavel; }
    public void setPersonalizavel(boolean personalizavel) { this.personalizavel = personalizavel; }
    public boolean isNovidade() { return novidade; }
    public void setNovidade(boolean novidade) { this.novidade = novidade; }
    public boolean isMaisVendido() { return maisVendido; }
    public void setMaisVendido(boolean maisVendido) { this.maisVendido = maisVendido; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Long getImagemId() { return imagemId; }
    public void setImagemId(Long imagemId) { this.imagemId = imagemId; }
    public String getIlustracao() { return ilustracao; }
    public void setIlustracao(String ilustracao) { this.ilustracao = ilustracao; }
    public String getCorFundo() { return corFundo; }
    public void setCorFundo(String corFundo) { this.corFundo = corFundo; }
    public String getCorA() { return corA; }
    public void setCorA(String corA) { this.corA = corA; }
    public String getCorB() { return corB; }
    public void setCorB(String corB) { this.corB = corB; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public Set<String> getOcasioes() { return ocasioes; }
    public void setOcasioes(Set<String> ocasioes) { this.ocasioes = ocasioes; }
    public Set<String> getPublicos() { return publicos; }
    public void setPublicos(Set<String> publicos) { this.publicos = publicos; }
    public int getVendidos() { return vendidos; }
    public void setVendidos(int vendidos) { this.vendidos = vendidos; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
