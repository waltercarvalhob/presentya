package br.com.presentya;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
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
                 "email":"cliente@teste.com",
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
    void compraComoConvidadoELiberaAoCriarConta() throws Exception {
        String catalogo = mvc.perform(get("/api/produtos")).andReturn().getResponse().getContentAsString();
        int produto = ((Number) ((java.util.Map<?, ?>) JsonPath.read(catalogo, "$[0]")).get("id")).intValue();

        String pedido = """
                {"itens":[{"produtoId":%d,"quantidade":1}],
                 "formaPagamento":"PIX","tipoEntrega":"RETIRADA","lojaRetirada":"Loja 1",
                 "nomeDestinatario":"Convidada","telefone":"98999990000","email":"convidada@teste.com"}
                """.formatted(produto);

        // Sem token: a compra é aceita e o pedido pode ser acompanhado só com o código.
        String criado = mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.convidado").value(true))
                .andExpect(jsonPath("$.clienteEmail").value("convidada@teste.com"))
                .andReturn().getResponse().getContentAsString();
        String codigo = JsonPath.read(criado, "$.codigo");
        mvc.perform(get("/api/pedidos/" + codigo)).andExpect(status().isOk());
        mvc.perform(post("/api/pedidos/" + codigo + "/simular-pagamento")).andExpect(status().isOk());

        // Outra pessoa não acha o pedido de ninguém pelo "meus" sem login.
        mvc.perform(get("/api/pedidos/meus")).andExpect(status().isUnauthorized());

        // Ao criar conta com o mesmo e-mail, o pedido de convidado passa a aparecer em "Meus pedidos".
        String cliente = token("/api/auth/registrar",
                "{\"nome\":\"Convidada\",\"email\":\"convidada@teste.com\",\"senha\":\"segredo1\"}", 201);
        mvc.perform(get("/api/pedidos/meus").header("Authorization", cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value(codigo))
                .andExpect(jsonPath("$[0].convidado").value(false));
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

    @Test
    void ofertasDoQrCodeEInteressados() throws Exception {
        String admin = token("/api/auth/login", "{\"email\":\"admin@presentya.com.br\",\"senha\":\"Admin@2026\"}", 200);
        mvc.perform(post("/api/admin/ofertas").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Canecas 20% off\",\"cupom\":\"vitrine20\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.cupom").value("VITRINE20"));
        mvc.perform(post("/api/admin/ofertas").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Vencida\",\"validaAte\":\"2000-01-01\"}"))
                .andExpect(status().isCreated());

        // A página pública só mostra as ofertas ativas e dentro do prazo.
        mvc.perform(get("/api/ofertas")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].titulo").value("Canecas 20% off"));

        String pessoa = "{\"nome\":\"Maria\",\"whatsapp\":\"%s\",\"origem\":\"qrcode\",\"aceite\":%s}";
        mvc.perform(post("/api/interessados").contentType(MediaType.APPLICATION_JSON).content(pessoa.formatted("(98) 99999-0000", false)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/interessados").contentType(MediaType.APPLICATION_JSON).content(pessoa.formatted("(98) 99999-0000", true)))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/interessados").contentType(MediaType.APPLICATION_JSON).content(pessoa.formatted("5598999990000", true)))
                .andExpect(status().isNoContent());

        // Mesmo número em formatos diferentes vira um cadastro só; a lista é só do administrador.
        mvc.perform(get("/api/admin/interessados")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/interessados").header("Authorization", admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].whatsapp").value("5598999990000"))
                .andExpect(jsonPath("$[0].origem").value("qrcode"));
    }

    @Test
    void inscricaoNasNotificacoes() throws Exception {
        String admin = token("/api/auth/login", "{\"email\":\"admin@presentya.com.br\",\"senha\":\"Admin@2026\"}", 200);
        String chave = JsonPath.read(mvc.perform(get("/api/push/chave")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.chave");
        org.junit.jupiter.api.Assertions.assertEquals(65, br.com.presentya.push.WebPush.b64(chave).length);

        String p256dh = br.com.presentya.push.WebPush.b64(br.com.presentya.push.WebPush.pontoBruto(
                (java.security.interfaces.ECPublicKey) br.com.presentya.push.WebPush.novoParDeChaves().getPublic()));
        String inscricao = "{\"endpoint\":\"%s\",\"keys\":{\"p256dh\":\"" + p256dh + "\",\"auth\":\"BTBZMqHH6r4Tts7J_aSIgg\"},\"origem\":\"qrcode\"}";

        // Endereço fora dos serviços de push dos navegadores é recusado.
        mvc.perform(post("/api/push/inscricoes").contentType(MediaType.APPLICATION_JSON).content(inscricao.formatted("https://exemplo.com/x")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/push/inscricoes").contentType(MediaType.APPLICATION_JSON).content(inscricao.formatted("https://fcm.googleapis.com/fcm/send/abc")))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/push/inscricoes").contentType(MediaType.APPLICATION_JSON).content(inscricao.formatted("https://fcm.googleapis.com/fcm/send/abc")))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/admin/push").header("Authorization", admin)).andExpect(jsonPath("$.inscritos").value(1));
        mvc.perform(post("/api/admin/push/enviar").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Hoje\",\"mensagem\":\"Tudo com 10%\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enviadas").value(1));
        mvc.perform(post("/api/admin/push/enviar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Hoje\",\"mensagem\":\"x\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/push/cancelar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpoint\":\"https://fcm.googleapis.com/fcm/send/abc\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/push").header("Authorization", admin)).andExpect(jsonPath("$.inscritos").value(0));
    }

    @Test
    void ofertaComFotoEVideoELeituraDeCodigo() throws Exception {
        String admin = token("/api/auth/login", "{\"email\":\"admin@presentya.com.br\",\"senha\":\"Admin@2026\"}", 200);
        byte[] bytes = new byte[200];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) i;

        String foto = mvc.perform(multipart("/api/admin/midias").file(new MockMultipartFile("arquivo", "f.png", "image/png", bytes))
                        .header("Authorization", admin)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String video = mvc.perform(multipart("/api/admin/midias").file(new MockMultipartFile("arquivo", "v.mp4", "video/mp4", bytes))
                        .header("Authorization", admin)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        mvc.perform(multipart("/api/admin/midias").file(new MockMultipartFile("arquivo", "x.exe", "application/octet-stream", bytes))
                .header("Authorization", admin)).andExpect(status().isBadRequest());
        Integer fotoId = JsonPath.read(foto, "$.id"), videoId = JsonPath.read(video, "$.id");

        // Vídeo responde a "Range" (o Safari do iPhone só toca assim).
        mvc.perform(get("/api/imagens/" + videoId).header("Range", "bytes=0-9"))
                .andExpect(status().isPartialContent())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Content-Range", "bytes 0-9/200"));

        // Foto no lugar do vídeo é recusada; certo, a oferta aparece com as duas mídias.
        String oferta = "{\"titulo\":\"Kit Natal\",\"imagemId\":%d,\"videoId\":%d}";
        mvc.perform(post("/api/admin/ofertas").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content(oferta.formatted(fotoId, fotoId))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/ofertas").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(oferta.formatted(fotoId, videoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imagemUrl").value("/api/imagens/" + fotoId))
                .andExpect(jsonPath("$.videoUrl").value("/api/imagens/" + videoId));

        // Código já cadastrado em um produto: a leitura devolve o produto para editar.
        String produto = "{\"nome\":\"Caneca QR\",\"preco\":10,\"ativo\":false,\"codigoBarras\":\"PRESENTYA-CANECA-01\"}";
        mvc.perform(post("/api/admin/produtos").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(produto))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.codigoBarras").value("PRESENTYA-CANECA-01"));
        mvc.perform(get("/api/admin/produtos/codigo").param("valor", "PRESENTYA-CANECA-01").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.existente.nome").value("Caneca QR"));
        mvc.perform(get("/api/admin/produtos/codigo").param("valor", "PRESENTYA-CANECA-01"))
                .andExpect(status().isUnauthorized());

        org.junit.jupiter.api.Assertions.assertEquals("07891234567895",
                br.com.presentya.service.CodigoBarrasService.gtin("https://id.gs1.org/01/07891234567895/21/abc"));
        org.junit.jupiter.api.Assertions.assertNull(br.com.presentya.service.CodigoBarrasService.gtin("https://presentya.vercel.app"));
    }
    @Autowired
    br.com.presentya.mensageiro.TelegramService telegram;

    @Test
    void roboDeOfertasNoWhatsappENoTelegram() throws Exception {
        String admin = token("/api/auth/login", "{\"email\":\"admin@presentya.com.br\",\"senha\":\"Admin@2026\"}", 200);

        // Meta confere o webhook com o token de verificação.
        mvc.perform(get("/api/whatsapp/webhook").param("hub.mode", "subscribe").param("hub.verify_token", "verifica-presentya")
                .param("hub.challenge", "12345")).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("12345"));
        mvc.perform(get("/api/whatsapp/webhook").param("hub.mode", "subscribe").param("hub.verify_token", "errado")
                .param("hub.challenge", "12345")).andExpect(status().isForbidden());

        // Mensagem do QR Code no WhatsApp: só aceita com a assinatura da Meta.
        String msg = "{\"entry\":[{\"changes\":[{\"value\":{\"contacts\":[{\"profile\":{\"name\":\"Ana Lima\"},\"wa_id\":\"5598999990001\"}],"
                + "\"messages\":[{\"from\":\"5598999990001\",\"type\":\"text\",\"text\":{\"body\":\"Quero receber as ofertas da PresentYá (QR da vitrine)\"}}]}}]}]}";
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec("segredo-do-app".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        String assinatura = "sha256=" + java.util.HexFormat.of().formatHex(mac.doFinal(msg.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        mvc.perform(post("/api/whatsapp/webhook").contentType(MediaType.APPLICATION_JSON).content(msg).header("X-Hub-Signature-256", "sha256=00"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/whatsapp/webhook").contentType(MediaType.APPLICATION_JSON).content(msg).header("X-Hub-Signature-256", assinatura))
                .andExpect(status().isOk());

        // Telegram: "/start qrcode" vindo do QR; sem o segredo do webhook é recusado.
        String inicio = "{\"message\":{\"chat\":{\"id\":777001},\"from\":{\"first_name\":\"Bia\"},\"text\":\"/start qrcode\"}}";
        mvc.perform(post("/api/telegram/webhook").contentType(MediaType.APPLICATION_JSON).content(inicio)).andExpect(status().isForbidden());
        mvc.perform(post("/api/telegram/webhook").contentType(MediaType.APPLICATION_JSON).content(inicio)
                .header("X-Telegram-Bot-Api-Secret-Token", telegram.segredoWebhook())).andExpect(status().isOk());

        mvc.perform(get("/api/admin/canais").header("Authorization", admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsapp.contatos").value(1))
                .andExpect(jsonPath("$.telegram.contatos").value(1))
                .andExpect(jsonPath("$.whatsapp.link").value(startsWith("https://wa.me/")));

        // "PARAR" tira a pessoa da lista.
        mvc.perform(post("/api/telegram/webhook").contentType(MediaType.APPLICATION_JSON)
                .content(inicio.replace("/start qrcode", "PARAR")).header("X-Telegram-Bot-Api-Secret-Token", telegram.segredoWebhook()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/canais").header("Authorization", admin)).andExpect(jsonPath("$.telegram.contatos").value(0));
        mvc.perform(get("/api/admin/canais")).andExpect(status().isUnauthorized());
    }
    @Autowired
    br.com.presentya.service.PromocaoService promocaoService;

    @Autowired
    br.com.presentya.repository.OfertaRepository ofertaRepository;

    @Test
    void automacoesAgendadaDiariaEPromocaoDeProduto() throws Exception {
        String admin = token("/api/auth/login", "{\"email\":\"admin@presentya.com.br\",\"senha\":\"Admin@2026\"}", 200);
        java.time.LocalDateTime agora = java.time.LocalDateTime.now(java.time.ZoneId.of("America/Fortaleza")).withNano(0);
        // Horário de envio aberto o dia todo, para o teste não depender da hora em que roda.
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/configuracoes")
                .header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"envio_diario_hora\":\"00:00\",\"envio_diario_fim\":\"23:59\"}")).andExpect(status().isOk());

        // Agendada para amanhã: fica fora da página e sem anúncio até a hora chegar.
        String criada = mvc.perform(post("/api/admin/ofertas").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Agendada Natal\",\"iniciaEm\":\"" + agora.plusDays(1) + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.anunciadaEm").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        Long id = ((Number) JsonPath.read(criada, "$.id")).longValue();
        mvc.perform(get("/api/ofertas")).andExpect(jsonPath("$[?(@.titulo==\"Agendada Natal\")]", hasSize(0)));

        // A hora chegou: a agenda solta a oferta e marca como anunciada (uma vez só).
        var o = ofertaRepository.findById(id).orElseThrow();
        o.setIniciaEm(agora.minusMinutes(1));
        ofertaRepository.save(o);
        org.junit.jupiter.api.Assertions.assertEquals(1, promocaoService.anunciarAgendadas());
        org.junit.jupiter.api.Assertions.assertEquals(0, promocaoService.anunciarAgendadas());
        mvc.perform(get("/api/ofertas")).andExpect(jsonPath("$[?(@.titulo==\"Agendada Natal\")]", hasSize(1)));

        // Envio diário: no máximo uma vez por dia.
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/configuracoes")
                .header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"envio_diario_ativo\":\"true\"}")).andExpect(status().isOk());
        promocaoService.envioDiarioSePreciso();
        org.junit.jupiter.api.Assertions.assertFalse(promocaoService.envioDiarioSePreciso());

        // Fora do horário de envio, a oferta nova espera na fila; quando o horário abre, ela sai.
        java.time.LocalTime agoraHora = agora.toLocalTime();
        String fechado = "{\"envio_diario_hora\":\"%s\",\"envio_diario_fim\":\"%s\"}"
                .formatted(agoraHora.plusHours(2).withSecond(0), agoraHora.plusHours(3).withSecond(0));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/configuracoes")
                .header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(fechado)).andExpect(status().isOk());
        mvc.perform(post("/api/admin/ofertas").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Oferta da madrugada\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.anunciadaEm").doesNotExist());
        org.junit.jupiter.api.Assertions.assertEquals(0, promocaoService.anunciarAgendadas());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/configuracoes")
                .header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"envio_diario_hora\":\"00:00\",\"envio_diario_fim\":\"23:59\"}")).andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(1, promocaoService.anunciarAgendadas());
        mvc.perform(get("/api/admin/ofertas").header("Authorization", admin))
                .andExpect(jsonPath("$[?(@.titulo==\"Oferta da madrugada\" && @.anunciadaEm)]", hasSize(1)));

        // Produto que entra em promoção é salvo normalmente (o anúncio sai depois do commit).
        String produto = mvc.perform(post("/api/admin/produtos").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Kit Promo\",\"preco\":100,\"precoPromocional\":80}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/admin/produtos/" + JsonPath.read(produto, "$.id"))
                .header("Authorization", admin)).andExpect(status().isNoContent());
    }
}
