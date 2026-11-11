package br.com.valence.api.repositories;

import br.com.valence.api.models.AlertaModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlertaRepository extends JpaRepository<AlertaModel, Long> {

    @Query(value = "select a from AlertaModel a where a.propriedade.usuario.id = :usuarioId",
            countQuery = "select count(a) from AlertaModel a where a.propriedade.usuario.id = :usuarioId")
    Page<AlertaModel> buscarDoUsuario(@Param("usuarioId") Long usuarioId, Pageable pageable);

    @Query("select a from AlertaModel a where a.id = :id and a.propriedade.usuario.id = :usuarioId")
    Optional<AlertaModel> buscarPorIdDoUsuario(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    List<AlertaModel> findByOcorrenciaId(Long ocorrenciaId);

    boolean existsByOcorrenciaIdAndPropriedadeId(Long ocorrenciaId, Long propriedadeId);

    @Modifying
    @Query("delete from AlertaModel a where a.ocorrencia.id = :ocorrenciaId")
    void excluirDaOcorrencia(@Param("ocorrenciaId") Long ocorrenciaId);

    @Modifying
    @Query("delete from AlertaModel a where a.propriedade.id = :propriedadeId")
    void excluirDaPropriedade(@Param("propriedadeId") Long propriedadeId);
}
