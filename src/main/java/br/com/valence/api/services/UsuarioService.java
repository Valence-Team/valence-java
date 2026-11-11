package br.com.valence.api.services;

import br.com.valence.api.dtos.PerfilDto;
import br.com.valence.api.dtos.UsuarioResponseDto;
import br.com.valence.api.exceptions.RecursoNaoEncontradoException;
import br.com.valence.api.models.UsuarioModel;
import br.com.valence.api.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public UsuarioResponseDto buscarPerfil(UsuarioModel usuarioLogado) {
        return UsuarioResponseDto.de(carregar(usuarioLogado.getId()));
    }

    @Transactional
    public UsuarioResponseDto atualizarPerfil(UsuarioModel usuarioLogado, PerfilDto dto) {
        UsuarioModel usuario = carregar(usuarioLogado.getId());
        usuario.setNome(dto.getNome().trim());
        usuario.setTelefone(StringUtils.hasText(dto.getTelefone()) ? dto.getTelefone().trim() : null);
        usuario.setMunicipio(dto.getMunicipio().trim());
        usuario.setUf(dto.getUf().trim().toUpperCase(Locale.ROOT));
        return UsuarioResponseDto.de(usuario);
    }

    private UsuarioModel carregar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }
}
