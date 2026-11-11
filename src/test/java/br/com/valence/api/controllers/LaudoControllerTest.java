package br.com.valence.api.controllers;

import br.com.valence.api.IntegracaoTesteBase;
import br.com.valence.api.consumers.OcorrenciaTriadaConsumer;
import br.com.valence.api.dtos.eventos.LaudoEmitidoEventoDto;
import br.com.valence.api.enums.StatusOcorrencia;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LaudoControllerTest extends IntegracaoTesteBase {

    @Autowired
    private OcorrenciaTriadaConsumer consumer;

    @Test
    void confirmarCasoAtualizaAlertasPublicaNoMuralEEmiteEvento() throws Exception {
        String produtor = novoProdutor("Londrina", "PR");
        long propriedade = criarPropriedade(produtor, "Londrina", "PR", -23.3000, -51.1600, "MILHO");
        String vizinho = novoProdutor("Londrina", "PR");
        criarPropriedade(vizinho, "Londrina", "PR", -23.3300, -51.1600, "MILHO");
        long ocorrencia = criarOcorrencia(produtor, propriedade, "MILHO");
        alterarStatus(ocorrencia, StatusOcorrencia.TRIADA);
        consumer.receber(eventoTriada(ocorrencia));

        mockMvc.perform(post("/api/ocorrencias/{id}/laudo", ocorrencia)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAgronomo()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("decisao", "CONFIRMADA", "pragaConfirmada", "Cigarrinha-do-milho",
                                "parecer", "Sintomas compatíveis com enfezamento."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decisao").value("CONFIRMADA"))
                .andExpect(jsonPath("$.agronomoNome").value("Carla Agrônoma"));

        mockMvc.perform(get("/api/ocorrencias/{id}", ocorrencia).header(HttpHeaders.AUTHORIZATION, bearer(produtor)))
                .andExpect(jsonPath("$.status").value("CONFIRMADA"))
                .andExpect(jsonPath("$.laudo.pragaConfirmada").value("Cigarrinha-do-milho"));
        mockMvc.perform(get("/api/alertas").header(HttpHeaders.AUTHORIZATION, bearer(vizinho)))
                .andExpect(jsonPath("$.content[0].nivel").value("CONFIRMADO"))
                .andExpect(jsonPath("$.content[0].pragaProvavel").value("Cigarrinha-do-milho"));
        mockMvc.perform(get("/api/mural?municipio=Londrina&uf=PR&size=50").header(HttpHeaders.AUTHORIZATION, bearer(vizinho)))
                .andExpect(jsonPath("$.content[*].ocorrenciaId", hasItem((int) ocorrencia)));
        verify(rabbitTemplate).convertAndSend(eq("valence.eventos"), eq("laudo.emitido"),
                any(LaudoEmitidoEventoDto.class), any(MessagePostProcessor.class));
    }

    @Test
    void confirmarSemPragaDevolve400() throws Exception {
        String produtor = novoProdutor();
        long ocorrencia = criarOcorrencia(produtor, criarPropriedade(produtor, -23.31, -51.17, "MILHO"), "MILHO");
        mockMvc.perform(post("/api/ocorrencias/{id}/laudo", ocorrencia)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAgronomo()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("decisao", "CONFIRMADA", "parecer", "Sem praga informada."))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("pragaConfirmada"));
    }

    @Test
    void maisInformacoesMantemEmAnaliseEDescarteEncerraAlertas() throws Exception {
        String produtor = novoProdutor("Maringá", "PR");
        long propriedade = criarPropriedade(produtor, "Maringá", "PR", -23.4200, -51.9300, "MILHO");
        String vizinho = novoProdutor("Maringá", "PR");
        criarPropriedade(vizinho, "Maringá", "PR", -23.4400, -51.9300, "MILHO");
        long ocorrencia = criarOcorrencia(produtor, propriedade, "MILHO");
        alterarStatus(ocorrencia, StatusOcorrencia.TRIADA);
        consumer.receber(eventoTriada(ocorrencia));
        String agronomo = tokenAgronomo();

        mockMvc.perform(post("/api/ocorrencias/{id}/laudo", ocorrencia).header(HttpHeaders.AUTHORIZATION, bearer(agronomo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("decisao", "MAIS_INFORMACOES", "parecer", "Envie foto da face de baixo."))))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/ocorrencias/{id}", ocorrencia).header(HttpHeaders.AUTHORIZATION, bearer(produtor)))
                .andExpect(jsonPath("$.status").value("EM_ANALISE"));

        mockMvc.perform(post("/api/ocorrencias/{id}/laudo", ocorrencia).header(HttpHeaders.AUTHORIZATION, bearer(agronomo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("decisao", "DESCARTADA", "parecer", "Dano causado por granizo."))))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/alertas").header(HttpHeaders.AUTHORIZATION, bearer(vizinho)))
                .andExpect(jsonPath("$.content[0].nivel").value("ENCERRADO"));

        mockMvc.perform(post("/api/ocorrencias/{id}/laudo", ocorrencia).header(HttpHeaders.AUTHORIZATION, bearer(agronomo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("decisao", "DESCARTADA", "parecer", "Repetido."))))
                .andExpect(status().isUnprocessableEntity());
    }
}
