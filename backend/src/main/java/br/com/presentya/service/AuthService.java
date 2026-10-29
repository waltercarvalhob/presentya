package br.com.presentya.service;

import br.com.presentya.domain.Perfil;
import br.com.presentya.domain.Usuario;
import br.com.presentya.dto.AuthDtos.AtualizarPerfilRequest;
import br.com.presentya.dto.AuthDtos.LoginRequest;
import br.com.presentya.dto.AuthDtos.NovoUsuarioRequest;
import br.com.presentya.dto.AuthDtos.RegistroRequest;
import br.com.presentya.dto.AuthDtos.TokenResponse;
import br.com.presentya.dto.AuthDtos.UsuarioDTO;
import br.com.presentya.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final TokenService tokens;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder senhas, TokenService tokens) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.tokens = tokens;
    }

    @Transactional
    public TokenResponse registrar(RegistroRequest req) {
        Usuario u = criar(req.nome(), req.email(), req.senha(), req.telefone(), Perfil.USUARIO);
        return new TokenResponse(tokens.gerar(u), UsuarioDTO.de(u));
    }

    /** Cadastro feito pelo administrador, já com o perfil de acesso. */
    @Transactional
    public UsuarioDTO cadastrar(NovoUsuarioRequest req) {
        return UsuarioDTO.de(criar(req.nome(), req.email(), req.senha(), req.telefone(), req.perfil()));
    }

    private Usuario criar(String nome, String email, String senha, String telefone, Perfil perfil) {
        email = email.trim().toLowerCase();
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com este e-mail");
        }
        Usuario u = new Usuario();
        u.setNome(nome.trim());
        u.setEmail(email);
        u.setTelefone(telefone);
        u.setSenhaHash(senhas.encode(senha));
        u.setPerfil(perfil);
        return usuarios.save(u);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        Usuario u = usuarios.findByEmailIgnoreCase(req.email().trim())
                .filter(x -> senhas.matches(req.senha(), x.getSenhaHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos"));
        if (!u.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta desativada. Fale com a loja.");
        }
        return new TokenResponse(tokens.gerar(u), UsuarioDTO.de(u));
    }

    @Transactional(readOnly = true)
    public Usuario usuarioAtivo(Long id) {
        Usuario u = usuarios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida"));
        if (!u.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta desativada");
        }
        return u;
    }

    @Transactional
    public UsuarioDTO atualizarPerfil(Long id, AtualizarPerfilRequest req) {
        Usuario u = usuarioAtivo(id);
        u.setNome(req.nome().trim());
        u.setTelefone(req.telefone());
        if (req.novaSenha() != null && !req.novaSenha().isBlank()) {
            if (req.senhaAtual() == null || !senhas.matches(req.senhaAtual(), u.getSenhaHash())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta");
            }
            u.setSenhaHash(senhas.encode(req.novaSenha()));
        }
        return UsuarioDTO.de(usuarios.save(u));
    }
}
