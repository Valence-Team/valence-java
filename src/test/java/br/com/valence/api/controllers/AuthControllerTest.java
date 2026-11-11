package br.com.valence.api.controllers;

import br.com.valence.api.IntegracaoTesteBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends IntegracaoTesteBase {

    @Test
    void registrarDevolveTokenComPapelProdutor() throws Exception {
        mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(registro("maria-" + UUID.randomUUID() + "@teste.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEm").isNotEmpty())
                .andExpect(jsonPath("$.usuario.papel").value("PRODUTOR"))
                .andExpect(jsonPath("$.usuario.uf").value("SP"));
    }

    @Test
    void registrarComEmailRepetidoDevolve409() throws Exception {
        String email = "repetido-" + UUID.randomUUID() + "@teste.com";
        mockMvc.perform(post("/api/auth/registrar").contentType(MediaType.APPLICATION_JSON)
                        .content(json(registro(email))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/registrar").contentType(MediaType.APPLICATION_JSON)
                        .content(json(registro(email.toUpperCase()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.erro").value("Conflito"))
                .andExpect(jsonPath("$.caminho").value("/api/auth/registrar"));
    }

    @Test
    void registrarComDadosInvalidosDevolve400ComCampos() throws Exception {
        Map<String, Object> corpo = Map.of("nome", "", "email", "invalido", "senha", "123",
                "municipio", "Campinas", "uf", "ZZ");
        mockMvc.perform(post("/api/auth/registrar").contentType(MediaType.APPLICATION_JSON).content(json(corpo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Requisição inválida"))
                .andExpect(jsonPath("$.mensagem").value("Um ou mais campos são inválidos."))
                .andExpect(jsonPath("$.campos[*].campo", hasItems("nome", "email", "senha", "uf")));
    }

    @Test
    void loginComSenhaErradaDevolve401ComMensagemUnica() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "agronomo@valence.local", "senha", "senha-errada"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos."));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "ninguem@valence.local", "senha", "qualquer"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos."));
    }

    @Test
    void loginDoAgronomoDeTesteDevolvePapelAgronomo() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "agronomo@valence.local", "senha", "Agronomo@2026"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.papel").value("AGRONOMO"));
    }

    @Test
    void perfilDoUsuarioLogadoPodeSerLidoEAtualizado() throws Exception {
        String token = novoProdutor();
        mockMvc.perform(get("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.municipio").value("Campinas"));
        Map<String, Object> perfil = Map.of("nome", "Produtor Atualizado", "telefone", "(19) 3333-4444",
                "municipio", "Jundiaí", "uf", "sp");
        mockMvc.perform(put("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(perfil)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Produtor Atualizado"))
                .andExpect(jsonPath("$.telefone").value("(19) 3333-4444"))
                .andExpect(jsonPath("$.uf").value("SP"));
    }

    private Map<String, Object> registro(String email) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("nome", "Maria da Silva");
        corpo.put("email", email);
        corpo.put("senha", "Senha@1234");
        corpo.put("telefone", "(16) 99999-0000");
        corpo.put("municipio", "Ribeirão Preto");
        corpo.put("uf", "sp");
        return corpo;
    }
}
