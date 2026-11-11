package br.com.valence.api.repositories;

import br.com.valence.api.models.UsuarioModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<UsuarioModel, Long> {

    Optional<UsuarioModel> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
