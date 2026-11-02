package br.com.presentya.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Relógio das automações: a cada minuto solta as ofertas agendadas cuja hora chegou e faz o envio diário.
 * No plano gratuito do Render o servidor dorme sem visitas; ao acordar, tudo que estava pendente sai na hora.
 */
@Configuration
@EnableScheduling
public class AgendaPromocoes {
    private static final Logger log = LoggerFactory.getLogger(AgendaPromocoes.class);
    private final PromocaoService promocao;
    private final boolean ativa;

    public AgendaPromocoes(PromocaoService promocao, @Value("${app.agenda.ativa:true}") boolean ativa) {
        this.promocao = promocao;
        this.ativa = ativa;
    }

    @Scheduled(initialDelay = 30_000, fixedDelay = 60_000)
    public void executar() {
        if (!ativa) return;
        try {
            int agendadas = promocao.anunciarAgendadas();
            if (agendadas > 0) log.info("{} oferta(s) agendada(s) entraram no ar e foram enviadas", agendadas);
            if (promocao.envioDiarioSePreciso()) log.info("Envio diário das ofertas feito");
        } catch (RuntimeException e) {
            log.warn("Falha na agenda de promoções: {}", e.getMessage());
        }
    }
}
