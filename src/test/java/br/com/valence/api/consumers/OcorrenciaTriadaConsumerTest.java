package br.com.valence.api.consumers;

import br.com.valence.api.IntegracaoTesteBase;
import br.com.valence.api.dtos.eventos.OcorrenciaTriadaEventoDto;
import br.com.valence.api.enums.StatusOcorrencia;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OcorrenciaTriadaConsumerTest extends IntegracaoTesteBase {

    @Autowired
    private OcorrenciaTriadaConsumer consumer;

    @Test
    void criaAlertaSoParaVizinhoDaMesmaCulturaDentroDoRaio() throws Exception {
        String origem = novoProdutor("Cuiabá", "MT");
        long propriedadeOrigem = criarPropriedade(origem, "Cuiabá", "MT", -15.6000, -56.1000, "MILHO");
        String vizinho = novoProdutor("Cuiabá", "MT");
        criarPropriedade(vizinho, "Cuiabá", "MT", -15.6400, -56.1000, "MILHO");
        String outraCultura = novoProdutor("Cuiabá", "MT");
        criarPropriedade(outraCultura, "Cuiabá", "MT", -15.6100, -56.1100, "TOMATE");
        String distante = novoProdutor("Cuiabá", "MT");
        criarPropriedade(distante, "Cuiabá", "MT", -15.9000, -56.1000, "MILHO");
        long ocorrencia = criarOcorrencia(origem, propriedadeOrigem, "MILHO");
        alterarStatus(ocorrencia, StatusOcorrencia.TRIADA);

        consumer.receber(eventoTriada(ocorrencia));

        mockMvc.perform(get("/api/alertas").header(HttpHeaders.AUTHORIZATION, bearer(vizinho)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].ocorrenciaId").value(ocorrencia))
                .andExpect(jsonPath("$.content[0].nivel").value("SUSPEITA"))
                .andExpect(jsonPath("$.content[0].distanciaKm").value(4.0))
                .andExpect(jsonPath("$.content[0].propriedadeNome").value("Sítio Teste"))
                .andExpect(jsonPath("$.content[0].lido").value(false));
        mockMvc.perform(get("/api/alertas").header(HttpHeaders.AUTHORIZATION, bearer(outraCultura)))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/alertas").header(HttpHeaders.AUTHORIZATION, bearer(distante)))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/alertas").header(HttpHeaders.AUTHORIZATION, bearer(origem)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void eventoRepetidoNaoDuplicaAlertas() throws Exception {
        String origem = novoProdutor("Sinop", "MT");
        long propriedadeOrigem = criarPropriedade(origem, "Sinop", "MT", -11.8600, -55.5000, "MILHO");
        String vizinho = novoProdutor("Sinop", "MT");
        criarPropriedade(vizinho, "Sinop", "MT", -11.8800, -55.5000, "MILHO");
        long ocorrencia = criarOcorrencia(origem, propriedadeOrigem, "MILHO");
        alterarStatus(ocorrencia, StatusOcorrencia.TRIADA);
        OcorrenciaTriadaEventoDto evento = eventoTriada(ocorrencia);

        consumer.receber(evento);
        consumer.receber(evento);

        mockMvc.perform(get("/api/alertas").header(HttpHeaders.AUTHORIZATION, bearer(vizinho)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void produtorMarcaAlertaComoLido() throws Exception {
        String origem = novoProdutor("Sorriso", "MT");
        long propriedadeOrigem = criarPropriedade(origem, "Sorriso", "MT", -12.5400, -55.7100, "MILHO");
        String vizinho = novoProdutor("Sorriso", "MT");
        criarPropriedade(vizinho, "Sorriso", "MT", -12.5600, -55.7100, "MILHO");
        long ocorrencia = criarOcorrencia(origem, propriedadeOrigem, "MILHO");
        alterarStatus(ocorrencia, StatusOcorrencia.TRIADA);
        consumer.receber(eventoTriada(ocorrencia));

        MvcResult lista = mockMvc.perform(get("/api/alertas").header(HttpHeaders.AUTHORIZATION, bearer(vizinho)))
                .andReturn();
        long alerta = lerJson(lista).get("content").get(0).get("id").asLong();

        mockMvc.perform(patch("/api/alertas/{id}/lido", alerta).header(HttpHeaders.AUTHORIZATION, bearer(vizinho)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lido").value(true));
        mockMvc.perform(patch("/api/alertas/{id}/lido", alerta).header(HttpHeaders.AUTHORIZATION, bearer(origem)))
                .andExpect(status().isNotFound());
    }
}
