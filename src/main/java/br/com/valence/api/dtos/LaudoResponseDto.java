package br.com.valence.api.dtos;

import br.com.valence.api.enums.DecisaoLaudo;
import br.com.valence.api.models.LaudoModel;

import java.time.Instant;

public record LaudoResponseDto(
        Long id,
        DecisaoLaudo decisao,
        String pragaConfirmada,
        String parecer,
        Instant emitidoEm,
        String agronomoNome) {

    public static LaudoResponseDto de(LaudoModel laudo) {
        return new LaudoResponseDto(laudo.getId(), laudo.getDecisao(), laudo.getPragaConfirmada(), laudo.getParecer(),
                laudo.getEmitidoEm(), laudo.getAgronomo().getNome());
    }
}
