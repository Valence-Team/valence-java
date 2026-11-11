package br.com.valence.api.security;

import br.com.valence.api.dtos.ErroResponseDto;
import br.com.valence.api.exceptions.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class RespostaErroJson {

    private final ObjectMapper objectMapper;

    public void escrever(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String mensagem)
            throws IOException {
        ErroResponseDto corpo = ErroResponseDto.de(status.value(), GlobalExceptionHandler.titulo(status), mensagem,
                request.getRequestURI());
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), corpo);
    }
}
