package br.com.valence.api.services;

import br.com.valence.api.dtos.ModeracaoDto;
import br.com.valence.api.dtos.ModeracaoItemDto;
import br.com.valence.api.dtos.PaginaDto;
import br.com.valence.api.enums.AcaoModeracao;
import br.com.valence.api.enums.StatusPost;
import br.com.valence.api.exceptions.RecursoNaoEncontradoException;
import br.com.valence.api.exceptions.ValidacaoException;
import br.com.valence.api.models.DenunciaModel;
import br.com.valence.api.models.PostMuralModel;
import br.com.valence.api.repositories.DenunciaRepository;
import br.com.valence.api.repositories.PostMuralRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ModeracaoService {

    private static final List<StatusPost> STATUS_EM_MODERACAO = List.of(StatusPost.ATIVO, StatusPost.OCULTO);

    private final PostMuralRepository postMuralRepository;
    private final DenunciaRepository denunciaRepository;

    @Transactional(readOnly = true)
    public PaginaDto<ModeracaoItemDto> listarFila(Pageable pageable) {
        Pageable semOrdenacao = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return PaginaDto.de(postMuralRepository.buscarFilaModeracao(STATUS_EM_MODERACAO, semOrdenacao)
                .map(this::paraItem));
    }

    @Transactional
    public ModeracaoItemDto decidir(Long postId, ModeracaoDto dto) {
        if (dto.getDecisao() != AcaoModeracao.MANTER && dto.getDecisao() != AcaoModeracao.REMOVER) {
            throw new ValidacaoException("decisao", "Use MANTER ou REMOVER.");
        }
        PostMuralModel post = postMuralRepository.findById(postId)
                .filter(encontrado -> encontrado.getStatus() != StatusPost.REMOVIDO)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Publicação não encontrada."));
        ModeracaoItemDto itemAntes = paraItem(post);
        Instant agora = Instant.now();
        denunciaRepository.findByPostIdAndDecisaoFinalIsNull(postId).forEach(denuncia -> {
            denuncia.setDecisaoFinal(dto.getDecisao());
            denuncia.setDecididaEm(agora);
        });
        post.setStatus(dto.getDecisao() == AcaoModeracao.REMOVER ? StatusPost.REMOVIDO : StatusPost.ATIVO);
        return new ModeracaoItemDto(itemAntes.postId(), itemAntes.texto(), itemAntes.municipio(), itemAntes.uf(),
                post.getStatus(), itemAntes.criadoEm(), itemAntes.autorNome(), itemAntes.totalDenuncias(),
                itemAntes.categoriaIa(), itemAntes.gravidadeIa(), itemAntes.justificativaIa());
    }

    private ModeracaoItemDto paraItem(PostMuralModel post) {
        List<DenunciaModel> pendentes = denunciaRepository.findByPostIdAndDecisaoFinalIsNull(post.getId());
        Optional<DenunciaModel> maisGrave = pendentes.stream()
                .filter(denuncia -> denuncia.getGravidadeIa() != null)
                .max(Comparator.comparing(DenunciaModel::getGravidadeIa));
        return new ModeracaoItemDto(post.getId(), post.getTexto(), post.getMunicipio(), post.getUf(),
                post.getStatus(), post.getCriadoEm(), post.getAutor().getNome(), pendentes.size(),
                maisGrave.map(DenunciaModel::getCategoriaIa).orElse(null),
                maisGrave.map(DenunciaModel::getGravidadeIa).orElse(null),
                maisGrave.map(DenunciaModel::getJustificativaIa).orElse(null));
    }
}
