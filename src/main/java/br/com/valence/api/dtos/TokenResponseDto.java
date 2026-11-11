package br.com.valence.api.dtos;

import java.time.Instant;

public record TokenResponseDto(
        String token,
        String tipo,
        Instant expiraEm,
        UsuarioResponseDto usuario) {
}
