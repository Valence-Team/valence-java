package br.com.valence.api.repositories;

import br.com.valence.api.models.EventoProcessadoModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventoProcessadoRepository extends JpaRepository<EventoProcessadoModel, UUID> {
}
