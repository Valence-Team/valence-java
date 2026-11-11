package br.com.valence.api.repositories;

import br.com.valence.api.models.DenunciaModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DenunciaRepository extends JpaRepository<DenunciaModel, Long> {

    boolean existsByPostIdAndUsuarioId(Long postId, Long usuarioId);

    List<DenunciaModel> findByPostIdAndDecisaoFinalIsNull(Long postId);
}
