package br.com.valence.api;

import br.com.valence.api.clients.IaServiceClient;
import br.com.valence.api.dtos.eventos.OcorrenciaTriadaEventoDto;
import br.com.valence.api.dtos.ia.ClasseVisaoDto;
import br.com.valence.api.dtos.ia.RagResultadoDto;
import br.com.valence.api.dtos.ia.TrechoRagDto;
import br.com.valence.api.dtos.ia.VisaoResultadoDto;
import br.com.valence.api.enums.Cultura;
import br.com.valence.api.enums.StatusOcorrencia;
import br.com.valence.api.repositories.OcorrenciaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegracaoTesteBase {

    protected static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I',
            'H', 'D', 'R'};

    protected static final String TRIAGEM_VALIDA = """
            {"hipoteses":[{"praga":"Ferrugem-comum","probabilidade":0.82},
            {"praga":"Helmintosporiose","probabilidade":0.10}],
            "severidade":3,
            "cuidadosImediatos":["Isolar a área afetada","Aplicar inseticida na dose de 2 ml por litro",
            "Fotografar as folhas das plantas vizinhas"],
            "textoAlerta":"Possível ferrugem-comum em milho na região. Observe pústulas nas folhas.",
            "encaminharAgronomo":false}
            """;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected OcorrenciaRepository ocorrenciaRepository;

    @Autowired
    protected TransactionTemplate transactionTemplate;

    @MockitoBean
    protected ChatModel chatModel;

    @MockitoBean
    protected IaServiceClient iaServiceClient;

    @MockitoBean
    protected RabbitTemplate rabbitTemplate;

    protected String login(String email, String senha) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "senha", senha))))
                .andExpect(status().isOk())
                .andReturn();
        return lerJson(resultado).get("token").asText();
    }

    protected String tokenAgronomo() throws Exception {
        return login("agronomo@valence.local", "Agronomo@2026");
    }

    protected String tokenGestor() throws Exception {
        return login("gestor@valence.local", "Gestor@2026");
    }

    protected String novoProdutor() throws Exception {
        return novoProdutor("Campinas", "SP");
    }

    protected String novoProdutor(String municipio, String uf) throws Exception {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("nome", "Produtor Teste");
        corpo.put("email", "produtor-" + UUID.randomUUID() + "@teste.com");
        corpo.put("senha", "Senha@1234");
        corpo.put("municipio", municipio);
        corpo.put("uf", uf);
        MvcResult resultado = mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(corpo)))
                .andExpect(status().isCreated())
                .andReturn();
        return lerJson(resultado).get("token").asText();
    }

    protected long criarPropriedade(String token, double latitude, double longitude, String... culturas)
            throws Exception {
        return criarPropriedade(token, "Campinas", "SP", latitude, longitude, culturas);
    }

    protected long criarPropriedade(String token, String municipio, String uf, double latitude, double longitude,
                                    String... culturas) throws Exception {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("nome", "Sítio Teste");
        corpo.put("municipio", municipio);
        corpo.put("uf", uf);
        corpo.put("latitude", latitude);
        corpo.put("longitude", longitude);
        corpo.put("areaHectares", 10.0);
        corpo.put("culturas", List.of(culturas));
        MvcResult resultado = mockMvc.perform(post("/api/propriedades")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(corpo)))
                .andExpect(status().isCreated())
                .andReturn();
        return lerJson(resultado).get("id").asLong();
    }

    protected long criarOcorrencia(String token, long propriedadeId, String cultura) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/ocorrencias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("propriedadeId", propriedadeId, "cultura", cultura,
                                "descricao", "Folhas com pústulas alaranjadas na parte de baixo."))))
                .andExpect(status().isCreated())
                .andReturn();
        return lerJson(resultado).get("id").asLong();
    }

    protected MvcResult enviarFoto(String token, long ocorrenciaId, byte[] conteudo) throws Exception {
        return mockMvc.perform(multipart("/api/ocorrencias/{id}/fotos", ocorrenciaId)
                        .file(new MockMultipartFile("foto", "folha.png", "image/png", conteudo))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andReturn();
    }

    protected void alterarStatus(long ocorrenciaId, StatusOcorrencia status) {
        transactionTemplate.executeWithoutResult(transacao ->
                ocorrenciaRepository.findById(ocorrenciaId).orElseThrow().setStatus(status));
    }

    protected OcorrenciaTriadaEventoDto eventoTriada(long ocorrenciaId) {
        return new OcorrenciaTriadaEventoDto(UUID.randomUUID(), OcorrenciaTriadaEventoDto.TIPO, 1, Instant.now(),
                ocorrenciaId, Cultura.MILHO, "Ferrugem-comum", 3, 0.8, "Campinas", "SP", Instant.now());
    }

    protected void responderLlm(String resposta) {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(resposta)))));
    }

    protected void visaoReconhece(String nome, double confianca) {
        when(iaServiceClient.classificar(any(), any(), any(), any()))
                .thenReturn(new VisaoResultadoDto(true, null, "MILHO",
                        List.of(new ClasseVisaoDto("milho_ferrugem_comum", nome, confianca))));
    }

    protected void ragResponde() {
        when(iaServiceClient.consultarRag(any(), any(), any()))
                .thenReturn(new RagResultadoDto(List.of(new TrechoRagDto("Ferrugem-comum do milho",
                        "Pústulas alaranjadas nas duas faces da folha.", "Embrapa",
                        "https://www.embrapa.br", 0.91)), "Explicação de teste com base na ficha.",
                        "openai/gpt-oss-20b"));
    }

    protected JsonNode lerJson(MvcResult resultado) throws Exception {
        return objectMapper.readTree(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    protected String json(Object valor) throws Exception {
        return objectMapper.writeValueAsString(valor);
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }
}
