package br.com.presentya;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Fluxo completo da loja com banco H2 em memória e pagamento simulado. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PresentyaApiApplicationTests {

    @Autowired
    MockMvc mvc;

    private String token(String url, String corpo, int esperado) throws Exception {
        String resp = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().is(esperado)).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(resp, "$.token");
    }

    @Test
    void fluxoDeCompraComPixEPermissoes() throws Exception {
        String admin = token("/api/auth/login", "{\"email\":\"admin@presentya.com.br\",\"senha\":\"Admin@2026\"}", 200);

        String catalogo = mvc.perform(get("/api/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(15)))
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(catalogo, "$[?(@.nome=='Caneca com Foto')].id");
        int caneca = ids.get(0);

        mvc.perform(get("/api/categorias")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(11)));

        String cliente = token("/api/auth/registrar",
                "{\"nome\":\"Cliente Teste\",\"email\":\"cliente@teste.com\",\"senha\":\"segredo1\"}", 201);

        String pedido = """
                {"itens":[{"produtoId":%d,"quantidade":2,"personalizacao":"Ana & João"}],
                 "formaPagamento":"PIX","tipoEntrega":"ENTREGA","nomeDestinatario":"Ana","telefone":"98999990000",
                 "cep":"65000-000","endereco":"Rua Grande","numero":"10","bairro":"Centro","cidade":"Sao Luis","uf":"MA",
                 "subtotal":0.01}
                """.formatted(caneca);
        String criado = mvc.perform(post("/api/pedidos").header("Authorization", cliente)
                        .contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.subtotal").value(99.80))
                .andExpect(jsonPath("$.frete").value(15.00))
                .andExpect(jsonPath("$.total").value(114.80))
                .andExpect(jsonPath("$.pixCopiaCola").value(startsWith("PIX-DE-TESTE")))
                .andReturn().getResponse().getContentAsString();
        String codigo = JsonPath.read(criado, "$.codigo");

        mvc.perform(post("/api/pedidos/" + codigo + "/simular-pagamento").header("Authorization", cliente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAGO"));
        mvc.perform(get("/api/usuarios/me").header("Authorization", cliente))
                .andExpect(jsonPath("$.pontos").value(114)).andExpect(jsonPath("$.comprasPagas").value(1));

        // Perfis: cliente não entra no painel; admin entra.
        mvc.perform(get("/api/admin/painel").header("Authorization", cliente)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/painel").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pagos").value(1))
                .andExpect(jsonPath("$.faturamento").value(greaterThan(0.0)));
        mvc.perform(get("/api/admin/produtos")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCadastraUsuarioComPerfil() throws Exception {
        String admin = token("/api/auth/login", "{\"email\":\"admin@presentya.com.br\",\"senha\":\"Admin@2026\"}", 200);
        String novo = "{\"nome\":\"Técnico Teste\",\"email\":\"Tecnico@Teste.com\",\"senha\":\"segredo1\",\"perfil\":\"TECNICO\"}";

        mvc.perform(post("/api/admin/usuarios").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(novo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("TECNICO"))
                .andExpect(jsonPath("$.email").value("tecnico@teste.com"));
        mvc.perform(post("/api/admin/usuarios").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(novo))
                .andExpect(status().isConflict());

        // O técnico entra no painel de pedidos, mas não pode cadastrar usuários.
        String tecnico = token("/api/auth/login", "{\"email\":\"tecnico@teste.com\",\"senha\":\"segredo1\"}", 200);
        mvc.perform(get("/api/admin/pedidos").header("Authorization", tecnico)).andExpect(status().isOk());
        mvc.perform(post("/api/admin/usuarios").header("Authorization", tecnico)
                        .contentType(MediaType.APPLICATION_JSON).content(novo.replace("Tecnico@", "outro@")))
                .andExpect(status().isForbidden());
    }
}
