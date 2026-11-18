package br.com.presentya.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Plano gratuito do Render: o servidor dorme após 15 min sem visitas e leva ~30 s para acordar.
 * Dentro do horário das lojas, a API chama o próprio endereço público a cada 10 min para não dormir.
 * Fora do horário ela dorme normalmente (economiza as 750 h/mês do Render e as horas do banco no Neon).
 */
@Component
public class MantemAcordado {
    private static final Logger log = LoggerFactory.getLogger(MantemAcordado.class);
    private static final ZoneId FUSO = ZoneId.of("America/Fortaleza");

    private final boolean ativo;
    private final URI status;
    private final LocalTime inicio;
    private final LocalTime fim;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public MantemAcordado(@Value("${app.manter-acordado.ativo:true}") boolean ativo,
                          @Value("${app.api-url}") String apiUrl,
                          @Value("${app.manter-acordado.inicio:09:00}") String inicio,
                          @Value("${app.manter-acordado.fim:22:00}") String fim) {
        this.ativo = ativo && apiUrl.startsWith("https://");
        this.status = URI.create(apiUrl.replaceAll("/+$", "") + "/api/status");
        this.inicio = LocalTime.parse(inicio.trim());
        this.fim = LocalTime.parse(fim.trim());
        if (this.ativo) log.info("Mantendo a API acordada das {} às {} (horário de São Luís)", this.inicio, this.fim);
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 600_000)
    public void cutucar() {
        LocalTime agora = LocalTime.now(FUSO);
        if (!ativo || agora.isBefore(inicio) || !agora.isBefore(fim)) return;
        try {
            http.send(HttpRequest.newBuilder(status).timeout(Duration.ofSeconds(30)).GET().build(),
                    HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.debug("Falha ao manter a API acordada: {}", e.getMessage());
        }
    }
}
