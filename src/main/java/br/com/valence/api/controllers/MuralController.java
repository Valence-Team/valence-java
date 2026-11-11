package br.com.valence.api.controllers;

import br.com.valence.api.dtos.ComentarioDto;
import br.com.valence.api.dtos.ComentarioResponseDto;
import br.com.valence.api.dtos.DenunciaDto;
import br.com.valence.api.dtos.DenunciaResponseDto;
import br.com.valence.api.dtos.PaginaDto;
import br.com.valence.api.dtos.PostMuralDto;
import br.com.valence.api.dtos.PostMuralEdicaoDto;
import br.com.valence.api.dtos.PostMuralResponseDto;
import br.com.valence.api.models.UsuarioModel;
import br.com.valence.api.services.DenunciaService;
import br.com.valence.api.services.MuralService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/mural")
@RequiredArgsConstructor
@Tag(name = "Mural", description = "Publicações da região, comentários e denúncias")
public class MuralController {

    private final MuralService muralService;
    private final DenunciaService denunciaService;

    @GetMapping
    @Operation(summary = "Publicações ativas da região; sem filtro, usa o município do usuário")
    public ResponseEntity<PaginaDto<PostMuralResponseDto>> listar(
            @RequestParam(required = false) String municipio,
            @RequestParam(required = false) String uf,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario,
            @PageableDefault(size = 10, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(muralService.listarDaRegiao(municipio, uf, usuario, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de uma publicação")
    public ResponseEntity<PostMuralResponseDto> buscar(@PathVariable Long id,
                                                       @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario) {
        return ResponseEntity.ok(muralService.buscar(id, usuario));
    }

    @PostMapping
    @Operation(summary = "Cria uma publicação no mural da região")
    public ResponseEntity<PostMuralResponseDto> criar(@RequestBody @Valid PostMuralDto dto,
                                                      @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario) {
        PostMuralResponseDto criada = muralService.criar(dto, usuario);
        return ResponseEntity.created(URI.create("/api/mural/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita o texto da publicação (só o autor)")
    public ResponseEntity<PostMuralResponseDto> editar(@PathVariable Long id, @RequestBody @Valid PostMuralEdicaoDto dto,
                                                       @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario) {
        return ResponseEntity.ok(muralService.editar(id, dto, usuario));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a publicação (autor, agrônomo ou gestor)")
    public ResponseEntity<Void> remover(@PathVariable Long id,
                                        @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario) {
        muralService.remover(id, usuario);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/comentarios")
    @Operation(summary = "Comentários da publicação")
    public ResponseEntity<PaginaDto<ComentarioResponseDto>> listarComentarios(
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario,
            @PageableDefault(size = 20, sort = "criadoEm", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(muralService.listarComentarios(id, usuario, pageable));
    }

    @PostMapping("/{id}/comentarios")
    @Operation(summary = "Comenta a publicação")
    public ResponseEntity<ComentarioResponseDto> comentar(@PathVariable Long id, @RequestBody @Valid ComentarioDto dto,
                                                          @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario) {
        return new ResponseEntity<>(muralService.comentar(id, dto, usuario), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/denuncias")
    @Operation(summary = "Denuncia a publicação; o Spring AI classifica e, se grave, oculta para moderação")
    public ResponseEntity<DenunciaResponseDto> denunciar(@PathVariable Long id, @RequestBody @Valid DenunciaDto dto,
                                                         @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario) {
        return new ResponseEntity<>(denunciaService.denunciar(id, dto, usuario), HttpStatus.CREATED);
    }
}
