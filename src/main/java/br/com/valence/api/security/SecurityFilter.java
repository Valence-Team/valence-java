package br.com.valence.api.security;

import br.com.valence.api.models.UsuarioModel;
import br.com.valence.api.repositories.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    public static final String ATRIBUTO_TOKEN_INVALIDO = "valence.tokenInvalido";
    private static final String PREFIXO = "Bearer ";

    private final TokenService tokenService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho != null && cabecalho.startsWith(PREFIXO)) {
            Optional<UsuarioModel> usuario = tokenService.validar(cabecalho.substring(PREFIXO.length()).trim())
                    .flatMap(usuarioRepository::findById);
            if (usuario.isPresent()) {
                UsernamePasswordAuthenticationToken autenticacao = new UsernamePasswordAuthenticationToken(
                        usuario.get(), null, usuario.get().getAuthorities());
                SecurityContext contexto = SecurityContextHolder.createEmptyContext();
                contexto.setAuthentication(autenticacao);
                SecurityContextHolder.setContext(contexto);
            } else {
                request.setAttribute(ATRIBUTO_TOKEN_INVALIDO, Boolean.TRUE);
            }
        }
        chain.doFilter(request, response);
    }
}
