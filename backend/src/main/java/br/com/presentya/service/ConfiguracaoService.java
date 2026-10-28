package br.com.presentya.service;

import br.com.presentya.domain.Configuracao;
import br.com.presentya.repository.ConfiguracaoRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dados da loja editáveis pelo administrador. Valores padrão vêm do site original e das sugestões. */
@Service
public class ConfiguracaoService {
    public static final Map<String, String> PADRAO = new LinkedHashMap<>();
    static {
        PADRAO.put("whatsapp", "5598984084256");
        PADRAO.put("whatsapp_exibicao", "(98) 98408-4256");
        PADRAO.put("email", "presentya2026@gmail.com");
        PADRAO.put("instagram", "presentya_");
        PADRAO.put("site", "presentya.com.br");
        PADRAO.put("horario_resumo", "Seg a Dom");
        PADRAO.put("loja1_nome", "Pátio Norte Shopping");
        PADRAO.put("loja1_horario", "Seg a sáb: 10h às 22h\nDomingo: 13h às 21h");
        PADRAO.put("loja2_nome", "Quiosque Mix Mateus Forquilha");
        PADRAO.put("loja2_horario", "Seg a sáb: 10h às 19h\nDomingo: 9h às 15h");
        PADRAO.put("cidade_entrega", "São Luís");
        PADRAO.put("frete_cidade", "15.00");
        PADRAO.put("frete_brasil", "35.00");
        PADRAO.put("aviso_topo", "");
    }

    private final ConfiguracaoRepository repo;

    public ConfiguracaoService(ConfiguracaoRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public Map<String, String> todas() {
        Map<String, String> m = new LinkedHashMap<>(PADRAO);
        repo.findAll().forEach(c -> m.put(c.getChave(), c.getValor()));
        return m;
    }

    public String valor(String chave) {
        return repo.findById(chave).map(Configuracao::getValor).orElse(PADRAO.getOrDefault(chave, ""));
    }

    public BigDecimal decimal(String chave) {
        try {
            return new BigDecimal(valor(chave).replace(",", ".").trim());
        } catch (NumberFormatException e) {
            return new BigDecimal(PADRAO.get(chave));
        }
    }

    /** Só aceita as chaves conhecidas, para ninguém gravar lixo no banco. */
    @Transactional
    public Map<String, String> salvar(Map<String, String> valores) {
        valores.forEach((k, v) -> {
            if (PADRAO.containsKey(k)) {
                String texto = v == null ? "" : v.trim();
                repo.save(new Configuracao(k, texto.length() > 4000 ? texto.substring(0, 4000) : texto));
            }
        });
        return todas();
    }
}
