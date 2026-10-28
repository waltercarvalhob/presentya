package br.com.presentya.config;

import br.com.presentya.domain.Categoria;
import br.com.presentya.domain.Perfil;
import br.com.presentya.domain.Produto;
import br.com.presentya.domain.Usuario;
import br.com.presentya.repository.CategoriaRepository;
import br.com.presentya.repository.ProdutoRepository;
import br.com.presentya.repository.UsuarioRepository;
import br.com.presentya.service.Slugs;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Na primeira execução (banco vazio) cria o administrador, as categorias e os produtos do site original. */
@Component
public class DataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UsuarioRepository usuarios;
    private final CategoriaRepository categorias;
    private final ProdutoRepository produtos;
    private final PasswordEncoder senhas;
    private final String adminEmail;
    private final String adminSenha;

    public DataSeeder(UsuarioRepository usuarios, CategoriaRepository categorias, ProdutoRepository produtos,
                      PasswordEncoder senhas, @Value("${app.admin.email}") String adminEmail,
                      @Value("${app.admin.senha}") String adminSenha) {
        this.usuarios = usuarios;
        this.categorias = categorias;
        this.produtos = produtos;
        this.senhas = senhas;
        this.adminEmail = adminEmail;
        this.adminSenha = adminSenha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.count() == 0) {
            Usuario admin = new Usuario();
            admin.setNome("Administrador PresentYá");
            admin.setEmail(adminEmail.toLowerCase());
            admin.setSenhaHash(senhas.encode(adminSenha));
            admin.setPerfil(Perfil.ADMIN);
            usuarios.save(admin);
            log.warn("Administrador criado: {} — troque a senha em 'Minha conta' após o primeiro acesso.", adminEmail);
        }
        if (categorias.count() == 0) criarCategorias();
        if (produtos.count() == 0) criarProdutos();
    }

    private final Map<String, Categoria> porSlug = new HashMap<>();

    private Categoria cat(String nome, Categoria pai, int ordem) {
        Categoria c = new Categoria();
        c.setNome(nome);
        c.setPai(pai);
        c.setOrdem(ordem);
        c.setSlug(Slugs.de(pai == null ? nome : pai.getSlug() + "-" + nome));
        categorias.save(c);
        porSlug.put(c.getSlug(), c);
        return c;
    }

    /** Estrutura do menu sugerida pela loja (documento sugestões.docx). */
    private void criarCategorias() {
        int o = 0;
        cat("Kit Presente", null, o++);
        Categoria ela = cat("Para Ela", null, o++);
        cat("Bolsas e Mochilas Femininas", ela, 0);
        cat("Necessaire", ela, 1);
        cat("Carteiras", ela, 2);
        cat("Óculos", ela, 3);
        cat("Relógios", ela, 4);
        Categoria ele = cat("Para Ele", null, o++);
        cat("Bolsas e Mochilas Masculinas", ele, 0);
        cat("Óculos", ele, 1);
        cat("Relógios", ele, 2);
        cat("Carteiras", ele, 3);
        cat("Cintos", ele, 4);
        cat("Necessaire", ele, 5);
        cat("Personalizados", null, o++);
        cat("Acessórios para Celular", null, o++);
        cat("Produtos 3D", null, o++);
        cat("Eletrônicos", null, o++);
        cat("Infantil", null, o++);
        cat("Chaveiros", null, o++);
        cat("Embalagem para Presente", null, o++);
        cat("Cartão Presente", null, o++);
    }

    private void produto(String nome, String desc, String preco, String categoria, List<String> ocasioes,
                         List<String> publicos, boolean perso, String ilustracao, String bg, String a, String b,
                         boolean novidade, boolean maisVendido, int vendidos, int diasAtras) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setDescricao(desc);
        p.setPreco(new BigDecimal(preco));
        p.setCategoria(porSlug.get(categoria));
        p.setOcasioes(Set.copyOf(ocasioes));
        p.setPublicos(Set.copyOf(publicos));
        p.setPersonalizavel(perso);
        p.setIlustracao(ilustracao);
        p.setCorFundo(bg);
        p.setCorA(a);
        p.setCorB(b);
        p.setNovidade(novidade);
        p.setMaisVendido(maisVendido);
        p.setVendidos(vendidos);
        p.setCriadoEm(Instant.now().minus(diasAtras, ChronoUnit.DAYS));
        produtos.save(p);
    }

    /** Os 12 presentes do site original + cartões presente. Troque por fotos e preços reais no painel. */
    private void criarProdutos() {
        if (porSlug.isEmpty()) categorias.findAll().forEach(c -> porSlug.put(c.getSlug(), c));
        produto("Caixa Surpresa Amor", "Camadas de carinho: bombons, cartinhas e uma lembrança personalizada no fundo.", "149.90",
                "kit-presente", List.of("namoro", "aniversario"), List.of("ela", "ele"), true, "box",
                "var(--rose-soft)", "var(--rose)", "var(--gold-bright)", false, true, 10, 9);
        produto("Caneca com Foto", "A foto de vocês estampada para lembrar do momento a cada café.", "49.90",
                "personalizados", List.of("aniversario", "namoro", "maes", "pais"), List.of("ela", "ele", "familia"), true, "mug",
                "var(--sky)", "var(--card)", "var(--rose)", false, true, 9, 10);
        produto("Quadro Memórias", "Uma foto especial emoldurada com data e frase, pronta para a parede.", "89.90",
                "personalizados", List.of("namoro", "maes", "aniversario"), List.of("ela", "familia"), true, "frame",
                "var(--paper)", "var(--sage)", "var(--gold-bright)", false, false, 7, 8);
        produto("Cesta Café da Manhã", "Delícias, flores e cartão escrito à mão para começar o dia sorrindo.", "189.90",
                "kit-presente", List.of("aniversario", "maes", "namoro", "pais"), List.of("ela", "ele", "familia"), false, "basket",
                "var(--gold-soft)", "var(--rose)", "var(--gold)", false, true, 8, 11);
        produto("Luminária com Nome", "Luz suave com o nome de quem você ama. Linda no quarto das crianças.", "119.90",
                "infantil", List.of("aniversario", "natal"), List.of("criancas", "ela"), true, "lamp",
                "var(--rose-soft)", "var(--gold-soft)", "var(--gold-bright)", true, false, 6, 2);
        produto("Ursinho + Bombons", "Pelúcia macia com bombons e laço. Abraço garantido.", "79.90",
                "infantil", List.of("aniversario", "namoro"), List.of("criancas", "ela"), false, "bear",
                "var(--sage-soft)", "var(--gold-soft)", "var(--rose)", false, false, 5, 7);
        produto("Kit Spa Relax", "Vela aromática, sais de banho e hidratante. Um convite para descansar.", "129.90",
                "kit-presente", List.of("maes", "aniversario"), List.of("ela", "familia"), false, "spa",
                "var(--sage-soft)", "var(--rose)", "var(--gold-soft)", true, false, 6, 3);
        produto("Chaveiro Iniciais", "As iniciais de vocês sempre por perto. Pequeno no tamanho, enorme no significado.", "34.90",
                "chaveiros", List.of("namoro", "aniversario", "pais"), List.of("ele", "ela"), true, "key",
                "var(--paper)", "var(--rose)", "var(--gold-bright)", false, false, 4, 6);
        produto("Kit Natal em Família", "Mimos natalinos e um enfeite com o sobrenome da família.", "159.90",
                "kit-presente", List.of("natal"), List.of("familia"), true, "tree",
                "var(--sage-soft)", "var(--rose)", "var(--gold-bright)", true, false, 5, 1);
        produto("Kit Corporativo", "Caneca, bloco e chocolate com a marca da sua empresa.", "99.90",
                "kit-presente", List.of("corporativo", "natal", "pais"), List.of("empresa", "ele"), true, "tall",
                "var(--sky)", "var(--paper)", "var(--gold)", false, false, 6, 5);
        produto("Coração de Bombons", "Caixa em coração com bombons finos e cartão. O clássico que funciona.", "59.90",
                "kit-presente", List.of("namoro", "maes"), List.of("ela"), false, "choc",
                "var(--rose-soft)", "var(--rose)", "var(--gold-bright)", false, false, 7, 4);
        produto("Box Aniversário", "Balão, confete, doces e cartão com o nome do aniversariante.", "139.90",
                "kit-presente", List.of("aniversario"), List.of("ele", "ela", "criancas"), true, "box",
                "var(--gold-soft)", "var(--sage)", "var(--rose)", true, true, 8, 0);
        for (String valor : List.of("50", "100", "200")) {
            produto("Cartão Presente R$ " + valor, "Deixe a pessoa escolher o próprio presente na PresentYá. Vale em qualquer produto da loja.",
                    valor + ".00", "cartao-presente", List.of("aniversario", "namoro", "natal", "maes", "pais", "corporativo"),
                    List.of("ela", "ele", "familia", "empresa"), true, "card",
                    "var(--gold-soft)", "var(--brand)", "var(--ink)", false, false, 0, 12);
        }
        log.info("Catálogo inicial criado com {} produtos.", produtos.count());
    }
}
