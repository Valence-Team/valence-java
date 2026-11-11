package br.com.valence.api.repositories;

import br.com.valence.api.enums.Cultura;
import br.com.valence.api.models.PropriedadeModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PropriedadeRepository extends JpaRepository<PropriedadeModel, Long> {

    Page<PropriedadeModel> findByUsuarioId(Long usuarioId, Pageable pageable);

    Optional<PropriedadeModel> findByIdAndUsuarioId(Long id, Long usuarioId);

    @Query("""
            select distinct p from PropriedadeModel p join p.culturas c
            where c = :cultura and p.id <> :origemId
              and p.latitude between :latitudeMinima and :latitudeMaxima
              and p.longitude between :longitudeMinima and :longitudeMaxima
            """)
    List<PropriedadeModel> buscarCandidatasAlerta(@Param("cultura") Cultura cultura,
                                                  @Param("origemId") Long origemId,
                                                  @Param("latitudeMinima") double latitudeMinima,
                                                  @Param("latitudeMaxima") double latitudeMaxima,
                                                  @Param("longitudeMinima") double longitudeMinima,
                                                  @Param("longitudeMaxima") double longitudeMaxima);
}
