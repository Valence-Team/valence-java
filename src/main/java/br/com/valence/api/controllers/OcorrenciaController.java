package br.com.valence.api.controllers;

import br.com.valence.api.dtos.OcorrenciaDto;
import br.com.valence.api.dtos.OcorrenciaResponseDto;
import br.com.valence.api.dtos.PaginaDto;
import br.com.valence.api.enums.StatusOcorrencia;
import br.com.valence.api.models.FotoOcorrenciaModel;
import br.com.valence.api.models.UsuarioModel;
import br.com.valence.api.services.FotoService;
import br.com.valence.api.services.OcorrenciaService;
import br.com.valence.api.services.TriagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/ocorrencias")
@RequiredArgsConstructor
@Tag(name = "Ocorrências", description = "Registro, fotos e triagem de ocorrências fitossanitárias")
public class OcorrenciaController {

    private final OcorrenciaService ocorrenciaService;
    private final FotoService fotoService;
    private final TriagemService triagemService;

    @GetMapping
    @PreAuthorize("hasRole('PRODUTOR')")
    @Operation(summary = "Lista paginada das ocorrências do produtor, com filtro opcional de status")
    public ResponseEntity<PaginaDto<OcorrenciaResponseDto>> listar(
            @RequestParam(required = false) StatusOcorrencia status,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario,
            @PageableDefault(size = 10, sort = "dataRegistro", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ocorrenciaService.listarDoUsuario(usuario, status, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('PRODUTOR')")
    @Operation(summary = "Registra uma ocorrência; o Idempotency-Key (UUID) evita duplicar o envio offline")
    public ResponseEntity<OcorrenciaResponseDto> criar(
            @RequestBody @Valid OcorrenciaDto dto,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario) {
        OcorrenciaService.ResultadoCriacao resultado = ocorrenciaService.criar(dto, idempotencyKey, usuario);
        if (resultado.repetida()) {
            return ResponseEntity.ok().header("Idempotency-Replayed", "true").body(resultado.ocorrencia());
        }
        return ResponseEntity.created(URI.create("/api/ocorrencias/" + resultado.ocorrencia().id()))
                .body(resultado.ocorrencia());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRODUTOR', 'AGRONOMO', 'GESTOR')")
    @Operation(summary = "Detalhe com triagem e laudo; acesso do dono, do agrônomo e do gestor")
    public ResponseEntity<OcorrenciaResponseDto> buscar(@PathVariable Long id,
                                                        @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario) {
        return ResponseEntity.ok(ocorrenciaService.buscar(id, usuario));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PRODUTOR')")
    @Operation(summary = "Edita a ocorrência enquanto estiver RECEBIDA ou TRIADA")
    public ResponseEntity<OcorrenciaResponseDto> atualizar(@PathVariable Long id, @RequestBody @Valid OcorrenciaDto dto,
                                                           @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario) {
        return ResponseEntity.ok(ocorrenciaService.atualizar(id, dto, usuario));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PRODUTOR')")
    @Operation(summary = "Remove a ocorrência enquanto estiver RECEBIDA ou TRIADA")
    public ResponseEntity<Void> excluir(@PathVariable Long id,
                                        @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario) {
        ocorrenciaService.excluir(id, usuario);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PRODUTOR')")
    @Operation(summary = "Envia a foto (parte 'foto', JPEG, PNG ou WebP, até 8 MB) e roda a triagem")
    public ResponseEntity<OcorrenciaResponseDto> enviarFoto(@PathVariable Long id,
                                                            @RequestPart("foto") MultipartFile foto,
                                                            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario)
            throws IOException {
        FotoOcorrenciaModel salva = fotoService.adicionar(id, foto.getBytes(), usuario);
        triagemService.triar(id);
        return ResponseEntity.created(URI.create("/api/ocorrencias/" + id + "/fotos/" + salva.getId()))
                .body(ocorrenciaService.buscar(id, usuario));
    }

    @GetMapping("/{id}/fotos/{fotoId}")
    @PreAuthorize("hasAnyRole('PRODUTOR', 'AGRONOMO', 'GESTOR')")
    @Operation(summary = "Bytes da foto; acesso do dono, do agrônomo e do gestor")
    public ResponseEntity<byte[]> foto(@PathVariable Long id, @PathVariable Long fotoId,
                                       @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario) {
        FotoService.FotoConteudo foto = fotoService.buscar(id, fotoId, usuario);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.contentType()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(foto.conteudo());
    }

    @PostMapping("/{id}/triagem")
    @PreAuthorize("hasRole('PRODUTOR')")
    @Operation(summary = "Reprocessa a triagem com IA e devolve a ocorrência atualizada")
    public ResponseEntity<OcorrenciaResponseDto> reprocessarTriagem(@PathVariable Long id,
                                                                    @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario) {
        ocorrenciaService.validarPodeTriar(id, usuario);
        triagemService.triar(id);
        return ResponseEntity.ok(ocorrenciaService.buscar(id, usuario));
    }
}
