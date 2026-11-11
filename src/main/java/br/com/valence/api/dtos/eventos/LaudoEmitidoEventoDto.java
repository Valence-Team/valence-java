package br.com.valence.api.dtos.eventos;

import br.com.valence.api.enums.Cultura;
import br.com.valence.api.enums.DecisaoLaudo;

import java.time.Instant;
import java.util.UUID;

public record LaudoEmitidoEventoDto(
        UUID eventoId,
        String tipo,
        int versao,
        Instant emitidoEm,
        Long ocorrenciaId,
        DecisaoLaudo decisao,
        String pragaConfirmada,
        Cultura cultura,
        String municipio,
        String uf) {

    public static final String TIPO = "laudo.emitido";
}
