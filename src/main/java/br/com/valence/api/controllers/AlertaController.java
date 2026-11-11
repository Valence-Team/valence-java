package br.com.valence.api.controllers;

import br.com.valence.api.dtos.AlertaResponseDto;
import br.com.valence.api.dtos.PaginaDto;
import br.com.valence.api.models.UsuarioModel;
import br.com.valence.api.services.AlertaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PRODUTOR')")
@Tag(name = "Alertas", description = "Alertas de pragas nas propriedades vizinhas")
public class AlertaController {

    private final AlertaService alertaService;

    @GetMapping
    @Operation(summary = "Alertas recebidos pelas propriedades do produtor")
    public ResponseEntity<PaginaDto<AlertaResponseDto>> listar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario,
            @PageableDefault(size = 10, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(alertaService.listar(usuario, pageable));
    }

    @PatchMapping("/{id}/lido")
    @Operation(summary = "Marca o alerta como lido")
    public ResponseEntity<AlertaResponseDto> marcarComoLido(@PathVariable Long id,
                                                            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario) {
        return ResponseEntity.ok(alertaService.marcarComoLido(id, usuario));
    }
}
