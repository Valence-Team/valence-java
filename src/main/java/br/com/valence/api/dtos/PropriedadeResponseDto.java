package br.com.valence.api.dtos;

import br.com.valence.api.enums.Cultura;
import br.com.valence.api.models.PropriedadeModel;

import java.time.Instant;
import java.util.List;

public record PropriedadeResponseDto(
        Long id,
        String nome,
        String municipio,
        String uf,
        Double latitude,
        Double longitude,
        Double areaHectares,
        List<Cultura> culturas,
        Instant criadoEm) {

    public static PropriedadeResponseDto de(PropriedadeModel propriedade) {
        List<Cultura> culturas = propriedade.getCulturas().stream().sorted().toList();
        return new PropriedadeResponseDto(propriedade.getId(), propriedade.getNome(), propriedade.getMunicipio(),
                propriedade.getUf(), propriedade.getLatitude(), propriedade.getLongitude(),
                propriedade.getAreaHectares(), culturas, propriedade.getCriadoEm());
    }
}
