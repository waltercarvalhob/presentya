package br.com.presentya.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Pessoa que chamou a loja no WhatsApp ou no Telegram pelo QR Code e recebe as ofertas automaticamente. */
@Entity
@Table(name = "contato_mensageiro", uniqueConstraints = @UniqueConstraint(columnNames = {"canal", "destino"}))
public class ContatoMensageiro {
    public enum Canal { WHATSAPP, TELEGRAM }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12)
    private Canal canal;
    /** Número do WhatsApp (só dígitos) ou id do chat do Telegram. */
    @Column(nullable = false, length = 40)
    private String destino;
    @Column(length = 120)
    private String nome;
    @Column(length = 20)
    private String origem;
    /** O WhatsApp só deixa mandar mensagem livre até 24 h depois da última mensagem da pessoa. */
    private Instant ultimaMensagemEm;
    private Instant criadoEm = Instant.now();

    protected ContatoMensageiro() {}
    public ContatoMensageiro(Canal canal, String destino) { this.canal = canal; this.destino = destino; }

    public Long getId() { return id; }
    public Canal getCanal() { return canal; }
    public String getDestino() { return destino; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public Instant getUltimaMensagemEm() { return ultimaMensagemEm; }
    public void setUltimaMensagemEm(Instant ultimaMensagemEm) { this.ultimaMensagemEm = ultimaMensagemEm; }
    public Instant getCriadoEm() { return criadoEm; }
}
