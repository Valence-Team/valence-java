package br.com.valence.api.controllers;

import br.com.valence.api.IntegracaoTesteBase;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MuralModeracaoTest extends IntegracaoTesteBase {

    private static final String DENUNCIA_GRAVE = """
            {"categoria":"RECOMENDACAO_PERIGOSA","gravidade":"ALTA","acao":"OCULTAR_E_REVISAR",
            "justificativa":"Indica uso de veneno sem orientação técnica."}
            """;

    @Test
    void cicloCompletoDaPublicacaoComComentario() throws Exception {
        String municipio = cidade();
        String autor = novoProdutor(municipio, "SP");
        String leitor = novoProdutor(municipio, "SP");
        long post = publicar(autor, municipio, "Alguém viu lagarta no milho esta semana?");

        mockMvc.perform(get("/api/mural").header(HttpHeaders.AUTHORIZATION, bearer(leitor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(post))
                .andExpect(jsonPath("$.content[0].autor.papel").value("PRODUTOR"));
        mockMvc.perform(put("/api/mural/{id}", post).header(HttpHeaders.AUTHORIZATION, bearer(autor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("texto", "Alguém viu lagarta-do-cartucho no milho?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texto").value("Alguém viu lagarta-do-cartucho no milho?"));
        mockMvc.perform(put("/api/mural/{id}", post).header(HttpHeaders.AUTHORIZATION, bearer(leitor))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("texto", "Edição indevida"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/mural/{id}/comentarios", post).header(HttpHeaders.AUTHORIZATION, bearer(leitor))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("texto", "Vi sim, perto do rio."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.texto").value("Vi sim, perto do rio."));
        mockMvc.perform(get("/api/mural/{id}/comentarios", post).header(HttpHeaders.AUTHORIZATION, bearer(autor)))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/mural/{id}", post).header(HttpHeaders.AUTHORIZATION, bearer(leitor)))
                .andExpect(jsonPath("$.totalComentarios").value(1));
        mockMvc.perform(delete("/api/mural/{id}", post).header(HttpHeaders.AUTHORIZATION, bearer(autor)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/mural/{id}", post).header(HttpHeaders.AUTHORIZATION, bearer(leitor)))
                .andExpect(status().isNotFound());
    }

    @Test
    void denunciaGraveOcultaPublicacaoEModeradorDecide() throws Exception {
        responderLlm(DENUNCIA_GRAVE);
        String municipio = cidade();
        String autor = novoProdutor(municipio, "SP");
        String denunciante = novoProdutor(municipio, "SP");
        long post = publicar(autor, municipio, "Passe bastante veneno, quanto mais melhor.");

        mockMvc.perform(denunciar(denunciante, post))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoriaIa").value("RECOMENDACAO_PERIGOSA"))
                .andExpect(jsonPath("$.gravidadeIa").value("ALTA"))
                .andExpect(jsonPath("$.statusPost").value("OCULTO"));
        mockMvc.perform(denunciar(denunciante, post)).andExpect(status().isConflict());
        mockMvc.perform(get("/api/mural").header(HttpHeaders.AUTHORIZATION, bearer(denunciante)))
                .andExpect(jsonPath("$.content[*].id", not(hasItem((int) post))));

        String gestor = tokenGestor();
        mockMvc.perform(get("/api/moderacao?size=50").header(HttpHeaders.AUTHORIZATION, bearer(gestor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].postId", hasItem((int) post)));
        mockMvc.perform(patch("/api/moderacao/{id}", post).header(HttpHeaders.AUTHORIZATION, bearer(gestor))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("decisao", "MANTER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.categoriaIa").value("RECOMENDACAO_PERIGOSA"));
        mockMvc.perform(get("/api/mural").header(HttpHeaders.AUTHORIZATION, bearer(denunciante)))
                .andExpect(jsonPath("$.content[*].id", hasItem((int) post)));
    }

    @Test
    void dadoPessoalOcultaSemEnviarTextoAoModelo() throws Exception {
        String municipio = cidade();
        String autor = novoProdutor(municipio, "SP");
        long post = publicar(autor, municipio, "Vendo semente, ligue (16) 99999-1234.");

        mockMvc.perform(denunciar(novoProdutor(municipio, "SP"), post))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoriaIa").value("DADOS_PESSOAIS"))
                .andExpect(jsonPath("$.statusPost").value("OCULTO"));
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void modeloForaDoArDeixaDenunciaPendenteParaHumano() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("HTTP 503"));
        String municipio = cidade();
        long post = publicar(novoProdutor(municipio, "SP"), municipio, "Melhor plantar milho em outubro.");

        mockMvc.perform(denunciar(novoProdutor(municipio, "SP"), post))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusPost").value("ATIVO"));
        mockMvc.perform(get("/api/moderacao?size=50").header(HttpHeaders.AUTHORIZATION, bearer(tokenAgronomo())))
                .andExpect(jsonPath("$.content[*].postId", hasItem((int) post)));
    }

    @Test
    void moderadorRemovePublicacao() throws Exception {
        String municipio = cidade();
        long post = publicar(novoProdutor(municipio, "SP"), municipio, "Compre meu curso milagroso!");
        mockMvc.perform(denunciar(novoProdutor(municipio, "SP"), post)).andExpect(status().isCreated());

        mockMvc.perform(patch("/api/moderacao/{id}", post).header(HttpHeaders.AUTHORIZATION, bearer(tokenAgronomo()))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("decisao", "REMOVER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REMOVIDO"));
        mockMvc.perform(get("/api/mural/{id}", post).header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor())))
                .andExpect(status().isNotFound());
    }

    @Test
    void autorNaoDenunciaAPropriaPublicacao() throws Exception {
        String municipio = cidade();
        String autor = novoProdutor(municipio, "SP");
        long post = publicar(autor, municipio, "Minha lavoura está ótima.");
        mockMvc.perform(denunciar(autor, post)).andExpect(status().isUnprocessableEntity());
    }

    private long publicar(String token, String municipio, String texto) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/mural").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("texto", texto, "municipio", municipio, "uf", "SP"))))
                .andExpect(status().isCreated())
                .andReturn();
        return lerJson(resultado).get("id").asLong();
    }

    private RequestBuilder denunciar(String token, long post) throws Exception {
        return post("/api/mural/{id}/denuncias", post).header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("motivo", "Conteúdo inadequado.")));
    }

    private static String cidade() {
        return "Cidade " + UUID.randomUUID().toString().substring(0, 8);
    }
}
