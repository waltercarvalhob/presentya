package br.com.presentya.dto;

import br.com.presentya.domain.Perfil;
import br.com.presentya.domain.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegistroRequest(
            @NotBlank(message = "Informe seu nome") @Size(max = 120) String nome,
            @NotBlank(message = "Informe seu e-mail") @Email(message = "E-mail inválido") @Size(max = 160) String email,
            @NotBlank(message = "Informe uma senha") @Size(min = 6, max = 72, message = "A senha deve ter de 6 a 72 caracteres") String senha,
            @Size(max = 30) String telefone) {}

    public record NovoUsuarioRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 120) String nome,
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") @Size(max = 160) String email,
            @NotBlank(message = "Informe uma senha") @Size(min = 6, max = 72, message = "A senha deve ter de 6 a 72 caracteres") String senha,
            @Size(max = 30) String telefone,
            @NotNull(message = "Escolha o perfil") Perfil perfil) {}

    public record LoginRequest(
            @NotBlank(message = "Informe seu e-mail") String email,
            @NotBlank(message = "Informe sua senha") String senha) {}

    public record AtualizarPerfilRequest(
            @NotBlank(message = "Informe seu nome") @Size(max = 120) String nome,
            @Size(max = 30) String telefone,
            String senhaAtual,
            @Size(min = 6, max = 72, message = "A nova senha deve ter de 6 a 72 caracteres") String novaSenha) {}

    public record UsuarioDTO(Long id, String nome, String email, String telefone, Perfil perfil,
                             boolean ativo, int pontos, int comprasPagas, Instant criadoEm) {
        public static UsuarioDTO de(Usuario u) {
            return new UsuarioDTO(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(), u.getPerfil(),
                    u.isAtivo(), u.getPontos(), u.getComprasPagas(), u.getCriadoEm());
        }
    }

    public record TokenResponse(String token, UsuarioDTO usuario) {}
}
