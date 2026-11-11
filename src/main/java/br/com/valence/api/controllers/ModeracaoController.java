package br.com.valence.api.controllers;

import br.com.valence.api.dtos.ModeracaoDto;
import br.com.valence.api.dtos.ModeracaoItemDto;
import br.com.valence.api.dtos.PaginaDto;
import br.com.valence.api.services.ModeracaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/moderacao")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('AGRONOMO', 'GESTOR')")
@Tag(name = "Moderação", description = "Decisão humana sobre publicações denunciadas")
public class ModeracaoController {

    private final ModeracaoService moderacaoService;

    @GetMapping
    @Operation(summary = "Fila de publicações com denúncias pendentes, ocultas primeiro")
    public ResponseEntity<PaginaDto<ModeracaoItemDto>> fila(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(moderacaoService.listarFila(pageable));
    }

    @PatchMapping("/{postId}")
    @Operation(summary = "Decide a publicação: MANTER ou REMOVER")
    public ResponseEntity<ModeracaoItemDto> decidir(@PathVariable Long postId, @RequestBody @Valid ModeracaoDto dto) {
        return ResponseEntity.ok(moderacaoService.decidir(postId, dto));
    }
}
