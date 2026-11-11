package br.com.valence.api.dtos;

import br.com.valence.api.models.PropriedadeModel;

public record PropriedadeResumoDto(Long id, String nome, String municipio, String uf) {

    public static PropriedadeResumoDto de(PropriedadeModel propriedade) {
        return new PropriedadeResumoDto(propriedade.getId(), propriedade.getNome(), propriedade.getMunicipio(),
                propriedade.getUf());
    }
}
