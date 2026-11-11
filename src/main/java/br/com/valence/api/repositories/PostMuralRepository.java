package br.com.valence.api.repositories;

import br.com.valence.api.enums.StatusPost;
import br.com.valence.api.models.PostMuralModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface PostMuralRepository extends JpaRepository<PostMuralModel, Long> {

    @Query(value = """
            select p from PostMuralModel p
            where p.status = :status and lower(p.municipio) = lower(:municipio) and upper(p.uf) = upper(:uf)
            """,
            countQuery = """
            select count(p) from PostMuralModel p
            where p.status = :status and lower(p.municipio) = lower(:municipio) and upper(p.uf) = upper(:uf)
            """)
    Page<PostMuralModel> buscarDaRegiao(@Param("status") StatusPost status,
                                        @Param("municipio") String municipio,
                                        @Param("uf") String uf,
                                        Pageable pageable);

    @Query(value = """
            select p from PostMuralModel p
            where p.status in :status
              and exists (select d.id from DenunciaModel d where d.post = p and d.decisaoFinal is null)
            order by p.status desc, p.criadoEm desc
            """,
            countQuery = """
            select count(p) from PostMuralModel p
            where p.status in :status
              and exists (select d.id from DenunciaModel d where d.post = p and d.decisaoFinal is null)
            """)
    Page<PostMuralModel> buscarFilaModeracao(@Param("status") Collection<StatusPost> status, Pageable pageable);
}
