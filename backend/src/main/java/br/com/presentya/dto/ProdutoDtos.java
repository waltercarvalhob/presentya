package br.com.presentya.dto;

import br.com.presentya.domain.Categoria;
import br.com.presentya.domain.Produto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class ProdutoDtos {
    private ProdutoDtos() {}

    /** Cores entram no SVG da vitrine: só variáveis do tema ou hexadecimal. */
    static final String COR = "^(var\\(--[a-z-]{1,30}\\)|#[0-9a-fA-F]{3,8})?$";

    public record ProdutoDTO(Long id, String nome, String descricao, BigDecimal preco, BigDecimal precoPromocional,
                             BigDecimal precoAtual, Integer estoque, boolean personalizavel, boolean novidade,
                             boolean maisVendido, boolean ativo, Long imagemId, String imagemUrl, String ilustracao,
                             String corFundo, String corA, String corB, Long categoriaId, String categoriaNome,
                             String categoriaSlug, Set<String> ocasioes, Set<String> publicos, int vendidos,
                             Instant criadoEm) {
        public static ProdutoDTO de(Produto p) {
            Categoria c = p.getCategoria();
            return new ProdutoDTO(p.getId(), p.getNome(), p.getDescricao(), p.getPreco(), p.getPrecoPromocional(),
                    p.precoAtual(), p.getEstoque(), p.isPersonalizavel(), p.isNovidade(), p.isMaisVendido(),
                    p.isAtivo(), p.getImagemId(), p.getImagemId() == null ? null : "/api/imagens/" + p.getImagemId(),
                    p.getIlustracao(), p.getCorFundo(), p.getCorA(), p.getCorB(),
                    c == null ? null : c.getId(), c == null ? null : c.getNome(), c == null ? null : c.getSlug(),
                    new TreeSet<>(p.getOcasioes()), new TreeSet<>(p.getPublicos()), p.getVendidos(), p.getCriadoEm());
        }
    }

    public record ProdutoRequest(
            @NotBlank(message = "Informe o nome do produto") @Size(max = 140) String nome,
            @Size(max = 4000) String descricao,
            @NotNull(message = "Informe o preço") @DecimalMin(value = "0.01", message = "Preço inválido") BigDecimal preco,
            @DecimalMin(value = "0.01", message = "Preço promocional inválido") BigDecimal precoPromocional,
            @Min(value = 0, message = "Estoque não pode ser negativo") Integer estoque,
            Boolean personalizavel, Boolean novidade, Boolean maisVendido, Boolean ativo,
            Long imagemId,
            @Pattern(regexp = "^[a-z]{0,20}$", message = "Ilustração inválida") String ilustracao,
            @Pattern(regexp = COR, message = "Cor inválida") String corFundo,
            @Pattern(regexp = COR, message = "Cor inválida") String corA,
            @Pattern(regexp = COR, message = "Cor inválida") String corB,
            Long categoriaId,
            Set<String> ocasioes, Set<String> publicos) {}

    public record CategoriaDTO(Long id, String nome, String slug, Long paiId, int ordem, boolean ativo,
                               List<CategoriaDTO> filhas) {}

    public record CategoriaRequest(
            @NotBlank(message = "Informe o nome da categoria") @Size(max = 80) String nome,
            Long paiId, Integer ordem, Boolean ativo) {}

    public record ImagemDTO(Long id, String url) {}
}
