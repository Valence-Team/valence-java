package br.com.valence.api.repositories;

import br.com.valence.api.models.ComentarioModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComentarioRepository extends JpaRepository<ComentarioModel, Long> {

    Page<ComentarioModel> findByPostId(Long postId, Pageable pageable);

    long countByPostId(Long postId);
}
