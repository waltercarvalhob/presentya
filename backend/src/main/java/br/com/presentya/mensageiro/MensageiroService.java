package br.com.presentya.mensageiro;

import br.com.presentya.domain.ContatoMensageiro;
import br.com.presentya.domain.ContatoMensageiro.Canal;
import br.com.presentya.domain.Oferta;
import br.com.presentya.repository.ContatoMensageiroRepository;
import br.com.presentya.repository.OfertaRepository;
import br.com.presentya.service.PromocaoService;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Robô de ofertas no WhatsApp e no Telegram: quem lê o QR Code e manda a mensagem já recebe as ofertas
 * do dia, e toda oferta nova vai sozinha para todos os contatos.
 */
@Service
public class MensageiroService {
    private static final Logger log = LoggerFactory.getLogger(MensageiroService.class);
    private static final Set<String> SAIR = Set.of("parar", "sair", "cancelar", "/parar", "/stop", "stop", "descadastrar");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");
    /** Margem antes das 24 h da Meta, para não ter mensagem recusada no limite. */
    private static final Duration JANELA_WHATSAPP = Duration.ofHours(23);

    /** Oferta pronta para envio (sem entidade JPA, para ir para outra thread). */
    record Peca(String titulo, String descricao, String cupom, String validade, String foto, String video) {}

