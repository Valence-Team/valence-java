package br.com.valence.api.dtos;

import br.com.valence.api.enums.AcaoModeracao;
import br.com.valence.api.enums.CategoriaDenuncia;
import br.com.valence.api.enums.GravidadeDenuncia;
import br.com.valence.api.enums.StatusPost;
import br.com.valence.api.models.DenunciaModel;

import java.time.Instant;

public record DenunciaResponseDto(
        Long id,
        Long postId,
        String motivo,
        CategoriaDenuncia categoriaIa,
        GravidadeDenuncia gravidadeIa,
        AcaoModeracao acaoIa,
        String justificativaIa,
        Instant criadaEm,
        StatusPost statusPost) {

    public static DenunciaResponseDto de(DenunciaModel denuncia) {
        return new DenunciaResponseDto(denuncia.getId(), denuncia.getPost().getId(), denuncia.getMotivo(),
                denuncia.getCategoriaIa(), denuncia.getGravidadeIa(), denuncia.getAcaoIa(),
                denuncia.getJustificativaIa(), denuncia.getCriadaEm(), denuncia.getPost().getStatus());
    }
}
