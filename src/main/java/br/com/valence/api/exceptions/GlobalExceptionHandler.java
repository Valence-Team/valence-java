package br.com.valence.api.exceptions;

import br.com.valence.api.dtos.CampoErroDto;
import br.com.valence.api.dtos.ErroResponseDto;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Validação (400)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponseDto> tratarValidacao(MethodArgumentNotValidException ex,
                                                           HttpServletRequest request) {
        List<CampoErroDto> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new CampoErroDto(erro.getField(), erro.getDefaultMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos.", request, campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResponseDto> tratarRestricao(ConstraintViolationException ex,
                                                           HttpServletRequest request) {
        List<CampoErroDto> campos = ex.getConstraintViolations().stream()
                .map(violacao -> new CampoErroDto(violacao.getPropertyPath().toString(), violacao.getMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos.", request, campos);
    }

    @ExceptionHandler(ValidacaoException.class)
    public ResponseEntity<ErroResponseDto> tratarValidacaoDeNegocio(ValidacaoException ex,
                                                                    HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos.", request,
                List.of(new CampoErroDto(ex.getCampo(), ex.getMessage())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponseDto> tratarCorpoInvalido(HttpMessageNotReadableException ex,
                                                               HttpServletRequest request) {
        if (ex.getCause() instanceof InvalidFormatException formato && formato.getTargetType().isEnum()) {
            String valores = Arrays.stream(formato.getTargetType().getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            return responder(HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos.", request,
                    List.of(new CampoErroDto(caminhoJson(formato), "Valor inválido. Use: " + valores + ".")));
        }
        return responder(HttpStatus.BAD_REQUEST, "O corpo da requisição está ausente ou mal formatado.", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponseDto> tratarTipoInvalido(MethodArgumentTypeMismatchException ex,
                                                              HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "Um ou mais parâmetros são inválidos.", request,
                List.of(new CampoErroDto(ex.getName(), "Valor inválido: " + ex.getValue() + ".")));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class,
            MissingRequestHeaderException.class})
    public ResponseEntity<ErroResponseDto> tratarParametroAusente(Exception ex, HttpServletRequest request) {
        String campo = switch (ex) {
            case MissingServletRequestParameterException e -> e.getParameterName();
            case MissingServletRequestPartException e -> e.getRequestPartName();
            case MissingRequestHeaderException e -> e.getHeaderName();
            default -> "requisicao";
        };
        return responder(HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos.", request,
                List.of(new CampoErroDto(campo, "Campo obrigatório.")));
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErroResponseDto> tratarOrdenacaoInvalida(PropertyReferenceException ex,
                                                                   HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "Um ou mais parâmetros são inválidos.", request,
                List.of(new CampoErroDto("sort", "Campo de ordenação inválido: " + ex.getPropertyName() + ".")));
    }

    // Autenticação e autorização (401 e 403)
    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResponseDto> tratarCredenciais(CredenciaisInvalidasException ex,
                                                             HttpServletRequest request) {
        return responder(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler({AcessoNegadoException.class, AccessDeniedException.class})
    public ResponseEntity<ErroResponseDto> tratarAcessoNegado(RuntimeException ex, HttpServletRequest request) {
        String mensagem = ex instanceof AcessoNegadoException
                ? ex.getMessage()
                : "Você não tem permissão para acessar este recurso.";
        return responder(HttpStatus.FORBIDDEN, mensagem, request);
    }

    // Recursos (404 e 405)
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponseDto> tratarNaoEncontrado(RecursoNaoEncontradoException ex,
                                                               HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponseDto> tratarRotaInexistente(NoResourceFoundException ex,
                                                                 HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, "Recurso não encontrado.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponseDto> tratarMetodo(HttpRequestMethodNotSupportedException ex,
                                                        HttpServletRequest request) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não suportado nesta rota.", request);
    }

    // Conflitos (409)
    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResponseDto> tratarConflito(ConflitoException ex, HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponseDto> tratarIntegridade(DataIntegrityViolationException ex,
                                                             HttpServletRequest request) {
        log.warn("Violação de integridade em {}", request.getRequestURI());
        return responder(HttpStatus.CONFLICT, "A operação conflita com um registro existente.", request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErroResponseDto> tratarConcorrencia(ObjectOptimisticLockingFailureException ex,
                                                              HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT,
                "O registro foi alterado por outra requisição. Recarregue e tente de novo.", request);
    }

    // Arquivos (413 e 415)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroResponseDto> tratarArquivoGrande(MaxUploadSizeExceededException ex,
                                                               HttpServletRequest request) {
        return responder(HttpStatus.PAYLOAD_TOO_LARGE, "A foto deve ter até 8 MB.", request);
    }

    @ExceptionHandler({MidiaNaoSuportadaException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ErroResponseDto> tratarMidia(Exception ex, HttpServletRequest request) {
        String mensagem = ex instanceof MidiaNaoSuportadaException
                ? ex.getMessage()
                : "Tipo de conteúdo não suportado nesta rota.";
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, mensagem, request);
    }

    // Regras de negócio (422)
    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponseDto> tratarRegra(RegraNegocioException ex, HttpServletRequest request) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    // Erro inesperado (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponseDto> tratarErroInesperado(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado em {}", request.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente em instantes.", request);
    }

    private ResponseEntity<ErroResponseDto> responder(HttpStatus status, String mensagem, HttpServletRequest request) {
        return responder(status, mensagem, request, List.of());
    }

    private ResponseEntity<ErroResponseDto> responder(HttpStatus status, String mensagem, HttpServletRequest request,
                                                      List<CampoErroDto> campos) {
        ErroResponseDto corpo = ErroResponseDto.de(status.value(), titulo(status), mensagem,
                request.getRequestURI(), campos);
        return ResponseEntity.status(status).body(corpo);
    }

    public static String titulo(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Requisição inválida";
            case UNAUTHORIZED -> "Não autenticado";
            case FORBIDDEN -> "Acesso negado";
            case NOT_FOUND -> "Não encontrado";
            case METHOD_NOT_ALLOWED -> "Método não permitido";
            case CONFLICT -> "Conflito";
            case PAYLOAD_TOO_LARGE -> "Arquivo muito grande";
            case UNSUPPORTED_MEDIA_TYPE -> "Tipo de mídia não suportado";
            case UNPROCESSABLE_ENTITY -> "Regra de negócio";
            case TOO_MANY_REQUESTS -> "Muitas requisições";
            case SERVICE_UNAVAILABLE -> "Serviço indisponível";
            default -> "Erro interno";
        };
    }

    private static String caminhoJson(JsonMappingException ex) {
        StringBuilder caminho = new StringBuilder();
        for (JsonMappingException.Reference referencia : ex.getPath()) {
            if (referencia.getFieldName() != null) {
                if (!caminho.isEmpty()) {
                    caminho.append('.');
                }
                caminho.append(referencia.getFieldName());
            } else {
                caminho.append('[').append(referencia.getIndex()).append(']');
            }
        }
        return caminho.toString();
    }
}
