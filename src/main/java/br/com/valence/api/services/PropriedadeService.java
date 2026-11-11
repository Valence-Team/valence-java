package br.com.valence.api.services;

import br.com.valence.api.dtos.PaginaDto;
import br.com.valence.api.dtos.PropriedadeDto;
import br.com.valence.api.dtos.PropriedadeResponseDto;
import br.com.valence.api.exceptions.ConflitoException;
import br.com.valence.api.exceptions.RecursoNaoEncontradoException;
import br.com.valence.api.models.PropriedadeModel;
import br.com.valence.api.models.UsuarioModel;
import br.com.valence.api.repositories.AlertaRepository;
import br.com.valence.api.repositories.OcorrenciaRepository;
import br.com.valence.api.repositories.PropriedadeRepository;
import br.com.valence.api.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PropriedadeService {

    private final PropriedadeRepository propriedadeRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final AlertaRepository alertaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public PaginaDto<PropriedadeResponseDto> listar(UsuarioModel usuario, Pageable pageable) {
        return PaginaDto.de(propriedadeRepository.findByUsuarioId(usuario.getId(), pageable)
                .map(PropriedadeResponseDto::de));
    }

    @Transactional(readOnly = true)
    public PropriedadeResponseDto buscar(Long id, UsuarioModel usuario) {
        return PropriedadeResponseDto.de(carregarDoUsuario(id, usuario));
    }

    @Transactional
    public PropriedadeResponseDto criar(PropriedadeDto dto, UsuarioModel usuario) {
        PropriedadeModel propriedade = new PropriedadeModel();
        copiarDados(dto, propriedade);
        propriedade.setUsuario(usuarioRepository.getReferenceById(usuario.getId()));
        propriedade.setCriadoEm(Instant.now());
        return PropriedadeResponseDto.de(propriedadeRepository.save(propriedade));
    }

    @Transactional
    public PropriedadeResponseDto atualizar(Long id, PropriedadeDto dto, UsuarioModel usuario) {
        PropriedadeModel propriedade = carregarDoUsuario(id, usuario);
        copiarDados(dto, propriedade);
        return PropriedadeResponseDto.de(propriedade);
    }

    @Transactional
    public void excluir(Long id, UsuarioModel usuario) {
        PropriedadeModel propriedade = carregarDoUsuario(id, usuario);
        if (ocorrenciaRepository.existsByPropriedadeId(propriedade.getId())) {
            throw new ConflitoException("A propriedade tem ocorrências registradas e não pode ser removida.");
        }
        alertaRepository.excluirDaPropriedade(propriedade.getId());
        propriedadeRepository.delete(propriedade);
    }

    public PropriedadeModel carregarDoUsuario(Long id, UsuarioModel usuario) {
        return propriedadeRepository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Propriedade não encontrada."));
    }

    private void copiarDados(PropriedadeDto dto, PropriedadeModel propriedade) {
        BeanUtils.copyProperties(dto, propriedade, "culturas");
        propriedade.setNome(dto.getNome().trim());
        propriedade.setMunicipio(dto.getMunicipio().trim());
        propriedade.setUf(dto.getUf().trim().toUpperCase(Locale.ROOT));
        propriedade.getCulturas().clear();
        propriedade.getCulturas().addAll(new HashSet<>(dto.getCulturas()));
    }
}
