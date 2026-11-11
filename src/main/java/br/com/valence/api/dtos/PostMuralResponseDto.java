package br.com.valence.api.dtos;

import br.com.valence.api.enums.StatusPost;
import br.com.valence.api.models.PostMuralModel;

import java.time.Instant;

public record PostMuralResponseDto(
        Long id,
        String municipio,
        String uf,
        String texto,
        StatusPost status,
        Long ocorrenciaId,
        Instant criadoEm,
        AutorDto autor,
        long totalComentarios) {

    public static PostMuralResponseDto de(PostMuralModel post, long totalComentarios) {
        return new PostMuralResponseDto(post.getId(), post.getMunicipio(), post.getUf(), post.getTexto(),
                post.getStatus(), post.getOcorrenciaId(), post.getCriadoEm(), AutorDto.de(post.getAutor()),
                totalComentarios);
    }
}
