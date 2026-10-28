package br.com.presentya.web.admin;

import br.com.presentya.domain.Usuario;
import br.com.presentya.dto.AdminDtos.AlterarUsuarioRequest;
import br.com.presentya.dto.AuthDtos.UsuarioDTO;
import br.com.presentya.repository.UsuarioRepository;
import br.com.presentya.service.ConfiguracaoService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Somente ADMIN: usuários/perfis e dados da loja. */
@RestController
@RequestMapping("/api/admin")
public class AdminLojaController {
    private final UsuarioRepository usuarios;
    private final ConfiguracaoService config;

    public AdminLojaController(UsuarioRepository usuarios, ConfiguracaoService config) {
        this.usuarios = usuarios;
        this.config = config;
    }

    @GetMapping("/usuarios")
    public List<UsuarioDTO> usuarios() {
        return usuarios.findAllByOrderByNomeAsc().stream().map(UsuarioDTO::de).toList();
    }

    @PatchMapping("/usuarios/{id}")
    @Transactional
    public UsuarioDTO alterar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                              @RequestBody AlterarUsuarioRequest req) {
        if (id.equals(Long.valueOf(jwt.getSubject()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Você não pode alterar o próprio perfil de acesso");
        }
        Usuario u = usuarios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        if (req.perfil() != null) u.setPerfil(req.perfil());
        if (req.ativo() != null) u.setAtivo(req.ativo());
        return UsuarioDTO.de(usuarios.save(u));
    }

    @GetMapping("/configuracoes")
    public Map<String, String> configuracoes() {
        return config.todas();
    }

    @PutMapping("/configuracoes")
    public Map<String, String> salvar(@RequestBody Map<String, String> valores) {
        return config.salvar(valores);
    }
}
