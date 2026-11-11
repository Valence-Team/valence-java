package br.com.valence.api.dtos;

import br.com.valence.api.enums.Papel;
import br.com.valence.api.models.UsuarioModel;

public record UsuarioResponseDto(
        Long id,
        String nome,
        String email,
        Papel papel,
        String municipio,
        String uf,
        String telefone) {

    public static UsuarioResponseDto de(UsuarioModel usuario) {
        return new UsuarioResponseDto(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPapel(),
                usuario.getMunicipio(), usuario.getUf(), usuario.getTelefone());
    }
}