    private final ContatoMensageiroRepository contatos;
    private final OfertaRepository ofertas;
    private final TelegramService telegram;
    private final WhatsappService whatsapp;
    private final String apiUrl;
    private final String paginaOfertas;
    private final ExecutorService fila = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "envio-mensageiro");
        t.setDaemon(true);
        return t;
    });
    private volatile String ultimoEnvio;

    public MensageiroService(ContatoMensageiroRepository contatos, OfertaRepository ofertas, TelegramService telegram,
                             WhatsappService whatsapp, @Value("${app.api-url}") String apiUrl,
                             @Value("${app.frontend-url}") String frontendUrl) {
        this.contatos = contatos;
        this.ofertas = ofertas;
        this.telegram = telegram;
        this.whatsapp = whatsapp;
        this.apiUrl = apiUrl.replaceAll("/+$", "");
        this.paginaOfertas = frontendUrl.split(",")[0].trim().replaceAll("/+$", "") + "/ofertas";
    }

    /** Mensagem que chegou pelo webhook de um dos canais. */
    public void recebido(Canal canal, String destino, String nome, String texto, String origem) {
        String t = texto == null ? "" : texto.strip().toLowerCase();
        if (SAIR.contains(t)) {
            contatos.deleteByCanalAndDestino(canal, destino);
            fila.submit(() -> registrar(texto(canal, destino, "Pronto, você não vai mais receber ofertas da PresentYá. "
                    + "Para voltar, é só mandar qualquer mensagem aqui. 💛")));
            return;
        }
        ContatoMensageiro c = contatos.findByCanalAndDestino(canal, destino).orElseGet(() -> new ContatoMensageiro(canal, destino));
        boolean novo = c.getId() == null;
        if (nome != null && !nome.isBlank()) c.setNome(nome.length() > 120 ? nome.substring(0, 120) : nome);
        if (c.getOrigem() == null) c.setOrigem(origem);
        c.setUltimaMensagemEm(Instant.now());
        contatos.save(c);
        List<Peca> vigentes = vigentes();
        String primeiroNome = c.getNome() == null ? "" : ", " + c.getNome().split(" ")[0];
        fila.submit(() -> boasVindas(canal, destino, primeiroNome, novo, vigentes));
    }

    /** Oferta que acabou de passar a valer: vai para todos os contatos dos dois canais. */
    public void divulgar(Oferta o) {
        divulgar(peca(o), "🎁 Nova oferta na PresentYá!");
    }

    /** Anúncio que não é uma oferta cadastrada (produto em promoção, resumo do dia). */
    public void divulgar(String titulo, String descricao, Long fotoId, String cabecalho) {
        divulgar(new Peca(titulo, descricao, null, null, fotoId == null ? null : apiUrl + "/api/imagens/" + fotoId, null), cabecalho);
    }

    private void divulgar(Peca p, String cabecalho) {
        if (!telegram.ativo() && !whatsapp.ativo()) return;
        List<ContatoMensageiro> todos = contatos.findAll();
        Instant limite = Instant.now().minus(JANELA_WHATSAPP);
        for (ContatoMensageiro c : todos) {
            Canal canal = c.getCanal();
            String destino = c.getDestino();
            boolean naJanela = c.getUltimaMensagemEm() != null && c.getUltimaMensagemEm().isAfter(limite);
            fila.submit(() -> {
                String erro;
                if (canal == Canal.WHATSAPP && !naJanela) {
                    if (!whatsapp.temModelo()) return; // sem modelo aprovado a Meta recusaria
                    erro = whatsapp.enviarModelo(destino, p.titulo());
                } else {
                    erro = enviarPeca(canal, destino, p, cabecalho);
                }
                if (TelegramService.BLOQUEADO.equals(erro)) contatos.deleteByCanalAndDestino(canal, destino);
                registrar(erro);
            });
        }
    }

    /** Links que abrem a conversa com a loja (públicos: viram botões na página de ofertas). */
    public Map<String, String> links(String whatsappPadrao) {
        // Sem emoji: o redirecionamento do wa.me estraga caracteres de 4 bytes e a mensagem chegaria com "�".
        String texto = "Quero receber as ofertas da PresentYá (vitrine)";
        String link = telegram.link("qrcode");
        return Map.of("whatsapp", "https://wa.me/" + whatsapp.numero(whatsappPadrao) + "?text="
                        + java.net.URLEncoder.encode(texto, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"),
                "telegram", link == null ? "" : link);
    }

    public Map<String, Object> resumo(String whatsappPadrao) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("whatsapp", Map.of("ativo", whatsapp.ativo(), "modelo", whatsapp.temModelo(),
                "contatos", contatos.countByCanal(Canal.WHATSAPP), "link", links(whatsappPadrao).get("whatsapp")));
        Map<String, Object> tg = new LinkedHashMap<>();
        tg.put("ativo", telegram.ativo() && telegram.usuarioBot() != null);
        tg.put("bot", telegram.usuarioBot() == null ? "" : telegram.usuarioBot());
        tg.put("contatos", contatos.countByCanal(Canal.TELEGRAM));
        tg.put("link", telegram.link("qrcode") == null ? "" : telegram.link("qrcode"));
        r.put("telegram", tg);
        r.put("ultimoEnvio", ultimoEnvio == null ? "" : ultimoEnvio);
        return r;
    }

    private void boasVindas(Canal canal, String destino, String primeiroNome, boolean novo, List<Peca> vigentes) {
        String oi = (novo ? "Oi" + primeiroNome + "! 👋 Que bom ter você aqui. " : "Oi de novo" + primeiroNome + "! ")
                + (vigentes.isEmpty()
                    ? "Agora você recebe as ofertas da PresentYá aqui, assim que saírem. Hoje ainda não tem oferta, mas fica de olho!"
                    : "Estas são as ofertas de hoje da PresentYá 🎁. As próximas chegam aqui sozinhas.");
        registrar(texto(canal, destino, oi + "\n\nPara não receber mais, mande PARAR."));
        for (Peca p : vigentes.stream().limit(5).toList()) {
            String erro = enviarPeca(canal, destino, p, null);
            registrar(erro);
            if (erro != null) break;
        }
    }

    private String enviarPeca(Canal canal, String destino, Peca p, String cabecalho) {
        String legenda = legenda(canal, p, cabecalho);
        if (canal == Canal.TELEGRAM) {
            String erro = p.video() != null ? telegram.enviarVideo(destino, p.video(), legenda, "Ver todas as ofertas", paginaOfertas)
                    : p.foto() != null ? telegram.enviarFoto(destino, p.foto(), legenda, "Ver todas as ofertas", paginaOfertas) : null;
            // Vídeo/foto que o Telegram não conseguiu baixar: manda pelo menos o texto.
            if (erro == null && (p.video() != null || p.foto() != null)) return null;
            if (TelegramService.BLOQUEADO.equals(erro)) return erro;
            return telegram.enviarTexto(destino, legenda, "Ver todas as ofertas", paginaOfertas);
        }
        String comLink = legenda + "\n\nVeja todas: " + paginaOfertas;
        String erro = p.video() != null ? whatsapp.enviarVideo(destino, p.video(), comLink)
                : p.foto() != null ? whatsapp.enviarFoto(destino, p.foto(), comLink) : null;
        if (erro == null && (p.video() != null || p.foto() != null)) return null;
        return whatsapp.enviarTexto(destino, comLink);
    }

    private String texto(Canal canal, String destino, String texto) {
        return canal == Canal.TELEGRAM ? telegram.enviarTexto(destino, TelegramService.html(texto), null, null)
                : whatsapp.enviarTexto(destino, texto);
    }

    private static String legenda(Canal canal, Peca p, String cabecalho) {
        boolean tg = canal == Canal.TELEGRAM;
        String titulo = tg ? "<b>" + TelegramService.html(p.titulo()) + "</b>" : "*" + p.titulo() + "*";
        StringBuilder s = new StringBuilder();
        if (cabecalho != null) s.append(cabecalho).append("\n\n");
        s.append("🎁 ").append(titulo);
        if (p.descricao() != null) s.append("\n").append(tg ? TelegramService.html(p.descricao()) : p.descricao());
        if (p.cupom() != null) s.append("\n🏷️ Cupom: ").append(tg ? "<code>" + TelegramService.html(p.cupom()) + "</code>" : "*" + p.cupom() + "*");
        if (p.validade() != null) s.append("\n⏰ Válida até ").append(p.validade());
        return s.length() > 1000 ? s.substring(0, 999) + "…" : s.toString();
    }

    private List<Peca> vigentes() {
        return ofertas.findAllByOrderByOrdemAscCriadoEmDesc().stream().filter(PromocaoService::vigente).map(this::peca).toList();
    }

    private Peca peca(Oferta o) {
        return new Peca(o.getTitulo(), o.getDescricao(), o.getCupom(),
                o.getValidaAte() == null ? null : o.getValidaAte().format(DIA),
                o.getImagemId() == null ? null : apiUrl + "/api/imagens/" + o.getImagemId(),
                o.getVideoId() == null ? null : apiUrl + "/api/imagens/" + o.getVideoId());
    }

    private void registrar(String erro) {
        ultimoEnvio = Instant.now() + " " + (erro == null ? "OK" : erro);
        if (erro != null && !TelegramService.BLOQUEADO.equals(erro)) log.warn("Envio de oferta falhou: {}", erro);
    }
}
