package br.com.valence.api.controllers;

import br.com.valence.api.dtos.LaudoDto;
import br.com.valence.api.dtos.LaudoResponseDto;
import br.com.valence.api.models.UsuarioModel;
import br.com.valence.api.services.LaudoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Laudos", description = "Validação humana do agrônomo")
public class LaudoController {

    private final LaudoService laudoService;

    @PostMapping("/api/ocorrencias/{id}/laudo")
    @PreAuthorize("hasRole('AGRONOMO')")
    @Operation(summary = "Emite o laudo (CONFIRMADA, DESCARTADA ou MAIS_INFORMACOES) e publica laudo.emitido")
    public ResponseEntity<LaudoResponseDto> emitir(@PathVariable Long id, @RequestBody @Valid LaudoDto dto,
                                                   @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel agronomo) {
        return new ResponseEntity<>(laudoService.emitir(id, dto, agronomo), HttpStatus.CREATED);
    }
}
