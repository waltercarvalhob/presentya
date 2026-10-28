package br.com.presentya.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String nome;
    @Column(nullable = false, unique = true, length = 160)
    private String email;
    @Column(nullable = false)
    private String senhaHash;
    @Column(length = 30)
    private String telefone;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Perfil perfil = Perfil.USUARIO;
    private boolean ativo = true;
    /** Clube PresentYá: R$ 1 em compras = 1 ponto. */
    private int pontos;
    private int comprasPagas;
    private Instant criadoEm = Instant.now();

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public Perfil getPerfil() { return perfil; }
    public void setPerfil(Perfil perfil) { this.perfil = perfil; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public int getPontos() { return pontos; }
    public void setPontos(int pontos) { this.pontos = pontos; }
    public int getComprasPagas() { return comprasPagas; }
    public void setComprasPagas(int comprasPagas) { this.comprasPagas = comprasPagas; }
    public Instant getCriadoEm() { return criadoEm; }
}
