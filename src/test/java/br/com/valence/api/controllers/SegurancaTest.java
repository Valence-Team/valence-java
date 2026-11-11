package br.com.valence.api.controllers;

import br.com.valence.api.IntegracaoTesteBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SegurancaTest extends IntegracaoTesteBase {

    @Test
    void semTokenDevolve401NoFormatoDoContrato() throws Exception {
        mockMvc.perform(get("/api/propriedades"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.erro").value("Não autenticado"))
                .andExpect(jsonPath("$.caminho").value("/api/propriedades"));
    }

    @Test
    void tokenInvalidoDevolve401() throws Exception {
        mockMvc.perform(get("/api/propriedades").header(HttpHeaders.AUTHORIZATION, "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem", containsString("inválido")));
    }

    @Test
    void produtorNaoAcessaFilaDoAgronomo() throws Exception {
        mockMvc.perform(get("/api/analises").header(HttpHeaders.AUTHORIZATION, bearer(novoProdutor())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void agronomoAcessaFilaDeAnalise() throws Exception {
        mockMvc.perform(get("/api/analises").header(HttpHeaders.AUTHORIZATION, bearer(tokenAgronomo())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void produtorNaoEmiteLaudo() throws Exception {
        mockMvc.perform(post("/api/ocorrencias/1/laudo").header(HttpHeaders.AUTHORIZATION, bearer(novoProdutor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("decisao", "DESCARTADA", "parecer", "Teste"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void agronomoNaoCadastraPropriedade() throws Exception {
        mockMvc.perform(get("/api/propriedades").header(HttpHeaders.AUTHORIZATION, bearer(tokenAgronomo())))
                .andExpect(status().isForbidden());
    }

    @Test
    void produtorNaoAcessaModeracao() throws Exception {
        mockMvc.perform(get("/api/moderacao").header(HttpHeaders.AUTHORIZATION, bearer(novoProdutor())))
                .andExpect(status().isForbidden());
    }

    @Test
    void gestorAcessaModeracao() throws Exception {
        mockMvc.perform(get("/api/moderacao").header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor())))
                .andExpect(status().isOk());
    }

    @Test
    void documentacaoESaudeSaoPublicas() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    }

    @Test
    void rotaInexistenteDevolve404NoFormatoDoContrato() throws Exception {
        mockMvc.perform(get("/api/rota-que-nao-existe").header(HttpHeaders.AUTHORIZATION, bearer(novoProdutor())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
