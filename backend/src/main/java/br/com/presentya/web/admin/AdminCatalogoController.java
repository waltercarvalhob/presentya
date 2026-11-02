package br.com.presentya.web.admin;

import br.com.presentya.dto.ProdutoDtos.CategoriaDTO;
import br.com.presentya.dto.ProdutoDtos.CategoriaRequest;
import br.com.presentya.dto.ProdutoDtos.ImagemDTO;
import br.com.presentya.dto.ProdutoDtos.ProdutoDTO;
import br.com.presentya.dto.ProdutoDtos.ProdutoRequest;
import br.com.presentya.dto.ProdutoDtos.CodigoDTO;
import br.com.presentya.service.CatalogoService;
import br.com.presentya.service.CodigoBarrasService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Produtos e imagens: ADMIN e TECNICO. Categorias: só ADMIN (regra em SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminCatalogoController {
    private final CatalogoService catalogo;
    private final CodigoBarrasService codigos;

    public AdminCatalogoController(CatalogoService catalogo, CodigoBarrasService codigos) {
        this.catalogo = catalogo;
        this.codigos = codigos;
    }

    /** Código lido pela câmera (ou digitado/leitor USB): produto já cadastrado ou dados das bases públicas. */
    @GetMapping("/produtos/codigo")
    public CodigoDTO codigo(@RequestParam("valor") String valor) {
        return codigos.consultar(valor);
    }

    @GetMapping("/produtos")
    public List<ProdutoDTO> produtos() {
        return catalogo.todosProdutos();
    }

    @GetMapping("/produtos/{id}")
    public ProdutoDTO produto(@PathVariable Long id) {
        return catalogo.produto(id, true);
    }

    @PostMapping("/produtos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoDTO criar(@Valid @RequestBody ProdutoRequest req) {
        return catalogo.salvarProduto(null, req);
    }

    @PutMapping("/produtos/{id}")
    public ProdutoDTO atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoRequest req) {
        return catalogo.salvarProduto(id, req);
    }

    @DeleteMapping("/produtos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        catalogo.removerProduto(id);
    }

    @PostMapping("/imagens")
    @ResponseStatus(HttpStatus.CREATED)
    public ImagemDTO enviarImagem(@RequestParam("arquivo") MultipartFile arquivo) {
        return catalogo.salvarImagem(arquivo);
    }

    /** Foto ou vídeo (ofertas e promoções). */
    @PostMapping("/midias")
    @ResponseStatus(HttpStatus.CREATED)
    public ImagemDTO enviarMidia(@RequestParam("arquivo") MultipartFile arquivo) {
        return catalogo.salvarMidia(arquivo);
    }

    @GetMapping("/categorias")
    public List<CategoriaDTO> categorias() {
        return catalogo.arvoreCategorias(false);
    }

    @PostMapping("/categorias")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaDTO criarCategoria(@Valid @RequestBody CategoriaRequest req) {
        return catalogo.salvarCategoria(null, req);
    }

    @PutMapping("/categorias/{id}")
    public CategoriaDTO atualizarCategoria(@PathVariable Long id, @Valid @RequestBody CategoriaRequest req) {
        return catalogo.salvarCategoria(id, req);
    }

    @DeleteMapping("/categorias/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerCategoria(@PathVariable Long id) {
        catalogo.removerCategoria(id);
    }
}
