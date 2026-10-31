package br.com.presentya.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Celular/navegador que aceitou receber notificações de ofertas (geralmente pelo QR Code da vitrine). */
@Entity
@Table(name = "inscricao_push")
public class InscricaoPush {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** Endereço do serviço de push do navegador (Google, Apple, Mozilla, Microsoft). */
    @Column(nullable = false, unique = true, length = 1000)
    private String endpoint;
    @Column(nullable = false, length = 120)
    private String p256dh;
    @Column(nullable = false, length = 60)
    private String auth;
    @Column(length = 20)
    private String origem;
    private Instant criadoEm = Instant.now();

    public Long getId() { return id; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getP256dh() { return p256dh; }
    public void setP256dh(String p256dh) { this.p256dh = p256dh; }
    public String getAuth() { return auth; }
    public void setAuth(String auth) { this.auth = auth; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public Instant getCriadoEm() { return criadoEm; }
}
