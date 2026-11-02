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
import br.com.presentya.mensageiro.MensageiroService;
import br.com.presentya.push.PushService;
import br.com.presentya.push.PushService.Notificacao;
import br.com.presentya.repository.OfertaRepository;
import br.com.presentya.dto.ProdutoDtos.ProdutoDTO;
import br.com.presentya.service.CatalogoService.ProdutoEmPromocao;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.server.ResponseStatusException;

/** Ofertas da página /ofertas (QR Code da vitrine), quem quer recebê-las e as notificações automáticas. */
@Service
public class PromocaoService {
    private static final ZoneId FUSO = ZoneId.of("America/Fortaleza");
    private static final Set<String> ORIGENS = Set.of("qrcode", "nfc", "proximidade", "site");

    private final OfertaRepository ofertas;
    private final InteressadoRepository interessados;
    private final PushService push;
    private final CatalogoService catalogo;
    private final MensageiroService mensageiro;
    private final ConfiguracaoService config;
    private final String apiUrl;

    public PromocaoService(OfertaRepository ofertas, InteressadoRepository interessados, PushService push,
                           CatalogoService catalogo, MensageiroService mensageiro, ConfiguracaoService config,
                           @Value("${app.api-url}") String apiUrl) {
        this.ofertas = ofertas;
        this.interessados = interessados;
        this.push = push;
        this.catalogo = catalogo;
        this.mensageiro = mensageiro;
        this.config = config;
        this.apiUrl = apiUrl.replaceAll("/+$", "");
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

    /**
     * Oferta que passa a valer (nova, reativada ou cuja hora agendada já chegou) vira notificação automática
     * para todos. Agendada para depois: a AgendaPromocoes anuncia quando chegar a hora.
     */
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
        o.setImagemId(midia(req.imagemId(), "image/", "Foto da oferta não encontrada"));
        o.setVideoId(midia(req.videoId(), "video/", "Vídeo da oferta não encontrado"));
        o.setIniciaEm(req.iniciaEm());
        if (!vigente(o)) o.setAnunciadaEm(null);
        ofertas.save(o);
        if (!jaValia && vigente(o)) anunciar(o);
        return OfertaDTO.de(o);
    }

    /** Ofertas agendadas cuja hora chegou: entram no ar e vão para todos (chamado a cada minuto pela agenda). */
    @Transactional
    public int anunciarAgendadas() {
        List<Oferta> prontas = ofertas.findByAtivoTrueAndIniciaEmNotNullAndAnunciadaEmIsNull().stream()
                .filter(PromocaoService::vigente).toList();
        prontas.forEach(this::anunciar);
        return prontas.size();
    }

    private void anunciar(Oferta o) {
        String texto = o.getTitulo() + (o.getDescricao() == null ? "" : " – " + o.getDescricao())
                + (o.getCupom() == null ? "" : " · Cupom " + o.getCupom());
        push.enviarParaTodos(new Notificacao("🎁 Nova oferta na PresentYá", texto, "/ofertas", foto(o.getImagemId())));
        mensageiro.divulgar(o);
        o.setAnunciadaEm(Instant.now());
    }

    /** Produto que entrou em promoção vira anúncio para todos, depois que o cadastro foi salvo de verdade. */
    @TransactionalEventListener
    public void produtoEmPromocao(ProdutoEmPromocao p) {
        int desconto = p.preco().subtract(p.precoPromocional()).multiply(BigDecimal.valueOf(100))
                .divide(p.preco(), 0, RoundingMode.HALF_UP).intValue();
        String texto = "De " + reais(p.preco()) + " por " + reais(p.precoPromocional()) + " (" + desconto + "% off)";
        push.enviarParaTodos(new Notificacao("🔥 " + p.nome() + " em promoção", texto, "/ofertas", foto(p.imagemId())));
        mensageiro.divulgar(p.nome(), texto, p.imagemId(), "🔥 Produto em promoção na PresentYá!");
    }

    /**
     * Envio diário: uma vez por dia, entre a hora escolhida e o horário limite (padrão 10:00 às 23:00). Se o servidor estava
     * dormindo (plano gratuito do Render), manda assim que acordar, ainda no mesmo dia.
     */
    @Transactional
    public boolean envioDiarioSePreciso() {
        if (!"true".equals(config.valor("envio_diario_ativo"))) return false;
        LocalTime hora = horario("envio_diario_hora", LocalTime.of(10, 0));
        LocalTime fim = horario("envio_diario_fim", LocalTime.of(23, 0));
        LocalDateTime agora = LocalDateTime.now(FUSO);
        if (agora.toLocalTime().isBefore(hora) || !agora.toLocalTime().isBefore(fim)) return false;
        String hoje = agora.toLocalDate().toString();
        if (hoje.equals(config.valor("envio_diario_ultimo"))) return false;
        config.gravarInterno("envio_diario_ultimo", hoje);
        return enviarResumoDoDia();
    }

