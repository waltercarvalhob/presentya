package br.com.presentya.domain;

import jakarta.persistence.*;

/**
 * Par de chaves VAPID do servidor, gerado uma única vez. Fica fora de "configuracao" porque aquela
 * tabela é pública (/api/configuracoes) e a chave privada não pode vazar.
 */
@Entity
@Table(name = "chaves_push")
public class ChavesPush {
    public static final long ID = 1L;

    @Id
    private Long id = ID;
    @Column(nullable = false, length = 120)
    private String publica;
    @Column(nullable = false, length = 400)
    private String privada;

    protected ChavesPush() {}
    public ChavesPush(String publica, String privada) { this.publica = publica; this.privada = privada; }

    public Long getId() { return id; }
    public String getPublica() { return publica; }
    public String getPrivada() { return privada; }
}
