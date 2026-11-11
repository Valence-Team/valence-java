package br.com.valence.api.repositories;

import br.com.valence.api.enums.StatusOcorrencia;
import br.com.valence.api.models.OcorrenciaModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OcorrenciaRepository extends JpaRepository<OcorrenciaModel, Long> {

    Page<OcorrenciaModel> findByUsuarioId(Long usuarioId, Pageable pageable);

    Page<OcorrenciaModel> findByUsuarioIdAndStatus(Long usuarioId, StatusOcorrencia status, Pageable pageable);

    Optional<OcorrenciaModel> findByUsuarioIdAndIdempotencyKey(Long usuarioId, String idempotencyKey);

    boolean existsByPropriedadeId(Long propriedadeId);

    List<OcorrenciaModel> findTop5ByStatusAndDataRegistroBeforeOrderByDataRegistroAsc(StatusOcorrencia status,
                                                                                    Instant limite);

    @Query(value = """
            select o from OcorrenciaModel o
            where o.status in :status
            order by coalesce(o.severidade, 0) desc, o.dataRegistro asc
            """,
            countQuery = "select count(o) from OcorrenciaModel o where o.status in :status")
    Page<OcorrenciaModel> buscarFilaAnalise(@Param("status") Collection<StatusOcorrencia> status, Pageable pageable);
}
