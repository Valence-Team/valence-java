package br.com.valence.api.dtos;

import br.com.valence.api.enums.CategoriaDenuncia;
import br.com.valence.api.enums.GravidadeDenuncia;
import br.com.valence.api.enums.StatusPost;

import java.time.Instant;

public record ModeracaoItemDto(
        Long postId,
        String texto,
        String municipio,
        String uf,
        StatusPost status,
        Instant criadoEm,
        String autorNome,
        long totalDenuncias,
        CategoriaDenuncia categoriaIa,
        GravidadeDenuncia gravidadeIa,
        String justificativaIa) {
}
