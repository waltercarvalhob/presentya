package br.com.presentya.domain;

import jakarta.persistence.*;

/** Informações editáveis da loja (contatos, horários, frete) no formato chave/valor. */
@Entity
@Table(name = "configuracao")
public class Configuracao {
    @Id @Column(length = 60)
    private String chave;
    @Column(length = 4000)
    private String valor;

    protected Configuracao() {}
    public Configuracao(String chave, String valor) { this.chave = chave; this.valor = valor; }

    public String getChave() { return chave; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
}
