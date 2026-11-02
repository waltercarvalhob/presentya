package br.com.presentya.web;

import br.com.presentya.domain.Imagem;
import br.com.presentya.dto.ProdutoDtos.CategoriaDTO;
import br.com.presentya.dto.ProdutoDtos.ProdutoDTO;
import br.com.presentya.service.CatalogoService;
import br.com.presentya.service.ConfiguracaoService;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Rotas públicas da vitrine. */
@RestController
@RequestMapping("/api")
public class LojaController {
    private final CatalogoService catalogo;
    private final ConfiguracaoService config;

    public LojaController(CatalogoService catalogo, ConfiguracaoService config) {
        this.catalogo = catalogo;
        this.config = config;
    }

    @GetMapping("/status")
    public Map<String, String> status() {
        return Map.of("status", "ok");
    }

    @GetMapping("/produtos")
    public List<ProdutoDTO> produtos() {
        return catalogo.produtosAtivos();
    }

    @GetMapping("/produtos/{id}")
    public ProdutoDTO produto(@PathVariable Long id) {
        return catalogo.produto(id, false);
    }

    @GetMapping("/categorias")
    public List<CategoriaDTO> categorias() {
        return catalogo.arvoreCategorias(true);
    }

    @GetMapping("/configuracoes")
    public Map<String, String> configuracoes() {
        return config.todas();
    }

    @GetMapping("/imagens/{id}")
    public ResponseEntity<Resource> imagem(@PathVariable Long id) {
        Imagem i = catalogo.imagem(id);
        // Resource (e não byte[]) faz o Spring atender pedidos de "Range": o Safari só toca vídeo assim.
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(i.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(new ByteArrayResource(i.getDados()));
    }
}
