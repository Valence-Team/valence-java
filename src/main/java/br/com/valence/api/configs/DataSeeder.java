package br.com.valence.api.configs;

import br.com.valence.api.enums.Cultura;
import br.com.valence.api.enums.Papel;
import br.com.valence.api.models.PropriedadeModel;
import br.com.valence.api.models.UsuarioModel;
import br.com.valence.api.repositories.PropriedadeRepository;
import br.com.valence.api.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "valence.seed.habilitado", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    // Hashes BCrypt das senhas de teste documentadas no README
    private static final String HASH_PRODUTOR = "$2b$10$y3E..kDJENNjysA72xQ9iuiQ2KR27nPLIemB.7ivSS7fWllQgAfi6";
    private static final String HASH_VIZINHO = "$2b$10$GYPfJhdQzCh.8nvGcpWOquPREeCG/c/8uQRRXbC0nhEp/LtMpfH.u";
    private static final String HASH_AGRONOMO = "$2b$10$d1f/lCa8M9QDOzuPJebMouzxsJRE4yqlQhsgjglpk.SgAgeXi92s.";
    private static final String HASH_GESTOR = "$2b$10$AyKHnguF.26d3qQ7OtQ5iOi9bqWPLv8oB0PwxdfL5lrA1K0cYYice";

    private final UsuarioRepository usuarioRepository;
    private final PropriedadeRepository propriedadeRepository;

    @Override
    @Transactional
    public void run(String... args) {
        UsuarioModel produtor = criarUsuario("Ana Produtora", "produtor@valence.local", HASH_PRODUTOR, Papel.PRODUTOR);
        UsuarioModel vizinho = criarUsuario("Bruno Vizinho", "vizinho@valence.local", HASH_VIZINHO, Papel.PRODUTOR);
        criarUsuario("Carla Agrônoma", "agronomo@valence.local", HASH_AGRONOMO, Papel.AGRONOMO);
        criarUsuario("Diego Gestor", "gestor@valence.local", HASH_GESTOR, Papel.GESTOR);
        if (produtor != null) {
            criarPropriedade(produtor, "Sítio Boa Esperança", -21.2050, -47.8550, 12.5, Set.of(Cultura.MILHO));
        }
        if (vizinho != null) {
            criarPropriedade(vizinho, "Chácara Santa Luzia", -21.2300, -47.8000, 8.0,
                    Set.of(Cultura.MILHO, Cultura.TOMATE));
        }
    }

    private UsuarioModel criarUsuario(String nome, String email, String hash, Papel papel) {
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            return null;
        }
        UsuarioModel usuario = new UsuarioModel();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenhaHash(hash);
        usuario.setPapel(papel);
        usuario.setMunicipio("Ribeirão Preto");
        usuario.setUf("SP");
        usuario.setCriadoEm(Instant.now());
        log.info("Usuário de teste criado: {} ({}).", email, papel);
        return usuarioRepository.save(usuario);
    }

    private void criarPropriedade(UsuarioModel dono, String nome, double latitude, double longitude, double area,
                                  Set<Cultura> culturas) {
        PropriedadeModel propriedade = new PropriedadeModel();
        propriedade.setNome(nome);
        propriedade.setMunicipio("Ribeirão Preto");
        propriedade.setUf("SP");
        propriedade.setLatitude(latitude);
        propriedade.setLongitude(longitude);
        propriedade.setAreaHectares(area);
        propriedade.getCulturas().addAll(culturas);
        propriedade.setCriadoEm(Instant.now());
        propriedade.setUsuario(dono);
        propriedadeRepository.save(propriedade);
    }
}
