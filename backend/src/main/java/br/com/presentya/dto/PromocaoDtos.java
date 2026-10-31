package br.com.presentya.dto;

import br.com.presentya.domain.Interessado;
import br.com.presentya.domain.Oferta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

public final class PromocaoDtos {
    private PromocaoDtos() {}

    public record OfertaDTO(Long id, String titulo, String descricao, String cupom, LocalDate validaAte,
                            int ordem, boolean ativo) {
        public static OfertaDTO de(Oferta o) {
            return new OfertaDTO(o.getId(), o.getTitulo(), o.getDescricao(), o.getCupom(), o.getValidaAte(),
                    o.getOrdem(), o.isAtivo());
        }
    }

    public record OfertaRequest(
            @NotBlank(message = "Informe o título da oferta") @Size(max = 120) String titulo,
            @Size(max = 1000) String descricao,
            @Size(max = 40, message = "Cupom muito longo") String cupom,
            LocalDate validaAte, Integer ordem, Boolean ativo) {}

    public record InteressadoDTO(Long id, String nome, String whatsapp, String email, String origem, Instant criadoEm) {
        public static InteressadoDTO de(Interessado i) {
            return new InteressadoDTO(i.getId(), i.getNome(), i.getWhatsapp(), i.getEmail(), i.getOrigem(), i.getCriadoEm());
        }
    }

    public record InteressadoRequest(
            @NotBlank(message = "Informe seu nome") @Size(max = 120) String nome,
            @NotBlank(message = "Informe seu WhatsApp") @Size(max = 30) String whatsapp,
            @Email(message = "E-mail inválido") @Size(max = 160) String email,
            @Size(max = 20) String origem,
            @AssertTrue(message = "Marque que aceita receber ofertas") boolean aceite) {}

    /** O mesmo formato de PushSubscription.toJSON() do navegador. */
    public record InscricaoPushRequest(
            @NotBlank @Size(max = 1000) String endpoint,
            @NotNull(message = "Inscrição de notificação inválida") @Valid ChavesDoNavegador keys,
            @Size(max = 20) String origem) {}

    public record ChavesDoNavegador(@NotBlank @Size(max = 120) String p256dh, @NotBlank @Size(max = 60) String auth) {}

    public record CancelarPushRequest(@NotBlank @Size(max = 1000) String endpoint) {}

    public record EnvioPushRequest(
            @NotBlank(message = "Informe o título") @Size(max = 80) String titulo,
            @NotBlank(message = "Informe a mensagem") @Size(max = 300) String mensagem) {}
}
