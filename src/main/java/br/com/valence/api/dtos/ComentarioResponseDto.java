package br.com.valence.api.dtos;

import br.com.valence.api.models.ComentarioModel;

import java.time.Instant;

public record ComentarioResponseDto(Long id, String texto, Instant criadoEm, AutorDto autor) {

    public static ComentarioResponseDto de(ComentarioModel comentario) {
        return new ComentarioResponseDto(comentario.getId(), comentario.getTexto(), comentario.getCriadoEm(),
                AutorDto.de(comentario.getAutor()));
    }
}
