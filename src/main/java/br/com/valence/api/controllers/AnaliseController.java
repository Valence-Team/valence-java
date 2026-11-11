package br.com.valence.api.controllers;

import br.com.valence.api.dtos.OcorrenciaResponseDto;
import br.com.valence.api.dtos.PaginaDto;
import br.com.valence.api.enums.StatusOcorrencia;
import br.com.valence.api.services.OcorrenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analises")
@RequiredArgsConstructor
@PreAuthorize("hasRole('AGRONOMO')")
@Tag(name = "Análises", description = "Fila do agrônomo")
public class AnaliseController {

    private final OcorrenciaService ocorrenciaService;

    @GetMapping
    @Operation(summary = "Fila de ocorrências TRIADA e EM_ANALISE, da maior severidade para a menor")
    public ResponseEntity<PaginaDto<OcorrenciaResponseDto>> fila(@RequestParam(required = false) StatusOcorrencia status,
                                                                 @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ocorrenciaService.listarFilaAnalise(status, pageable));
    }
}
