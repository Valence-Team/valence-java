package br.com.valence.api.services;

import br.com.valence.api.dtos.DenunciaDto;
import br.com.valence.api.dtos.DenunciaResponseDto;
import br.com.valence.api.dtos.ia.AnaliseDenunciaIaDto;
import br.com.valence.api.enums.AcaoModeracao;
import br.com.valence.api.enums.GravidadeDenuncia;
import br.com.valence.api.enums.StatusPost;
import br.com.valence.api.exceptions.ConflitoException;
import br.com.valence.api.exceptions.RegraNegocioException;
import br.com.valence.api.models.DenunciaModel;
import br.com.valence.api.models.PostMuralModel;
import br.com.valence.api.models.UsuarioModel;
import br.com.valence.api.repositories.DenunciaRepository;
import br.com.valence.api.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DenunciaService {

    private final DenunciaRepository denunciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MuralService muralService;
    private final DenunciaIaService denunciaIaService;
    private final TransactionTemplate transactionTemplate;

    public DenunciaResponseDto denunciar(Long postId, DenunciaDto dto, UsuarioModel usuario) {
        DadosDenuncia dados;
        try {
            dados = transactionTemplate.execute(status -> registrar(postId, dto, usuario));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitoException("Você já denunciou esta publicação.");
        }
        Optional<AnaliseDenunciaIaDto> analise = denunciaIaService.analisar(dados.textoPost(), dto.getMotivo());
        return transactionTemplate.execute(status -> aplicarAnalise(dados.denunciaId(), analise));
    }

    private DadosDenuncia registrar(Long postId, DenunciaDto dto, UsuarioModel usuario) {
        muralService.carregar(postId);
        if (denunciaRepository.existsByPostIdAndUsuarioId(postId, usuario.getId())) {
            throw new ConflitoException("Você já denunciou esta publicação.");
        }
        PostMuralModel post = muralService.carregarVisivel(postId, usuario);
        if (Objects.equals(post.getAutor().getId(), usuario.getId())) {
            throw new RegraNegocioException("Você não pode denunciar a sua própria publicação.");
        }
        DenunciaModel denuncia = new DenunciaModel();
        denuncia.setMotivo(dto.getMotivo().trim());
        denuncia.setCriadaEm(Instant.now());
        denuncia.setPost(post);
        denuncia.setUsuario(usuarioRepository.getReferenceById(usuario.getId()));
        DenunciaModel salva = denunciaRepository.saveAndFlush(denuncia);
        return new DadosDenuncia(salva.getId(), post.getTexto());
    }

    private DenunciaResponseDto aplicarAnalise(Long denunciaId, Optional<AnaliseDenunciaIaDto> analise) {
        DenunciaModel denuncia = denunciaRepository.findById(denunciaId).orElseThrow();
        analise.ifPresent(resultado -> {
            denuncia.setCategoriaIa(resultado.categoria());
            denuncia.setGravidadeIa(resultado.gravidade());
            denuncia.setAcaoIa(resultado.acao());
            denuncia.setJustificativaIa(resultado.justificativa());
            PostMuralModel post = denuncia.getPost();
            if (resultado.gravidade() == GravidadeDenuncia.ALTA
                    && resultado.acao() == AcaoModeracao.OCULTAR_E_REVISAR
                    && post.getStatus() == StatusPost.ATIVO) {
                post.setStatus(StatusPost.OCULTO);
                log.info("Publicação {} ocultada de forma provisória para moderação humana.", post.getId());
            }
        });
        return DenunciaResponseDto.de(denuncia);
    }

    private record DadosDenuncia(Long denunciaId, String textoPost) {
    }
}
