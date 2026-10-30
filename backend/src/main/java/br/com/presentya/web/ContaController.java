package br.com.presentya.web;

import br.com.presentya.domain.Usuario;
import br.com.presentya.dto.AuthDtos.AtualizarPerfilRequest;
import br.com.presentya.dto.AuthDtos.UsuarioDTO;
import br.com.presentya.dto.PedidoDtos.NovoPedidoRequest;
import br.com.presentya.dto.PedidoDtos.PedidoDTO;
import br.com.presentya.service.AuthService;
import br.com.presentya.service.PedidoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** Rotas do cliente logado: perfil e pedidos. */
@RestController
@RequestMapping("/api")
public class ContaController {
    private final AuthService auth;
    private final PedidoService pedidos;

    public ContaController(AuthService auth, PedidoService pedidos) {
        this.auth = auth;
        this.pedidos = pedidos;
    }

    private Usuario eu(Jwt jwt) {
        return auth.usuarioAtivo(Long.valueOf(jwt.getSubject()));
    }

    /** null quando a requisição não traz login (compra ou acompanhamento como convidado). */
    private Usuario euOuConvidado(Jwt jwt) {
        return jwt != null ? eu(jwt) : null;
    }

    @GetMapping("/usuarios/me")
    public UsuarioDTO me(@AuthenticationPrincipal Jwt jwt) {
        return UsuarioDTO.de(eu(jwt));
    }

    @PutMapping("/usuarios/me")
    public UsuarioDTO atualizar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AtualizarPerfilRequest req) {
        return auth.atualizarPerfil(Long.valueOf(jwt.getSubject()), req);
    }

    @PostMapping("/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoDTO criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody NovoPedidoRequest req) {
        return pedidos.criar(euOuConvidado(jwt), req);
    }

    @GetMapping("/pedidos/meus")
    public List<PedidoDTO> meus(@AuthenticationPrincipal Jwt jwt) {
        return pedidos.doCliente(eu(jwt).getId());
    }

    @GetMapping("/pedidos/{codigo}")
    public PedidoDTO detalhe(@AuthenticationPrincipal Jwt jwt, @PathVariable String codigo) {
        return pedidos.detalhe(codigo, euOuConvidado(jwt));
    }

    @PostMapping("/pedidos/{codigo}/sincronizar")
    public PedidoDTO sincronizar(@AuthenticationPrincipal Jwt jwt, @PathVariable String codigo) {
        return pedidos.sincronizar(codigo, euOuConvidado(jwt));
    }

    @PostMapping("/pedidos/{codigo}/simular-pagamento")
    public PedidoDTO simular(@AuthenticationPrincipal Jwt jwt, @PathVariable String codigo) {
        return pedidos.simularPagamento(codigo, euOuConvidado(jwt));
    }
}
