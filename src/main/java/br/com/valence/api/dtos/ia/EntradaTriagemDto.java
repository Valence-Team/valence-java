package br.com.valence.api.dtos.ia;

import br.com.valence.api.enums.Cultura;

import java.util.List;

public record EntradaTriagemDto(
        Cultura cultura,
        String descricao,
        List<ClasseVisaoDto> classes,
        List<TrechoRagDto> trechos) {
}
