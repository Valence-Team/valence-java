package br.com.valence.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final RespostaErroJson respostaErroJson;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        boolean tokenInvalido = Boolean.TRUE.equals(request.getAttribute(SecurityFilter.ATRIBUTO_TOKEN_INVALIDO));
        String mensagem = tokenInvalido
                ? "Token inválido ou expirado. Faça login novamente."
                : "Envie o token no cabeçalho Authorization: Bearer <token>.";
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        respostaErroJson.escrever(request, response, HttpStatus.UNAUTHORIZED, mensagem);
    }
}
