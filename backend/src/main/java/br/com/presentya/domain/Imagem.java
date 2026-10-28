package br.com.presentya.domain;

import jakarta.persistence.*;

/** Fotos dos produtos ficam no banco para não se perderem em hospedagens com disco temporário. */
@Entity
@Table(name = "imagem")
public class Imagem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 60)
    private String contentType;
    @Column(nullable = false)
    private byte[] dados;

    public Long getId() { return id; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public byte[] getDados() { return dados; }
    public void setDados(byte[] dados) { this.dados = dados; }
}
