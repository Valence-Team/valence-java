package br.com.valence.api.services;

import br.com.valence.api.dtos.OcorrenciaDto;
import br.com.valence.api.dtos.OcorrenciaResponseDto;
import br.com.valence.api.dtos.PaginaDto;
import br.com.valence.api.enums.Papel;
import br.com.valence.api.enums.StatusOcorrencia;
import br.com.valence.api.exceptions.AcessoNegadoException;
import br.com.valence.api.exceptions.ConflitoException;
import br.com.valence.api.exceptions.RecursoNaoEncontradoException;
import br.com.valence.api.exceptions.RegraNegocioException;
import br.com.valence.api.exceptions.ValidacaoException;
import br.com.valence.api.mappers.OcorrenciaMapper;
import br.com.valence.api.models.FotoOcorrenciaModel;
import br.com.valence.api.models.OcorrenciaModel;
import br.com.valence.api.models.PropriedadeModel;
import br.com.valence.api.models.UsuarioModel;
import br.com.valence.api.repositories.AlertaRepository;
import br.com.valence.api.repositories.OcorrenciaRepository;
import br.com.valence.api.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OcorrenciaService {

    private static final List<StatusOcorrencia> STATUS_FILA = List.of(StatusOcorrencia.TRIADA,
            StatusOcorrencia.EM_ANALISE);

    private final OcorrenciaRepository ocorrenciaRepository;
    private final AlertaRepository alertaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PropriedadeService propriedadeService;
    private final ArmazenamentoFotoService armazenamentoFotoService;
    private final OcorrenciaMapper ocorrenciaMapper;
    private final TransactionTemplate transactionTemplate;

    public ResultadoCriacao criar(OcorrenciaDto dto, String idempotencyKey, UsuarioModel usuario) {
        String chave = normalizarChave(idempotencyKey);
        try {
            return transactionTemplate.execute(status -> criarOuRepetir(dto, chave, usuario));
        } catch (DataIntegrityViolationException ex) {
            // Requisição simultânea com a mesma chave
            return transactionTemplate.execute(status -> criarOuRepetir(dto, chave, usuario));
        }
    }

    @Transactional(readOnly = true)
    public PaginaDto<OcorrenciaResponseDto> listarDoUsuario(UsuarioModel usuario, StatusOcorrencia status,
                                                           Pageable pageable) {
        var pagina = status == null
                ? ocorrenciaRepository.findByUsuarioId(usuario.getId(), pageable)
                : ocorrenciaRepository.findByUsuarioIdAndStatus(usuario.getId(), status, pageable);
        return PaginaDto.de(pagina.map(ocorrenciaMapper::paraResposta));
    }

    @Transactional(readOnly = true)
    public PaginaDto<OcorrenciaResponseDto> listarFilaAnalise(StatusOcorrencia status, Pageable pageable) {
        if (status != null && !STATUS_FILA.contains(status)) {
            throw new ValidacaoException("status", "Use TRIADA ou EM_ANALISE.");
        }
        List<StatusOcorrencia> filtro = status == null ? STATUS_FILA : List.of(status);
        Pageable semOrdenacao = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return PaginaDto.de(ocorrenciaRepository.buscarFilaAnalise(filtro, semOrdenacao)
                .map(ocorrenciaMapper::paraResposta));
    }

    @Transactional(readOnly = true)
    public OcorrenciaResponseDto buscar(Long id, UsuarioModel usuario) {
        OcorrenciaModel ocorrencia = carregar(id);
        validarLeitura(ocorrencia, usuario);
        return ocorrenciaMapper.paraResposta(ocorrencia);
    }

    @Transactional
    public OcorrenciaResponseDto atualizar(Long id, OcorrenciaDto dto, UsuarioModel usuario) {
        OcorrenciaModel ocorrencia = carregar(id);
        validarDono(ocorrencia, usuario);
        if (!ocorrencia.getStatus().permiteEdicao()) {
            throw new RegraNegocioException("A ocorrência só pode ser editada enquanto estiver RECEBIDA ou TRIADA.");
        }
        PropriedadeModel propriedade = propriedadeService.carregarDoUsuario(dto.getPropriedadeId(), usuario);
        ocorrencia.setPropriedade(propriedade);
        ocorrencia.setCultura(dto.getCultura());
        ocorrencia.setDescricao(dto.getDescricao().trim());
        ocorrencia.setAtualizadoEm(Instant.now());
        return ocorrenciaMapper.paraResposta(ocorrencia);
    }

    @Transactional
    public void excluir(Long id, UsuarioModel usuario) {
        OcorrenciaModel ocorrencia = carregar(id);
        validarDono(ocorrencia, usuario);
        if (!ocorrencia.getStatus().permiteEdicao()) {
            throw new RegraNegocioException("A ocorrência só pode ser removida enquanto estiver RECEBIDA ou TRIADA.");
        }
        List<String> arquivos = ocorrencia.getFotos().stream().map(FotoOcorrenciaModel::getCaminhoArquivo).toList();
        alertaRepository.excluirDaOcorrencia(ocorrencia.getId());
        ocorrenciaRepository.delete(ocorrencia);
        arquivos.forEach(armazenamentoFotoService::remover);
    }

    @Transactional(readOnly = true)
    public void validarPodeTriar(Long id, UsuarioModel usuario) {
        OcorrenciaModel ocorrencia = carregar(id);
        validarDono(ocorrencia, usuario);
        if (!ocorrencia.getStatus().permiteTriagem()) {
            throw new RegraNegocioException("A ocorrência já tem laudo final e não passa por nova triagem.");
        }
    }

    public OcorrenciaModel carregar(Long id) {
        return ocorrenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ocorrência não encontrada."));
    }

    public void validarLeitura(OcorrenciaModel ocorrencia, UsuarioModel usuario) {
        boolean dono = Objects.equals(ocorrencia.getUsuario().getId(), usuario.getId());
        boolean tecnico = usuario.getPapel() == Papel.AGRONOMO || usuario.getPapel() == Papel.GESTOR;
        if (!dono && !tecnico) {
            throw new AcessoNegadoException("Esta ocorrência pertence a outro produtor.");
        }
    }

    public void validarDono(OcorrenciaModel ocorrencia, UsuarioModel usuario) {
        if (!Objects.equals(ocorrencia.getUsuario().getId(), usuario.getId())) {
            throw new AcessoNegadoException("Só o produtor que registrou a ocorrência pode fazer isso.");
        }
    }

    private ResultadoCriacao criarOuRepetir(OcorrenciaDto dto, String chave, UsuarioModel usuario) {
        var existente = ocorrenciaRepository.findByUsuarioIdAndIdempotencyKey(usuario.getId(), chave);
        if (existente.isPresent()) {
            OcorrenciaModel original = existente.get();
            boolean mesmoConteudo = Objects.equals(original.getPropriedade().getId(), dto.getPropriedadeId())
                    && original.getCultura() == dto.getCultura()
                    && original.getDescricao().equals(dto.getDescricao().trim());
            if (!mesmoConteudo) {
                throw new ConflitoException("Esta Idempotency-Key já foi usada com outro conteúdo.");
            }
            return new ResultadoCriacao(ocorrenciaMapper.paraResposta(original), true);
        }
        PropriedadeModel propriedade = propriedadeService.carregarDoUsuario(dto.getPropriedadeId(), usuario);
        Instant agora = Instant.now();
        OcorrenciaModel ocorrencia = new OcorrenciaModel();
        ocorrencia.setCultura(dto.getCultura());
        ocorrencia.setDescricao(dto.getDescricao().trim());
        ocorrencia.setStatus(StatusOcorrencia.RECEBIDA);
        ocorrencia.setIdempotencyKey(chave);
        ocorrencia.setDataRegistro(agora);
        ocorrencia.setAtualizadoEm(agora);
        ocorrencia.setPropriedade(propriedade);
        ocorrencia.setUsuario(usuarioRepository.getReferenceById(usuario.getId()));
        OcorrenciaModel salva = ocorrenciaRepository.saveAndFlush(ocorrencia);
        return new ResultadoCriacao(ocorrenciaMapper.paraResposta(salva), false);
    }

    private String normalizarChave(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return UUID.randomUUID().toString();
        }
        String chave = idempotencyKey.trim().toLowerCase(Locale.ROOT);
        if (!uuidValido(chave)) {
            throw new ValidacaoException("Idempotency-Key",
                    "Use um UUID válido, como 3f6c1e2a-8b4d-4e9a-9c1f-2d7e5a6b8c90.");
        }
        return chave;
    }

    private static boolean uuidValido(String valor) {
        try {
            return UUID.fromString(valor).toString().equals(valor);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public record ResultadoCriacao(OcorrenciaResponseDto ocorrencia, boolean repetida) {
    }
}
