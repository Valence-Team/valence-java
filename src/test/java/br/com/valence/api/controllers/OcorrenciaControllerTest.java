package br.com.valence.api.controllers;

import br.com.valence.api.IntegracaoTesteBase;
import br.com.valence.api.dtos.eventos.OcorrenciaTriadaEventoDto;
import br.com.valence.api.enums.StatusOcorrencia;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OcorrenciaControllerTest extends IntegracaoTesteBase {

    @Test
    void idempotencyKeyEvitaOcorrenciaDuplicada() throws Exception {
        String token = novoProdutor();
        long propriedade = criarPropriedade(token, -22.80, -47.10, "MILHO");
        String chave = UUID.randomUUID().toString();
        String corpo = json(Map.of("propriedadeId", propriedade, "cultura", "MILHO", "descricao", "Folhas amareladas."));

        MvcResult primeira = mockMvc.perform(post("/api/ocorrencias").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", chave).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEBIDA"))
                .andReturn();
        long id = lerJson(primeira).get("id").asLong();

        mockMvc.perform(post("/api/ocorrencias").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", chave).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.id").value(id));

        String outroCorpo = json(Map.of("propriedadeId", propriedade, "cultura", "MILHO", "descricao", "Outro texto."));
        mockMvc.perform(post("/api/ocorrencias").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", chave).contentType(MediaType.APPLICATION_JSON).content(outroCorpo))
                .andExpect(status().isConflict());
    }

    @Test
    void idempotencyKeyForaDoFormatoDevolve400() throws Exception {
        String token = novoProdutor();
        long propriedade = criarPropriedade(token, -22.81, -47.11, "MILHO");
        mockMvc.perform(post("/api/ocorrencias").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", "nao-e-uuid").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("propriedadeId", propriedade, "cultura", "MILHO", "descricao", "Teste"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("Idempotency-Key"));
    }

    @Test
    void outroProdutorRecebe403EAgronomoConsegueLer() throws Exception {
        String token = novoProdutor();
        long id = criarOcorrencia(token, criarPropriedade(token, -22.82, -47.12, "MILHO"), "MILHO");
        mockMvc.perform(get("/api/ocorrencias/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(novoProdutor())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/ocorrencias/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(tokenAgronomo())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propriedade.nome").value("Sítio Teste"));
    }

    @Test
    void editaEExcluiEnquantoRecebida() throws Exception {
        String token = novoProdutor();
        long propriedade = criarPropriedade(token, -22.83, -47.13, "MILHO", "TOMATE");
        long id = criarOcorrencia(token, propriedade, "MILHO");
        mockMvc.perform(put("/api/ocorrencias/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("propriedadeId", propriedade, "cultura", "TOMATE",
                                "descricao", "Manchas escuras nas folhas do tomate."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cultura").value("TOMATE"));
        mockMvc.perform(delete("/api/ocorrencias/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/ocorrencias/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void naoEditaOcorrenciaComLaudoFinal() throws Exception {
        String token = novoProdutor();
        long propriedade = criarPropriedade(token, -22.84, -47.14, "MILHO");
        long id = criarOcorrencia(token, propriedade, "MILHO");
        alterarStatus(id, StatusOcorrencia.CONFIRMADA);
        mockMvc.perform(put("/api/ocorrencias/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("propriedadeId", propriedade, "cultura", "MILHO", "descricao", "Novo"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("Regra de negócio"));
    }

    @Test
    void listaFiltraPorStatus() throws Exception {
        String token = novoProdutor();
        long propriedade = criarPropriedade(token, -22.85, -47.15, "MILHO");
        long triada = criarOcorrencia(token, propriedade, "MILHO");
        criarOcorrencia(token, propriedade, "MILHO");
        alterarStatus(triada, StatusOcorrencia.TRIADA);
        mockMvc.perform(get("/api/ocorrencias?status=TRIADA&page=0&size=10&sort=dataRegistro,desc")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(triada));
    }

    @Test
    void envioDeFotoRodaTriagemPublicaEventoEDevolveAFoto() throws Exception {
        visaoReconhece("Ferrugem-comum", 0.85);
        ragResponde();
        responderLlm(TRIAGEM_VALIDA);
        String token = novoProdutor();
        long id = criarOcorrencia(token, criarPropriedade(token, -22.86, -47.16, "MILHO"), "MILHO");

        MvcResult resposta = mockMvc.perform(multipart("/api/ocorrencias/{id}/fotos", id)
                        .file(new MockMultipartFile("foto", "folha.png", "image/png", PNG))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TRIADA"))
                .andExpect(jsonPath("$.pragaProvavel").value("Ferrugem-comum"))
                .andExpect(jsonPath("$.severidade").value(3))
                .andExpect(jsonPath("$.triagem.confianca").value(0.85))
                .andExpect(jsonPath("$.triagem.cuidadosImediatos.length()").value(2))
                .andExpect(jsonPath("$.triagem.explicacao").value("Explicação de teste com base na ficha."))
                .andExpect(jsonPath("$.triagem.fontes[0].fonte").value("Embrapa"))
                .andExpect(jsonPath("$.fotos[0].contentType").value("image/png"))
                .andReturn();

        verify(rabbitTemplate).convertAndSend(eq("valence.eventos"), eq("ocorrencia.triada"),
                any(OcorrenciaTriadaEventoDto.class), any(MessagePostProcessor.class));

        String url = lerJson(resposta).get("fotos").get(0).get("url").asText();
        MvcResult foto = mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn();
        assertThat(foto.getResponse().getContentAsByteArray()).isEqualTo(PNG);
    }

    @Test
    void arquivoQueNaoEImagemDevolve415() throws Exception {
        String token = novoProdutor();
        long id = criarOcorrencia(token, criarPropriedade(token, -22.87, -47.17, "MILHO"), "MILHO");
        MvcResult resultado = enviarFoto(token, id, "isto não é uma imagem".getBytes(StandardCharsets.UTF_8));
        assertThat(resultado.getResponse().getStatus()).isEqualTo(415);
    }

    @Test
    void fotoDeOutroProdutorDevolve403() throws Exception {
        String token = novoProdutor();
        long id = criarOcorrencia(token, criarPropriedade(token, -22.88, -47.18, "MILHO"), "MILHO");
        MvcResult envio = enviarFoto(token, id, PNG);
        assertThat(envio.getResponse().getStatus()).isEqualTo(201);
        String url = lerJson(envio).get("fotos").get(0).get("url").asText();
        mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer(novoProdutor())))
                .andExpect(status().isForbidden());
    }
}
