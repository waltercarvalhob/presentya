package br.com.presentya.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/** Promoção divulgada na página /ofertas (aberta pelo QR Code da vitrine). */
@Entity
@Table(name = "oferta")
public class Oferta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String titulo;
    @Column(length = 1000)
    private String descricao;
    /** Cupom para o cliente mostrar no caixa ou mandar no WhatsApp (opcional). */
    @Column(length = 40)
    private String cupom;
    /** Último dia da oferta; vazio = sem prazo. */
    private LocalDate validaAte;
    private int ordem;
    private boolean ativo = true;
    private Instant criadoEm = Instant.now();

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getCupom() { return cupom; }
    public void setCupom(String cupom) { this.cupom = cupom; }
    public LocalDate getValidaAte() { return validaAte; }
    public void setValidaAte(LocalDate validaAte) { this.validaAte = validaAte; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Instant getCriadoEm() { return criadoEm; }
}
