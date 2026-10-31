package br.com.presentya.web;

import br.com.presentya.dto.PromocaoDtos.CancelarPushRequest;
import br.com.presentya.dto.PromocaoDtos.InscricaoPushRequest;
import br.com.presentya.dto.PromocaoDtos.InteressadoRequest;
import br.com.presentya.dto.PromocaoDtos.OfertaDTO;
import br.com.presentya.push.PushService;
import br.com.presentya.service.PromocaoService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Rotas públicas da página /ofertas. */
@RestController
@RequestMapping("/api")
public class PromocaoController {
    private final PromocaoService promocao;
    private final PushService push;

    public PromocaoController(PromocaoService promocao, PushService push) {
        this.promocao = promocao;
        this.push = push;
    }

    @GetMapping("/ofertas")
    public List<OfertaDTO> ofertas() {
        return promocao.ofertasVigentes();
    }

    @PostMapping("/interessados")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void interessado(@Valid @RequestBody InteressadoRequest req) {
        promocao.registrarInteressado(req);
    }

    /** Chave pública VAPID que o navegador precisa para se inscrever nas notificações. */
    @GetMapping("/push/chave")
    public Map<String, String> chavePush() {
        return Map.of("chave", push.chavePublica());
    }

    @PostMapping("/push/inscricoes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inscreverPush(@Valid @RequestBody InscricaoPushRequest req) {
        promocao.inscreverPush(req);
    }

    @PostMapping("/push/cancelar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelarPush(@Valid @RequestBody CancelarPushRequest req) {
        push.cancelar(req.endpoint());
    }
}
