package br.com.valence.api.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversorJson {

    private final ObjectMapper objectMapper;

    public String paraJson(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor == null ? List.of() : valor);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Não foi possível converter o valor para JSON.", ex);
        }
    }

    public <T> List<T> paraLista(String json, TypeReference<List<T>> tipo) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, tipo);
        } catch (JsonProcessingException ex) {
            log.warn("JSON armazenado em formato inesperado; devolvendo lista vazia.");
            return List.of();
        }
    }
}
