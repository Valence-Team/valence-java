package br.com.valence.api.services;

import br.com.valence.api.dtos.ComentarioDto;
import br.com.valence.api.dtos.ComentarioResponseDto;
import br.com.valence.api.dtos.PaginaDto;
import br.com.valence.api.dtos.PostMuralDto;
import br.com.valence.api.dtos.PostMuralEdicaoDto;
import br.com.valence.api.dtos.PostMuralResponseDto;
import br.com.valence.api.enums.Papel;
import br.com.valence.api.enums.StatusPost;
import br.com.valence.api.exceptions.AcessoNegadoException;
import br.com.valence.api.exceptions.RecursoNaoEncontradoException;
import br.com.valence.api.exceptions.RegraNegocioException;
import br.com.valence.api.models.ComentarioModel;
import br.com.valence.api.models.LaudoModel;
import br.com.valence.api.models.OcorrenciaModel;
import br.com.valence.api.models.PostMuralModel;
import br.com.valence.api.models.UsuarioModel;
import br.com.valence.api.repositories.ComentarioRepository;
import br.com.valence.api.repositories.PostMuralRepository;
import br.com.valence.api.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MuralService {

    private final PostMuralRepository postMuralRepository;
    private final ComentarioRepository comentarioRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public PaginaDto<PostMuralResponseDto> listarDaRegiao(String municipio, String uf, UsuarioModel usuario,
                                                         Pageable pageable) {
        String municipioFiltro = StringUtils.hasText(municipio) ? municipio.trim() : usuario.getMunicipio();
        String ufFiltro = StringUtils.hasText(uf) ? uf.trim() : usuario.getUf();
        return PaginaDto.de(postMuralRepository.buscarDaRegiao(StatusPost.ATIVO, municipioFiltro, ufFiltro, pageable)
                .map(this::paraResposta));
    }

    @Transactional(readOnly = true)
    public PostMuralResponseDto buscar(Long id, UsuarioModel usuario) {
        return paraResposta(carregarVisivel(id, usuario));
    }

    @Transactional
    public PostMuralResponseDto criar(PostMuralDto dto, UsuarioModel usuario) {
        PostMuralModel post = new PostMuralModel();
        post.setTexto(dto.getTexto().trim());
        post.setMunicipio(dto.getMunicipio().trim());
        post.setUf(dto.getUf().trim().toUpperCase(Locale.ROOT));
        post.setStatus(StatusPost.ATIVO);
        post.setCriadoEm(Instant.now());
        post.setAutor(usuarioRepository.getReferenceById(usuario.getId()));
        return paraResposta(postMuralRepository.save(post));
    }

    @Transactional
    public PostMuralResponseDto editar(Long id, PostMuralEdicaoDto dto, UsuarioModel usuario) {
        PostMuralModel post = carregar(id);
        if (!Objects.equals(post.getAutor().getId(), usuario.getId())) {
            throw new AcessoNegadoException("Só o autor pode editar a publicação.");
        }
        if (post.getStatus() != StatusPost.ATIVO) {
            throw new RegraNegocioException("A publicação está em moderação ou foi removida e não pode ser editada.");
        }
        post.setTexto(dto.getTexto().trim());
        return paraResposta(post);
    }

    @Transactional
    public void remover(Long id, UsuarioModel usuario) {
        PostMuralModel post = carregar(id);
        boolean autor = Objects.equals(post.getAutor().getId(), usuario.getId());
        if (!autor && !moderador(usuario)) {
            throw new AcessoNegadoException("Só o autor, um agrônomo ou um gestor pode remover a publicação.");
        }
        post.setStatus(StatusPost.REMOVIDO);
    }

    @Transactional(readOnly = true)
    public PaginaDto<ComentarioResponseDto> listarComentarios(Long postId, UsuarioModel usuario, Pageable pageable) {
        carregarVisivel(postId, usuario);
        return PaginaDto.de(comentarioRepository.findByPostId(postId, pageable).map(ComentarioResponseDto::de));
    }

    @Transactional
    public ComentarioResponseDto comentar(Long postId, ComentarioDto dto, UsuarioModel usuario) {
        PostMuralModel post = carregarVisivel(postId, usuario);
        if (post.getStatus() != StatusPost.ATIVO) {
            throw new RegraNegocioException("A publicação está em moderação e não aceita comentários.");
        }
        ComentarioModel comentario = new ComentarioModel();
        comentario.setTexto(dto.getTexto().trim());
        comentario.setCriadoEm(Instant.now());
        comentario.setPost(post);
        comentario.setAutor(usuarioRepository.getReferenceById(usuario.getId()));
        return ComentarioResponseDto.de(comentarioRepository.save(comentario));
    }

    @Transactional
    public void publicarCasoConfirmado(OcorrenciaModel ocorrencia, LaudoModel laudo, UsuarioModel agronomo) {
        String cultura = ocorrencia.getCultura().name().toLowerCase(Locale.ROOT);
        PostMuralModel post = new PostMuralModel();
        post.setTexto("Caso confirmado por agrônomo: " + laudo.getPragaConfirmada() + " em lavoura de " + cultura
                + " na região de " + ocorrencia.getPropriedade().getMunicipio() + "/"
                + ocorrencia.getPropriedade().getUf()
                + ". Confira as plantas da sua lavoura e registre no Valence qualquer sinal parecido.");
        post.setMunicipio(ocorrencia.getPropriedade().getMunicipio());
        post.setUf(ocorrencia.getPropriedade().getUf());
        post.setStatus(StatusPost.ATIVO);
        post.setOcorrenciaId(ocorrencia.getId());
        post.setCriadoEm(Instant.now());
        post.setAutor(usuarioRepository.getReferenceById(agronomo.getId()));
        postMuralRepository.save(post);
    }

    public PostMuralModel carregar(Long id) {
        return postMuralRepository.findById(id)
                .filter(post -> post.getStatus() != StatusPost.REMOVIDO)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Publicação não encontrada."));
    }

    public PostMuralModel carregarVisivel(Long id, UsuarioModel usuario) {
        PostMuralModel post = carregar(id);
        boolean autor = Objects.equals(post.getAutor().getId(), usuario.getId());
        if (post.getStatus() == StatusPost.OCULTO && !autor && !moderador(usuario)) {
            throw new RecursoNaoEncontradoException("Publicação não encontrada.");
        }
        return post;
    }

    public PostMuralResponseDto paraResposta(PostMuralModel post) {
        return PostMuralResponseDto.de(post, post.getId() == null ? 0 : comentarioRepository.countByPostId(post.getId()));
    }

    public static boolean moderador(UsuarioModel usuario) {
        return usuario.getPapel() == Papel.AGRONOMO || usuario.getPapel() == Papel.GESTOR;
    }
}
