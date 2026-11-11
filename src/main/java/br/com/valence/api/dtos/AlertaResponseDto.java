package br.com.valence.api.dtos;

import br.com.valence.api.enums.Cultura;
import br.com.valence.api.enums.NivelAlerta;

import java.time.Instant;

public record AlertaResponseDto(
        Long id,
        NivelAlerta nivel,
        Double distanciaKm,
        boolean lido,
        Instant criadoEm,
        Long ocorrenciaId,
        Cultura cultura,
        String pragaProvavel,
        String textoAlerta,
        Integer severidade,
        String propriedadeNome) {
}
