package br.com.valence.api.dtos;

import br.com.valence.api.enums.Papel;
import br.com.valence.api.models.UsuarioModel;

public record AutorDto(Long id, String nome, Papel papel) {

    public static AutorDto de(UsuarioModel usuario) {
        return new AutorDto(usuario.getId(), usuario.getNome(), usuario.getPapel());
    }
}
