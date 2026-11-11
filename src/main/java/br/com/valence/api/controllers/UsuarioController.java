package br.com.valence.api.controllers;

import br.com.valence.api.dtos.PerfilDto;
import br.com.valence.api.dtos.UsuarioResponseDto;
import br.com.valence.api.models.UsuarioModel;
import br.com.valence.api.services.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Perfil do usuário logado")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário logado")
    public ResponseEntity<UsuarioResponseDto> me(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario) {
        return ResponseEntity.ok(usuarioService.buscarPerfil(usuario));
    }

    @PutMapping("/me")
    @Operation(summary = "Atualiza nome, telefone, município e UF do usuário logado")
    public ResponseEntity<UsuarioResponseDto> atualizar(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioModel usuario,
                                                        @RequestBody @Valid PerfilDto dto) {
        return ResponseEntity.ok(usuarioService.atualizarPerfil(usuario, dto));
    }
}
