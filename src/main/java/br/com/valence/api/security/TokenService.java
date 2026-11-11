package br.com.valence.api.security;

import br.com.valence.api.models.UsuarioModel;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
public class TokenService {

    private static final int TAMANHO_MINIMO_SEGREDO = 32;

    private final Algorithm algoritmo;
    private final JWTVerifier verificador;
    private final long expiracaoMinutos;
    private final String emissor;

    // Método construtor
    public TokenService(@Value("${valence.jwt.secret:}") String segredo,
                        @Value("${valence.jwt.expiracao-minutos:120}") long expiracaoMinutos,
                        @Value("${valence.jwt.emissor:valence-api}") String emissor,
                        Environment environment) {
        String chave = segredo;
        if (!StringUtils.hasText(chave)) {
            if (environment.acceptsProfiles(Profiles.of("prod"))) {
                throw new IllegalStateException("Defina a variável de ambiente JWT_SECRET para o perfil prod.");
            }
            chave = gerarSegredoTemporario();
            log.warn("JWT_SECRET não definido: usando segredo temporário. Os tokens deixam de valer ao reiniciar.");
        } else if (chave.length() < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException("JWT_SECRET precisa ter pelo menos 32 caracteres.");
        }
        this.algoritmo = Algorithm.HMAC256(chave);
        this.verificador = JWT.require(algoritmo).withIssuer(emissor).build();
        this.expiracaoMinutos = expiracaoMinutos;
        this.emissor = emissor;
    }

    public TokenGerado gerar(UsuarioModel usuario) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(expiracaoMinutos, ChronoUnit.MINUTES);
        String token = JWT.create()
                .withIssuer(emissor)
                .withSubject(String.valueOf(usuario.getId()))
                .withClaim("email", usuario.getEmail())
                .withClaim("papel", usuario.getPapel().name())
                .withIssuedAt(agora)
                .withExpiresAt(expiraEm)
                .sign(algoritmo);
        return new TokenGerado(token, expiraEm);
    }

    public Optional<Long> validar(String token) {
        try {
            return Optional.of(Long.valueOf(verificador.verify(token).getSubject()));
        } catch (JWTVerificationException | NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private static String gerarSegredoTemporario() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public record TokenGerado(String token, Instant expiraEm) {
    }
}
