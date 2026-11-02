package br.com.presentya.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    /** Oferta agendada: entra no ar (e é enviada) neste dia e hora, no horário de São Luís. Vazio = já. */
    private LocalDateTime iniciaEm;
    /** Quando foi enviada para todos; vazio = ainda não (a agenda manda as agendadas quando chegar a hora). */
    private Instant anunciadaEm;
    /** Foto e vídeo da oferta (tabela imagem, que guarda os dois). */
    private Long imagemId;
    private Long videoId;
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
    public LocalDateTime getIniciaEm() { return iniciaEm; }
    public void setIniciaEm(LocalDateTime iniciaEm) { this.iniciaEm = iniciaEm; }
    public Instant getAnunciadaEm() { return anunciadaEm; }
    public void setAnunciadaEm(Instant anunciadaEm) { this.anunciadaEm = anunciadaEm; }
    public Long getImagemId() { return imagemId; }
    public void setImagemId(Long imagemId) { this.imagemId = imagemId; }
    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Instant getCriadoEm() { return criadoEm; }
}
