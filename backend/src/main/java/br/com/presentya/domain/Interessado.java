package br.com.presentya.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Pessoa que pediu para receber ofertas (pelo QR Code ou pelo aviso de proximidade). */
@Entity
@Table(name = "interessado")
public class Interessado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String nome;
    /** Só números, com 55 e DDD: é a chave para não duplicar o cadastro. */
    @Column(nullable = false, unique = true, length = 15)
    private String whatsapp;
    @Column(length = 160)
    private String email;
    /** De onde veio: "qrcode", "proximidade" ou "site". */
    @Column(length = 20)
    private String origem;
    private Instant criadoEm = Instant.now();

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public Instant getCriadoEm() { return criadoEm; }
}
