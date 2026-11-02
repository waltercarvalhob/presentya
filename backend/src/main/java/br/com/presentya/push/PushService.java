package br.com.presentya.push;

import br.com.presentya.domain.ChavesPush;
import br.com.presentya.domain.InscricaoPush;
import br.com.presentya.repository.ChavesPushRepository;
import br.com.presentya.repository.InscricaoPushRepository;
import br.com.presentya.service.ConfiguracaoService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Guarda quem aceitou notificações e entrega as mensagens em segundo plano. */
@Service
public class PushService {
    private static final Logger log = LoggerFactory.getLogger(PushService.class);
    /** Só aceitamos endereços dos serviços de push dos navegadores (evita usar o servidor para chamar qualquer URL). */
    private static final List<String> SERVICOS = List.of("fcm.googleapis.com", "android.googleapis.com",
            "push.services.mozilla.com", "push.apple.com", "notify.windows.com");

    /** imagem: endereço completo de uma foto, que o Android mostra grande na notificação (opcional). */
    public record Notificacao(String titulo, String mensagem, String url, String imagem) {
        public Notificacao(String titulo, String mensagem, String url) {
            this(titulo, mensagem, url, null);
        }

        String json() {
            return "{\"title\":" + js(titulo, 80) + ",\"body\":" + js(mensagem, 300) + ",\"url\":" + js(url, 200)
                    + (imagem == null ? "" : ",\"image\":" + js(imagem, 300)) + "}";
        }
    }

    private final InscricaoPushRepository inscricoes;
    private final ChavesPushRepository chavesRepo;
    private final ConfiguracaoService config;
    private final boolean envioAtivo;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    /** Threads comuns, não virtuais: no Java 21 do Render o envio feito em thread virtual não chegava a sair. */
    private final ExecutorService fila = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "envio-push");
        t.setDaemon(true);
        return t;
    });
    private volatile ChavesPush chaves;

    public PushService(InscricaoPushRepository inscricoes, ChavesPushRepository chavesRepo, ConfiguracaoService config,
                       @Value("${app.push.envio-ativo:true}") boolean envioAtivo) {
        this.inscricoes = inscricoes;
        this.chavesRepo = chavesRepo;
        this.config = config;
        this.envioAtivo = envioAtivo;
    }

    /** As chaves VAPID nascem no primeiro uso; trocar invalidaria todas as inscrições existentes. */
    private synchronized ChavesPush chaves() {
        if (chaves == null) {
            chaves = chavesRepo.findById(ChavesPush.ID).orElseGet(() -> {
                KeyPair kp = WebPush.novoParDeChaves();
                return chavesRepo.save(new ChavesPush(WebPush.b64(WebPush.pontoBruto((ECPublicKey) kp.getPublic())),
                        WebPush.b64(kp.getPrivate().getEncoded())));
            });
        }
        return chaves;
    }

    public String chavePublica() {
        return chaves().getPublica();
    }

    public InscricaoPush inscrever(String endpoint, String p256dh, String auth, String origem) {
        validar(endpoint, p256dh, auth);
        InscricaoPush i = inscricoes.findByEndpoint(endpoint).orElseGet(InscricaoPush::new);
        i.setEndpoint(endpoint);
        i.setP256dh(p256dh);
        i.setAuth(auth);
        if (i.getOrigem() == null) i.setOrigem(origem);
        return inscricoes.save(i);
    }

    public void cancelar(String endpoint) {
        inscricoes.deleteByEndpoint(endpoint);
    }

    public long inscritos() {
        return inscricoes.count();
    }

    /** Enfileira para todos os inscritos e devolve quantos são. */
    public int enviarParaTodos(Notificacao n) {
        List<InscricaoPush> todos = inscricoes.findAll();
        todos.forEach(i -> enviar(i, n));
        return todos.size();
    }

    public void enviar(InscricaoPush i, Notificacao n) {
        if (!envioAtivo) return;
        String endpoint = i.getEndpoint(), p256dh = i.getP256dh(), auth = i.getAuth();
        fila.submit(() -> entregar(endpoint, p256dh, auth, n));
    }

    /** Envia já, esperando a resposta: usado na ativação, para a pessoa saber na hora se funcionou. */
    public String enviarAgora(InscricaoPush i, Notificacao n) {
        return envioAtivo ? entregar(i.getEndpoint(), i.getP256dh(), i.getAuth(), n) : null;
    }

    /** Resultado do último envio, para o painel do administrador. */
    public String ultimoEnvio() {
        return ultimoEnvio;
    }

    private volatile String ultimoEnvio;

    /** Devolve null quando o serviço de push aceitou; senão, o motivo da falha. */
    private String entregar(String endpoint, String p256dh, String auth, Notificacao n) {
        String erro = tentarEntregar(endpoint, p256dh, auth, n);
        ultimoEnvio = Instant.now() + " " + (erro == null ? "OK" : erro);
        if (erro != null) log.warn("Falha ao enviar push para {}: {}", URI.create(endpoint).getHost(), erro);
        return erro;
    }

    private String tentarEntregar(String endpoint, String p256dh, String auth, Notificacao n) {
        try {
            ChavesPush c = chaves();
            byte[] corpo = WebPush.cifrar(n.json().getBytes(StandardCharsets.UTF_8), WebPush.b64(p256dh), WebPush.b64(auth));
            String jwt = WebPush.jwtVapid(endpoint, "mailto:" + config.valor("email"),
                    WebPush.chavePrivada(WebPush.b64(c.getPrivada())), Instant.now().plus(Duration.ofHours(12)).getEpochSecond());
            HttpRequest req = HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(20))
                    .header("TTL", "86400")
                    .header("Urgency", "normal")
                    .header("Content-Encoding", "aes128gcm")
                    .header("Content-Type", "application/octet-stream")
                    .header("Authorization", "vapid t=" + jwt + ", k=" + c.getPublica())
                    .POST(HttpRequest.BodyPublishers.ofByteArray(corpo)).build();
            HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
            // 404/410: a pessoa desativou as notificações ou desinstalou o navegador; tira da lista.
            if (r.statusCode() == 404 || r.statusCode() == 410) inscricoes.deleteByEndpoint(endpoint);
            if (r.statusCode() >= 400) {
                String corpoResposta = r.body() == null ? "" : r.body().strip();
                return "recusado pelo serviço de push (" + r.statusCode() + ")"
                        + (corpoResposta.isEmpty() ? "" : ": " + corpoResposta.substring(0, Math.min(200, corpoResposta.length())));
            }
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "envio interrompido";
        } catch (Exception e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static void validar(String endpoint, String p256dh, String auth) {
        try {
            URI u = URI.create(endpoint);
            String host = u.getHost() == null ? "" : u.getHost().toLowerCase();
            boolean conhecido = SERVICOS.stream().anyMatch(s -> host.equals(s) || host.endsWith("." + s));
            if ("https".equals(u.getScheme()) && conhecido
                    && WebPush.b64(p256dh).length == 65 && WebPush.b64(auth).length == 16) return;
        } catch (IllegalArgumentException e) {
            // cai no erro abaixo
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inscrição de notificação inválida");
    }

    private static String js(String s, int max) {
        String v = s == null ? "" : s.length() > max ? s.substring(0, max - 1) + "…" : s;
        StringBuilder b = new StringBuilder("\"");
        for (char ch : v.toCharArray()) {
            switch (ch) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                default -> b.append(ch < 0x20 ? ' ' : ch);
            }
        }
        return b.append('"').toString();
    }
}
