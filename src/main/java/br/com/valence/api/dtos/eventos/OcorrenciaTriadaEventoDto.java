package br.com.valence.api.dtos.eventos;

import br.com.valence.api.enums.Cultura;

import java.time.Instant;
import java.util.UUID;

public record OcorrenciaTriadaEventoDto(
        UUID eventoId,
        String tipo,
        int versao,
        Instant emitidoEm,
        Long ocorrenciaId,
        Cultura cultura,
        String pragaProvavel,
        Integer severidade,
        Double confianca,
        String municipio,
        String uf,
        Instant dataOcorrencia) {

    public static final String TIPO = "ocorrencia.triada";
}
