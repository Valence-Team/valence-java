package br.com.valence.api.dtos;

import br.com.valence.api.enums.Cultura;
import br.com.valence.api.enums.StatusOcorrencia;

import java.time.Instant;
import java.util.List;

public record OcorrenciaResponseDto(
        Long id,
        Cultura cultura,
        String descricao,
        StatusOcorrencia status,
        Integer severidade,
        String pragaProvavel,
        Instant dataRegistro,
        Instant atualizadoEm,
        PropriedadeResumoDto propriedade,
        List<FotoResponseDto> fotos,
        TriagemResponseDto triagem,
        LaudoResponseDto laudo) {
}
