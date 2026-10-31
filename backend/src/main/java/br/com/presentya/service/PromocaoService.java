package br.com.presentya.service;

import br.com.presentya.domain.InscricaoPush;
import br.com.presentya.domain.Interessado;
import br.com.presentya.domain.Oferta;
import br.com.presentya.dto.PromocaoDtos.InscricaoPushRequest;
import br.com.presentya.dto.PromocaoDtos.InteressadoDTO;
import br.com.presentya.dto.PromocaoDtos.InteressadoRequest;
import br.com.presentya.dto.PromocaoDtos.OfertaDTO;
import br.com.presentya.dto.PromocaoDtos.OfertaRequest;
import br.com.presentya.repository.InteressadoRepository;
import br.com.presentya.push.PushService;
import br.com.presentya.push.PushService.Notificacao;
import br.com.presentya.repository.OfertaRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Ofertas da página /ofertas (QR Code da vitrine), quem quer recebê-las e as notificações automáticas. */
@Service
public class PromocaoService {
    private static final ZoneId FUSO = ZoneId.of("America/Fortaleza");
    private static final Set<String> ORIGENS = Set.of("qrcode", "proximidade", "site");

    private final OfertaRepository ofertas;
    private final InteressadoRepository interessados;
    private final PushService push;

    public PromocaoService(OfertaRepository ofertas, InteressadoRepository interessados, PushService push) {
        this.ofertas = ofertas;
        this.interessados = interessados;
        this.push = push;
    }

    @Transactional(readOnly = true)
    public List<OfertaDTO> ofertasVigentes() {
        return ofertas.findAllByOrderByOrdemAscCriadoEmDesc().stream()
                .filter(PromocaoService::vigente).map(OfertaDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public List<OfertaDTO> todasOfertas() {
        return ofertas.findAllByOrderByOrdemAscCriadoEmDesc().stream().map(OfertaDTO::de).toList();
    }

    /** Oferta que passa a valer (nova ou reativada) vira notificação automática para todos os inscritos. */
    @Transactional
    public OfertaDTO salvarOferta(Long id, OfertaRequest req) {
        Oferta o = id == null ? new Oferta() : ofertas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta não encontrada"));
        boolean jaValia = o.getId() != null && vigente(o);
        o.setTitulo(req.titulo().trim());
        o.setDescricao(vazioNulo(req.descricao()));
        String cupom = vazioNulo(req.cupom());
        o.setCupom(cupom == null ? null : cupom.toUpperCase());
        o.setValidaAte(req.validaAte());
        if (req.ordem() != null) o.setOrdem(req.ordem());
        if (req.ativo() != null) o.setAtivo(req.ativo());
        ofertas.save(o);
        if (!jaValia && vigente(o)) {
            String texto = o.getTitulo() + (o.getDescricao() == null ? "" : " – " + o.getDescricao())
                    + (o.getCupom() == null ? "" : " · Cupom " + o.getCupom());
            push.enviarParaTodos(new Notificacao("🎁 Nova oferta na PresentYá", texto, "/ofertas"));
        }
        return OfertaDTO.de(o);
    }

    /** Inscreve o celular e já manda as ofertas do dia, para a pessoa ver na hora que funcionou. */
    @Transactional
    public void inscreverPush(InscricaoPushRequest req) {
        InscricaoPush i = push.inscrever(req.endpoint(), req.keys().p256dh(), req.keys().auth(),
                ORIGENS.contains(req.origem()) ? req.origem() : "site");
        List<OfertaDTO> vigentes = ofertasVigentes();
        if (vigentes.isEmpty()) {
            push.enviar(i, new Notificacao("Notificações ativadas 🎁", "Você vai receber aqui as próximas ofertas da PresentYá.", "/ofertas"));
            return;
        }
        String texto = vigentes.stream().limit(3)
                .map(o -> o.titulo() + (o.cupom() == null ? "" : " (cupom " + o.cupom() + ")"))
                .collect(Collectors.joining(" · "))
                + (vigentes.size() > 3 ? " e mais " + (vigentes.size() - 3) : "");
        push.enviar(i, new Notificacao("🎁 Ofertas de hoje na PresentYá", texto, "/ofertas"));
    }

    private static boolean vigente(Oferta o) {
        return o.isAtivo() && (o.getValidaAte() == null || !o.getValidaAte().isBefore(LocalDate.now(FUSO)));
    }

    @Transactional
    public void removerOferta(Long id) {
        if (!ofertas.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta não encontrada");
        ofertas.deleteById(id);
    }

    /** Mesmo WhatsApp de novo só atualiza o cadastro, para a lista não ficar com repetidos. */
    @Transactional
    public void registrarInteressado(InteressadoRequest req) {
        String numero = req.whatsapp().replaceAll("\\D", "");
        if (numero.length() == 10 || numero.length() == 11) numero = "55" + numero;
        if (numero.length() < 12 || numero.length() > 13) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o WhatsApp com DDD");
        }
        Interessado i = interessados.findByWhatsapp(numero).orElseGet(Interessado::new);
        i.setNome(req.nome().trim());
        i.setWhatsapp(numero);
        String email = vazioNulo(req.email());
        if (email != null) i.setEmail(email.toLowerCase());
        if (i.getOrigem() == null) i.setOrigem(ORIGENS.contains(req.origem()) ? req.origem() : "site");
        interessados.save(i);
    }

    @Transactional(readOnly = true)
    public List<InteressadoDTO> interessados() {
        return interessados.findAllByOrderByCriadoEmDesc().stream().map(InteressadoDTO::de).toList();
    }

    @Transactional
    public void removerInteressado(Long id) {
        interessados.deleteById(id);
    }

    private static String vazioNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
