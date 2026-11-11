package br.com.valence.api.dtos;

import java.time.Instant;
import java.util.List;

public record TriagemResponseDto(
        List<HipoteseDto> hipoteses,
        Integer severidade,
        List<String> cuidadosImediatos,
        String textoAlerta,
        boolean encaminharAgronomo,
        Double confianca,
        Instant geradaEm,
        String explicacao,
        List<FonteDto> fontes,
        String motivoImagem) {
}
