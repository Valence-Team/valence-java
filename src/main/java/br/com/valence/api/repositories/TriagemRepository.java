package br.com.valence.api.repositories;

import br.com.valence.api.models.TriagemModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TriagemRepository extends JpaRepository<TriagemModel, Long> {

    Optional<TriagemModel> findByOcorrenciaId(Long ocorrenciaId);
}
