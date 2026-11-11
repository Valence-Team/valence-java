package br.com.valence.api.services;

import br.com.valence.api.dtos.LoginDto;
import br.com.valence.api.dtos.RegistroDto;
import br.com.valence.api.dtos.TokenResponseDto;
import br.com.valence.api.dtos.UsuarioResponseDto;
import br.com.valence.api.enums.Papel;
import br.com.valence.api.exceptions.ConflitoException;
import br.com.valence.api.exceptions.CredenciaisInvalidasException;
import br.com.valence.api.models.UsuarioModel;
import br.com.valence.api.repositories.UsuarioRepository;
import br.com.valence.api.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    @Transactional
    public TokenResponseDto registrar(RegistroDto dto) {
        String email = dto.getEmail().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException("Já existe uma conta com este e-mail.");
        }
        UsuarioModel usuario = new UsuarioModel();
        usuario.setNome(dto.getNome().trim());
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(dto.getSenha()));
        usuario.setPapel(Papel.PRODUTOR);
        usuario.setTelefone(StringUtils.hasText(dto.getTelefone()) ? dto.getTelefone().trim() : null);
        usuario.setMunicipio(dto.getMunicipio().trim());
        usuario.setUf(dto.getUf().trim().toUpperCase(Locale.ROOT));
        usuario.setCriadoEm(Instant.now());
        return gerarResposta(usuarioRepository.save(usuario));
    }

    public TokenResponseDto login(LoginDto dto) {
        try {
            Authentication autenticacao = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getEmail().trim(), dto.getSenha()));
            return gerarResposta((UsuarioModel) autenticacao.getPrincipal());
        } catch (AuthenticationException ex) {
            throw new CredenciaisInvalidasException("E-mail ou senha inválidos.");
        }
    }

    private TokenResponseDto gerarResposta(UsuarioModel usuario) {
        TokenService.TokenGerado token = tokenService.gerar(usuario);
        return new TokenResponseDto(token.token(), "Bearer", token.expiraEm(), UsuarioResponseDto.de(usuario));
    }
}
