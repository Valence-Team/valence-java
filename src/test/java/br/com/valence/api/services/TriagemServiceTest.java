package br.com.valence.api.services;

import br.com.valence.api.IntegracaoTesteBase;
import br.com.valence.api.exceptions.IaIndisponivelException;
import br.com.valence.api.dtos.ia.VisaoResultadoDto;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TriagemServiceTest extends IntegracaoTesteBase {

    @Test
    void fotoRecusadaVaiParaOAgronomoSemChamarOModelo() throws Exception {
        when(iaServiceClient.classificar(any(), any(), any(), any()))
                .thenReturn(new VisaoResultadoDto(false, "IMAGEM_BORRADA", null, List.of()));
        String token = novoProdutor();
        long id = criarOcorrencia(token, criarPropriedade(token, -10.10, -48.10, "MILHO"), "MILHO");

        MvcResult resultado = enviarFoto(token, id, PNG);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        assertThat(lerJson(resultado).get("status").asText()).isEqualTo("EM_ANALISE");
        assertThat(lerJson(resultado).get("triagem").get("motivoImagem").asText()).isEqualTo("IMAGEM_BORRADA");
        assertThat(lerJson(resultado).get("triagem").get("encaminharAgronomo").asBoolean()).isTrue();
        verify(chatModel, never()).call(any(Prompt.class));
        verificarQueNaoPublicouTriagem();
    }

    @Test
    void servicoDeIaForaDoArUsaTriagemSoComOTexto() throws Exception {
        when(iaServiceClient.classificar(any(), any(), any(), any()))
                .thenThrow(new IaIndisponivelException("Serviço fora do ar.", null));
        responderLlm(TRIAGEM_VALIDA);
        String token = novoProdutor();
        long id = criarOcorrencia(token, criarPropriedade(token, -10.11, -48.11, "MILHO"), "MILHO");

        MvcResult resultado = enviarFoto(token, id, PNG);

        assertThat(lerJson(resultado).get("status").asText()).isEqualTo("TRIADA");
        assertThat(lerJson(resultado).get("triagem").get("confianca").asDouble()).isEqualTo(0.82);
        verify(iaServiceClient, never()).consultarRag(any(), any(), any());
        verify(rabbitTemplate).convertAndSend(eq("valence.eventos"), eq("ocorrencia.triada"), any(Object.class),
                any(MessagePostProcessor.class));
    }

    @Test
    void modeloForaDoArEncaminhaAoAgronomoSemAlerta() throws Exception {
        visaoReconhece("Ferrugem-comum", 0.9);
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("HTTP 429 - limite atingido"));
        String token = novoProdutor();
        long id = criarOcorrencia(token, criarPropriedade(token, -10.12, -48.12, "MILHO"), "MILHO");

        MvcResult resultado = enviarFoto(token, id, PNG);

        assertThat(lerJson(resultado).get("status").asText()).isEqualTo("EM_ANALISE");
        assertThat(lerJson(resultado).get("triagem").get("encaminharAgronomo").asBoolean()).isTrue();
        assertThat(lerJson(resultado).get("triagem").get("hipoteses").get(0).get("praga").asText())
                .isEqualTo("Ferrugem-comum");
        verificarQueNaoPublicouTriagem();
    }

    @Test
    void respostaForaDoFormatoNaoGeraAlerta() throws Exception {
        visaoReconhece("Ferrugem-comum", 0.9);
        responderLlm("Não consigo responder em JSON agora.");
        String token = novoProdutor();
        long id = criarOcorrencia(token, criarPropriedade(token, -10.13, -48.13, "MILHO"), "MILHO");

        MvcResult resultado = enviarFoto(token, id, PNG);

        assertThat(lerJson(resultado).get("status").asText()).isEqualTo("EM_ANALISE");
        verificarQueNaoPublicouTriagem();
    }

    @Test
    void confiancaBaixaDaVisaoNaoGeraAlerta() throws Exception {
        visaoReconhece("Ferrugem-comum", 0.3);
        responderLlm(TRIAGEM_VALIDA);
        String token = novoProdutor();
        long id = criarOcorrencia(token, criarPropriedade(token, -10.14, -48.14, "MILHO"), "MILHO");

        MvcResult resultado = enviarFoto(token, id, PNG);

        assertThat(lerJson(resultado).get("status").asText()).isEqualTo("EM_ANALISE");
        assertThat(lerJson(resultado).get("triagem").get("encaminharAgronomo").asBoolean()).isTrue();
        verificarQueNaoPublicouTriagem();
    }

    @Test
    void ocorrenciaSemFotoPodeSerTriadaPeloTexto() throws Exception {
        responderLlm(TRIAGEM_VALIDA);
        String token = novoProdutor();
        long id = criarOcorrencia(token, criarPropriedade(token, -10.15, -48.15, "MILHO"), "MILHO");

        mockMvc.perform(post("/api/ocorrencias/{id}/triagem", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TRIADA"))
                .andExpect(jsonPath("$.triagem.textoAlerta").isNotEmpty());
    }

    private void verificarQueNaoPublicouTriagem() {
        verify(rabbitTemplate, never()).convertAndSend(anyString(), eq("ocorrencia.triada"), any(Object.class),
                any(MessagePostProcessor.class));
    }
}
