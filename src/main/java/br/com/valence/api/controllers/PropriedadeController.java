package br.com.valence.api.controllers;

import br.com.valence.api.dtos.PaginaDto;
import br.com.valence.api.dtos.PropriedadeDto;
import br.com.valence.api.dtos.PropriedadeResponseDto;
import br.com.valence.api.models.UsuarioModel;
import br.com.valence.api.services.PropriedadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/propriedades")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PRODUTOR')")
@Tag(name = "Propriedades", description = "CRUD das propriedades do produtor logado")
public class PropriedadeController {

    private final PropriedadeService propriedadeService;

    @GetMapping
    @Operation(summary = "Lista paginada das propriedades do produtor")
    public ResponseEntity<PaginaDto<PropriedadeResponseDto>> listar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario,
            @PageableDefault(size = 10, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(propriedadeService.listar(usuario, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de uma propriedade")
    public ResponseEntity<PropriedadeResponseDto> buscar(@PathVariable Long id,
                                                         @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario) {
        return ResponseEntity.ok(propriedadeService.buscar(id, usuario));
    }

    @PostMapping
    @Operation(summary = "Cadastra uma propriedade")
    public ResponseEntity<PropriedadeResponseDto> criar(@RequestBody @Valid PropriedadeDto dto,
                                                        @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario) {
        PropriedadeResponseDto criada = propriedadeService.criar(dto, usuario);
        return ResponseEntity.created(URI.create("/api/propriedades/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma propriedade")
    public ResponseEntity<PropriedadeResponseDto> atualizar(@PathVariable Long id, @RequestBody @Valid PropriedadeDto dto,
                                                            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario) {
        return ResponseEntity.ok(propriedadeService.atualizar(id, dto, usuario));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma propriedade sem ocorrências registradas")
    public ResponseEntity<Void> excluir(@PathVariable Long id,
                                        @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario) {
        propriedadeService.excluir(id, usuario);
        return ResponseEntity.noContent().build();
    }
}
