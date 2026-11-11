package br.com.valence.api.repositories;

import br.com.valence.api.models.LaudoModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaudoRepository extends JpaRepository<LaudoModel, Long> {
}
