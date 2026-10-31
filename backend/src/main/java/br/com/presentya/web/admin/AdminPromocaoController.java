package br.com.presentya.web.admin;

import br.com.presentya.dto.PromocaoDtos.EnvioPushRequest;
import br.com.presentya.dto.PromocaoDtos.InteressadoDTO;
import br.com.presentya.dto.PromocaoDtos.OfertaDTO;
import br.com.presentya.dto.PromocaoDtos.OfertaRequest;
import br.com.presentya.push.PushService;
import br.com.presentya.push.PushService.Notificacao;
import br.com.presentya.service.PromocaoService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Somente ADMIN (regra em SecurityConfig): ofertas do QR Code e lista de interessados. */
@RestController
@RequestMapping("/api/admin")
public class AdminPromocaoController {
    private final PromocaoService promocao;
    private final PushService push;

    public AdminPromocaoController(PromocaoService promocao, PushService push) {
        this.promocao = promocao;
        this.push = push;
    }

    @GetMapping("/ofertas")
    public List<OfertaDTO> ofertas() {
        return promocao.todasOfertas();
    }

    @PostMapping("/ofertas")
    @ResponseStatus(HttpStatus.CREATED)
    public OfertaDTO criar(@Valid @RequestBody OfertaRequest req) {
        return promocao.salvarOferta(null, req);
    }

    @PutMapping("/ofertas/{id}")
    public OfertaDTO atualizar(@PathVariable Long id, @Valid @RequestBody OfertaRequest req) {
        return promocao.salvarOferta(id, req);
    }

    @DeleteMapping("/ofertas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        promocao.removerOferta(id);
    }

    @GetMapping("/interessados")
    public List<InteressadoDTO> interessados() {
        return promocao.interessados();
    }

    @DeleteMapping("/interessados/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerInteressado(@PathVariable Long id) {
        promocao.removerInteressado(id);
    }

    @GetMapping("/push")
    public Map<String, Long> resumoPush() {
        return Map.of("inscritos", push.inscritos());
    }

    /** Notificação avulsa para todos os celulares inscritos. */
    @PostMapping("/push/enviar")
    public Map<String, Integer> enviarPush(@Valid @RequestBody EnvioPushRequest req) {
        return Map.of("enviadas", push.enviarParaTodos(new Notificacao(req.titulo(), req.mensagem(), "/ofertas")));
    }
}
