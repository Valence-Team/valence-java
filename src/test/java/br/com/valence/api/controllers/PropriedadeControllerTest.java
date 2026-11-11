package br.com.valence.api.controllers;

import br.com.valence.api.IntegracaoTesteBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PropriedadeControllerTest extends IntegracaoTesteBase {

    @Test
    void crudCompletoComPaginacao() throws Exception {
        String token = novoProdutor();
        MvcResult criada = mockMvc.perform(post("/api/propriedades").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(propriedade("Sítio Novo", List.of("MILHO")))))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.culturas[0]").value("MILHO"))
                .andExpect(jsonPath("$.uf").value("SP"))
                .andReturn();
        long id = lerJson(criada).get("id").asLong();

        mockMvc.perform(get("/api/propriedades/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Sítio Novo"));

        mockMvc.perform(get("/api/propriedades?page=0&size=5").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.last").value(true));

        mockMvc.perform(put("/api/propriedades/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(propriedade("Sítio Atualizado", List.of("MILHO", "BATATA")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Sítio Atualizado"))
                .andExpect(jsonPath("$.culturas.length()").value(2));

        mockMvc.perform(delete("/api/propriedades/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/propriedades/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Propriedade não encontrada."));
    }

    @Test
    void dadosInvalidosDevolvem400ComCampos() throws Exception {
        Map<String, Object> corpo = propriedade("Sítio", List.of());
        corpo.put("latitude", 200.0);
        corpo.put("areaHectares", -1);
        mockMvc.perform(post("/api/propriedades").header(HttpHeaders.AUTHORIZATION, bearer(novoProdutor()))
                        .contentType(MediaType.APPLICATION_JSON).content(json(corpo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItems("latitude", "areaHectares", "culturas")));
    }

    @Test
    void culturaForaDoEscopoDevolveMensagemClara() throws Exception {
        mockMvc.perform(post("/api/propriedades").header(HttpHeaders.AUTHORIZATION, bearer(novoProdutor()))
                        .contentType(MediaType.APPLICATION_JSON).content(json(propriedade("Sítio", List.of("ARROZ")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("culturas[0]"))
                .andExpect(jsonPath("$.campos[0].mensagem").value("Valor inválido. Use: MILHO, TOMATE, BATATA."));
    }

    @Test
    void propriedadeDeOutroProdutorNaoAparece() throws Exception {
        long id = criarPropriedade(novoProdutor(), -22.90, -47.06, "MILHO");
        mockMvc.perform(get("/api/propriedades/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(novoProdutor())))
                .andExpect(status().isNotFound());
    }

    @Test
    void naoRemovePropriedadeComOcorrencia() throws Exception {
        String token = novoProdutor();
        long id = criarPropriedade(token, -22.91, -47.07, "MILHO");
        criarOcorrencia(token, id, "MILHO");
        mockMvc.perform(delete("/api/propriedades/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict());
    }

    private Map<String, Object> propriedade(String nome, List<String> culturas) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("nome", nome);
        corpo.put("municipio", "Campinas");
        corpo.put("uf", "sp");
        corpo.put("latitude", -22.9056);
        corpo.put("longitude", -47.0608);
        corpo.put("areaHectares", 15.5);
        corpo.put("culturas", culturas);
        return corpo;
    }
}
