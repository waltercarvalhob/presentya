package br.com.presentya.service;

import br.com.presentya.dto.ProdutoDtos.CodigoDTO;
import br.com.presentya.dto.ProdutoDtos.ImagemDTO;
import br.com.presentya.dto.ProdutoDtos.ProdutoDTO;
import br.com.presentya.repository.ProdutoRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Leitura do código de barras/QR Code no cadastro de produto: primeiro procura no próprio catálogo,
 * depois nas bases públicas (Cosmos, se houver token; Open Food Facts; UPCitemdb) e baixa a foto.
 */
@Service
public class CodigoBarrasService {
    private static final Logger log = LoggerFactory.getLogger(CodigoBarrasService.class);
    private static final Pattern GTIN = Pattern.compile("\\d{8,14}");
    /** QR Code no padrão GS1 Digital Link (ex.: https://id.gs1.org/01/07891234567895) traz o GTIN na URL. */
    private static final Pattern GS1_LINK = Pattern.compile("/01/(\\d{8,14})(?:[/?]|$)");

    private final ProdutoRepository produtos;
    private final CatalogoService catalogo;
    private final String cosmosToken;
    private final RestClient http;

    public CodigoBarrasService(ProdutoRepository produtos, CatalogoService catalogo,
                               @Value("${app.cosmos.token:}") String cosmosToken) {
        this.produtos = produtos;
        this.catalogo = catalogo;
        this.cosmosToken = cosmosToken == null ? "" : cosmosToken.trim();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6)).followRedirects(HttpClient.Redirect.NORMAL).build());
        fabrica.setReadTimeout(Duration.ofSeconds(10));
        this.http = RestClient.builder().requestFactory(fabrica)
                .defaultHeader("User-Agent", "PresentYa/1.0 (presentya2026@gmail.com)").build();
    }

    private record Achado(String nome, String descricao, String foto, String fonte) {}

    @Transactional
    public CodigoDTO consultar(String lido) {
        String codigo = lido == null ? "" : lido.strip();
        if (codigo.length() > 200) codigo = codigo.substring(0, 200);
        Optional<ProdutoDTO> existente = produtos.findFirstByCodigoBarras(codigo).map(ProdutoDTO::de);
        if (existente.isPresent()) return new CodigoDTO(codigo, existente.get(), null, null, null, null, "catálogo");

        String gtin = gtin(codigo);
        if (gtin == null) return new CodigoDTO(codigo, null, null, null, null, null, null);
        for (Achado a : List.of(cosmos(gtin), openFoodFacts(gtin), upcItemDb(gtin))) {
            if (a == null || a.nome() == null || a.nome().isBlank()) continue;
            ImagemDTO foto = baixarFoto(a.foto());
            String nome = a.nome().strip();
            return new CodigoDTO(codigo, null, nome.length() > 140 ? nome.substring(0, 140) : nome, a.descricao(),
                    foto == null ? null : foto.id(), foto == null ? null : foto.url(), a.fonte());
        }
        return new CodigoDTO(codigo, null, null, null, null, null, null);
    }

    public static String gtin(String codigo) {
        if (GTIN.matcher(codigo).matches()) return codigo;
        Matcher m = GS1_LINK.matcher(codigo);
        return m.find() ? m.group(1) : null;
    }

    private Achado cosmos(String gtin) {
        if (cosmosToken.isEmpty()) return null;
        Map<?, ?> r = json("https://api.cosmos.bluesoft.com.br/gtins/" + gtin + ".json", Map.of("X-Cosmos-Token", cosmosToken));
        if (r == null) return null;
        Map<?, ?> marca = r.get("brand") instanceof Map<?, ?> m ? m : Map.of();
        return new Achado(texto(r.get("description")), juntar("Marca: ", texto(marca.get("name"))), texto(r.get("thumbnail")), "Cosmos");
    }

    private Achado openFoodFacts(String gtin) {
        Map<?, ?> r = json("https://world.openfoodfacts.org/api/v2/product/" + gtin
                + ".json?fields=product_name_pt,product_name,brands,image_front_url,image_url", Map.of());
        if (r == null || !(r.get("product") instanceof Map<?, ?> p)) return null;
        String nome = texto(p.get("product_name_pt"));
        if (nome == null) nome = texto(p.get("product_name"));
        String foto = texto(p.get("image_front_url"));
        return new Achado(nome, juntar("Marca: ", texto(p.get("brands"))), foto != null ? foto : texto(p.get("image_url")), "Open Food Facts");
    }

    private Achado upcItemDb(String gtin) {
        Map<?, ?> r = json("https://api.upcitemdb.com/prod/trial/lookup?upc=" + gtin, Map.of());
        if (r == null || !(r.get("items") instanceof List<?> itens) || itens.isEmpty() || !(itens.get(0) instanceof Map<?, ?> i)) return null;
        String foto = i.get("images") instanceof List<?> fotos && !fotos.isEmpty() ? texto(fotos.get(0)) : null;
        String descricao = texto(i.get("description"));
        return new Achado(texto(i.get("title")), descricao != null ? descricao : juntar("Marca: ", texto(i.get("brand"))), foto, "UPCitemdb");
    }

    private Map<?, ?> json(String url, Map<String, String> cabecalhos) {
        try {
            return http.get().uri(URI.create(url)).headers(h -> cabecalhos.forEach(h::set)).retrieve().body(Map.class);
        } catch (RestClientException | IllegalArgumentException e) {
            log.debug("Consulta de código sem resultado em {}: {}", URI.create(url).getHost(), e.getMessage());
            return null;
        }
    }

    private ImagemDTO baixarFoto(String url) {
        if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) return null;
        try {
            ResponseEntity<byte[]> r = http.get().uri(URI.create(url)).retrieve().toEntity(byte[].class);
            String tipo = r.getHeaders().getContentType() == null ? null : r.getHeaders().getContentType().toString();
            return catalogo.salvarImagem(tipo, r.getBody());
        } catch (RestClientException | IllegalArgumentException e) {
            log.debug("Foto do produto não baixada ({}): {}", url, e.getMessage());
            return null;
        }
    }

    private static String texto(Object o) {
        return o == null || o.toString().isBlank() ? null : o.toString().strip();
    }

    private static String juntar(String prefixo, String valor) {
        return valor == null ? null : prefixo + valor;
    }
}
