package br.com.valence.api.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErroResponseDto(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoErroDto> campos) {

    public static ErroResponseDto de(int status, String erro, String mensagem, String caminho) {
        return new ErroResponseDto(Instant.now(), status, erro, mensagem, caminho, List.of());
    }

    public static ErroResponseDto de(int status, String erro, String mensagem, String caminho,
                                     List<CampoErroDto> campos) {
        return new ErroResponseDto(Instant.now(), status, erro, mensagem, caminho, campos);
    }
}
