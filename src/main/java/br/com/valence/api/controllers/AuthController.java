package br.com.valence.api.controllers;

import br.com.valence.api.dtos.LoginDto;
import br.com.valence.api.dtos.RegistroDto;
import br.com.valence.api.dtos.TokenResponseDto;
import br.com.valence.api.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Autenticação", description = "Cadastro de produtor e login com JWT")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registrar")
    @Operation(summary = "Cadastra um produtor e devolve o token, como no login")
    public ResponseEntity<TokenResponseDto> registrar(@RequestBody @Valid RegistroDto dto) {
        return new ResponseEntity<>(authService.registrar(dto), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica com e-mail e senha e devolve o token JWT")
    public ResponseEntity<TokenResponseDto> login(@RequestBody @Valid LoginDto dto) {
        return ResponseEntity.ok(authService.login(dto));
    }
}
