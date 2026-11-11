package br.com.valence.api.repositories;

import br.com.valence.api.models.FotoOcorrenciaModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FotoOcorrenciaRepository extends JpaRepository<FotoOcorrenciaModel, Long> {

    Optional<FotoOcorrenciaModel> findByIdAndOcorrenciaId(Long id, Long ocorrenciaId);

    long countByOcorrenciaId(Long ocorrenciaId);
}
