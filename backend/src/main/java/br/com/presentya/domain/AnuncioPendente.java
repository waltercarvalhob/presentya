package br.com.presentya.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Anúncio que aconteceu fora do horário de envio e espera na fila até o horário abrir. */
@Entity
@Table(name = "anuncio_pendente", uniqueConstraints = @UniqueConstraint(columnNames = {"tipo", "refId"}))
public class AnuncioPendente {
    public enum Tipo { OFERTA, PRODUTO }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    private Tipo tipo;
    /** Id da oferta ou do produto. */
    @Column(nullable = false)
    private Long refId;
    private Instant criadoEm = Instant.now();

    protected AnuncioPendente() {}
    public AnuncioPendente(Tipo tipo, Long refId) { this.tipo = tipo; this.refId = refId; }

    public Long getId() { return id; }
    public Tipo getTipo() { return tipo; }
    public Long getRefId() { return refId; }
    public Instant getCriadoEm() { return criadoEm; }
}
