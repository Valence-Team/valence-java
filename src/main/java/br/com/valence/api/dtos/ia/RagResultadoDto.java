package br.com.valence.api.dtos.ia;

import java.util.List;

public record RagResultadoDto(List<TrechoRagDto> trechos, String explicacao, String modeloLlm) {

    public List<TrechoRagDto> trechosOuVazio() {
        return trechos == null ? List.of() : trechos;
    }
}
