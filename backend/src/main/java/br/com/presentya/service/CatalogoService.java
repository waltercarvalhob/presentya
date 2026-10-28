package br.com.presentya.service;

import br.com.presentya.domain.Categoria;
import br.com.presentya.domain.Imagem;
import br.com.presentya.domain.Produto;
import br.com.presentya.dto.ProdutoDtos.CategoriaDTO;
import br.com.presentya.dto.ProdutoDtos.CategoriaRequest;
import br.com.presentya.dto.ProdutoDtos.ImagemDTO;
import br.com.presentya.dto.ProdutoDtos.ProdutoDTO;
import br.com.presentya.dto.ProdutoDtos.ProdutoRequest;
import br.com.presentya.repository.CategoriaRepository;
import br.com.presentya.repository.ImagemRepository;
import br.com.presentya.repository.ProdutoRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CatalogoService {
    private static final Set<String> TIPOS_IMAGEM = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final ProdutoRepository produtos;
    private final CategoriaRepository categorias;
    private final ImagemRepository imagens;

    public CatalogoService(ProdutoRepository produtos, CategoriaRepository categorias, ImagemRepository imagens) {
        this.produtos = produtos;
        this.categorias = categorias;
        this.imagens = imagens;
    }

    // ---------- produtos ----------

    @Transactional(readOnly = true)
    public List<ProdutoDTO> produtosAtivos() {
        return produtos.findByAtivoTrueOrderByVendidosDesc().stream().map(ProdutoDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public List<ProdutoDTO> todosProdutos() {
        return produtos.findAllByOrderByIdDesc().stream().map(ProdutoDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public ProdutoDTO produto(Long id, boolean incluirInativo) {
        Produto p = buscarProduto(id);
        if (!p.isAtivo() && !incluirInativo) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado");
        }
        return ProdutoDTO.de(p);
    }

    @Transactional
    public ProdutoDTO salvarProduto(Long id, ProdutoRequest req) {
        Produto p = id == null ? new Produto() : buscarProduto(id);
        p.setNome(req.nome().trim());
        p.setDescricao(req.descricao());
        p.setPreco(req.preco());
        p.setPrecoPromocional(req.precoPromocional());
        p.setEstoque(req.estoque());
        p.setPersonalizavel(Boolean.TRUE.equals(req.personalizavel()));
        p.setNovidade(Boolean.TRUE.equals(req.novidade()));
        p.setMaisVendido(Boolean.TRUE.equals(req.maisVendido()));
        p.setAtivo(req.ativo() == null || req.ativo());
        if (req.imagemId() != null && !imagens.existsById(req.imagemId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Imagem não encontrada");
        }
        p.setImagemId(req.imagemId());
        if (req.ilustracao() != null && !req.ilustracao().isBlank()) p.setIlustracao(req.ilustracao());
        if (req.corFundo() != null && !req.corFundo().isBlank()) p.setCorFundo(req.corFundo());
        if (req.corA() != null && !req.corA().isBlank()) p.setCorA(req.corA());
        if (req.corB() != null && !req.corB().isBlank()) p.setCorB(req.corB());
        p.setCategoria(req.categoriaId() == null ? null : buscarCategoria(req.categoriaId()));
        p.setOcasioes(limpar(req.ocasioes()));
        p.setPublicos(limpar(req.publicos()));
        return ProdutoDTO.de(produtos.save(p));
    }

    /** Produtos que já foram vendidos não são apagados, só desativados, para manter o histórico. */
    @Transactional
    public void removerProduto(Long id) {
        Produto p = buscarProduto(id);
        if (p.getVendidos() > 0) {
            p.setAtivo(false);
        } else {
            produtos.delete(p);
        }
    }

    private Produto buscarProduto(Long id) {
        return produtos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));
    }

    private static Set<String> limpar(Set<String> valores) {
        Set<String> s = new HashSet<>();
        if (valores != null) valores.stream().filter(v -> v != null && !v.isBlank()).map(Slugs::de).forEach(s::add);
        return s;
    }

    // ---------- categorias ----------

    @Transactional(readOnly = true)
    public List<CategoriaDTO> arvoreCategorias(boolean somenteAtivas) {
        List<Categoria> todas = categorias.findAllByOrderByOrdemAscNomeAsc().stream()
                .filter(c -> !somenteAtivas || c.isAtivo()).toList();
        Map<Long, List<CategoriaDTO>> filhas = new LinkedHashMap<>();
        for (Categoria c : todas) {
            if (c.getPai() != null) {
                filhas.computeIfAbsent(c.getPai().getId(), k -> new ArrayList<>()).add(dto(c, List.of()));
            }
        }
        return todas.stream().filter(c -> c.getPai() == null)
                .map(c -> dto(c, filhas.getOrDefault(c.getId(), List.of()))).toList();
    }

    private static CategoriaDTO dto(Categoria c, List<CategoriaDTO> filhas) {
        return new CategoriaDTO(c.getId(), c.getNome(), c.getSlug(), c.getPai() == null ? null : c.getPai().getId(),
                c.getOrdem(), c.isAtivo(), filhas);
    }

    @Transactional
    public CategoriaDTO salvarCategoria(Long id, CategoriaRequest req) {
        Categoria c = id == null ? new Categoria() : buscarCategoria(id);
        Categoria pai = req.paiId() == null ? null : buscarCategoria(req.paiId());
        if (pai != null && (pai.getPai() != null || pai.getId().equals(c.getId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use no máximo dois níveis de categoria");
        }
        if (pai != null && id != null && categorias.existsByPaiId(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta categoria tem subcategorias e não pode virar subcategoria");
        }
        c.setNome(req.nome().trim());
        c.setPai(pai);
        c.setOrdem(req.ordem() == null ? 0 : req.ordem());
        c.setAtivo(req.ativo() == null || req.ativo());
        if (c.getSlug() == null) c.setSlug(slugUnico(pai == null ? c.getNome() : pai.getSlug() + "-" + c.getNome()));
        categorias.save(c);
        return dto(c, List.of());
    }

    @Transactional
    public void removerCategoria(Long id) {
        Categoria c = buscarCategoria(id);
        if (categorias.existsByPaiId(id) || produtos.existsByCategoriaId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Categoria em uso. Mova os produtos e subcategorias antes, ou apenas desative-a.");
        }
        categorias.delete(c);
    }

    private Categoria buscarCategoria(Long id) {
        return categorias.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria não encontrada"));
    }

    private String slugUnico(String base) {
        String slug = Slugs.de(base), tentativa = slug;
        for (int i = 2; categorias.existsBySlug(tentativa); i++) tentativa = slug + "-" + i;
        return tentativa;
    }

    // ---------- imagens ----------

    @Transactional
    public ImagemDTO salvarImagem(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie um arquivo de imagem");
        }
        if (!TIPOS_IMAGEM.contains(arquivo.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use imagens JPG, PNG, WEBP ou GIF");
        }
        try {
            Imagem i = new Imagem();
            i.setContentType(arquivo.getContentType());
            i.setDados(arquivo.getBytes());
            imagens.save(i);
            return new ImagemDTO(i.getId(), "/api/imagens/" + i.getId());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler a imagem");
        }
    }

    @Transactional(readOnly = true)
    public Imagem imagem(Long id) {
        return imagens.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagem não encontrada"));
    }
}