    private LocalTime horario(String chave, LocalTime padrao) {
        try {
            return LocalTime.parse(config.valor(chave).trim());
        } catch (DateTimeParseException e) {
            return padrao;
        }
    }

    private boolean enviarResumoDoDia() {
        List<Oferta> vigentes = ofertas.findAllByOrderByOrdemAscCriadoEmDesc().stream().filter(PromocaoService::vigente).toList();
        List<ProdutoDTO> promocao = catalogo.produtosAtivos().stream()
                .filter(p -> p.precoPromocional() != null && p.precoPromocional().compareTo(p.preco()) < 0).toList();
        List<String> itens = new ArrayList<>();
        vigentes.forEach(o -> itens.add(o.getTitulo() + (o.getCupom() == null ? "" : " (cupom " + o.getCupom() + ")")));
        promocao.forEach(p -> itens.add(p.nome() + " por " + reais(p.precoPromocional())));
        if (itens.isEmpty()) return false;
        String texto = String.join(" · ", itens.subList(0, Math.min(4, itens.size())))
                + (itens.size() > 4 ? " e mais " + (itens.size() - 4) : "");
        Long fotoId = vigentes.stream().map(Oferta::getImagemId).filter(java.util.Objects::nonNull).findFirst()
                .orElse(promocao.stream().map(ProdutoDTO::imagemId).filter(java.util.Objects::nonNull).findFirst().orElse(null));
        push.enviarParaTodos(new Notificacao("🎁 Ofertas de hoje na PresentYá", texto, "/ofertas", foto(fotoId)));
        mensageiro.divulgar("Ofertas de hoje", texto, fotoId, "☀️ Bom dia! Ofertas de hoje na PresentYá:");
        return true;
    }

    private String foto(Long id) {
        return id == null ? null : apiUrl + "/api/imagens/" + id;
    }

    private static String reais(BigDecimal v) {
        return NumberFormat.getCurrencyInstance(Locale.of("pt", "BR")).format(v).replace('\u00a0', ' ');
    }

    /**
     * Inscreve o celular e já manda as ofertas do dia, esperando a resposta: se a entrega falhar,
     * a inscrição é desfeita e a pessoa vê o erro em vez de um falso "ativado".
     */
    public void inscreverPush(InscricaoPushRequest req) {
        InscricaoPush i = push.inscrever(req.endpoint(), req.keys().p256dh(), req.keys().auth(),
                ORIGENS.contains(req.origem()) ? req.origem() : "site");
        List<OfertaDTO> vigentes = ofertasVigentes();
        Notificacao boasVindas;
        if (vigentes.isEmpty()) {
            boasVindas = new Notificacao("Notificações ativadas 🎁", "Você vai receber aqui as próximas ofertas da PresentYá.", "/ofertas");
        } else {
            String texto = vigentes.stream().limit(3)
                    .map(o -> o.titulo() + (o.cupom() == null ? "" : " (cupom " + o.cupom() + ")"))
                    .collect(Collectors.joining(" · "))
                    + (vigentes.size() > 3 ? " e mais " + (vigentes.size() - 3) : "");
            boasVindas = new Notificacao("🎁 Ofertas de hoje na PresentYá", texto, "/ofertas");
        }
        String erro = push.enviarAgora(i, boasVindas);
        if (erro != null) {
            push.cancelar(i.getEndpoint());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não conseguimos enviar a notificação: " + erro);
        }
    }

    private Long midia(Long id, String tipo, String erro) {
        if (id == null) return null;
        String t = catalogo.tipoMidia(id);
        if (t == null || !t.startsWith(tipo)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, erro);
        return id;
    }

    /** No ar agora: ativa, já começou (se agendada) e ainda não venceu. */
    public static boolean vigente(Oferta o) {
        return o.isAtivo()
                && (o.getIniciaEm() == null || !o.getIniciaEm().isAfter(LocalDateTime.now(FUSO)))
                && (o.getValidaAte() == null || !o.getValidaAte().isBefore(LocalDate.now(FUSO)));
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
