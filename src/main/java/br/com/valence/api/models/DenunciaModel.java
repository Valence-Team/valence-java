package br.com.valence.api.models;

import br.com.valence.api.enums.AcaoModeracao;
import br.com.valence.api.enums.CategoriaDenuncia;
import br.com.valence.api.enums.GravidadeDenuncia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "TB_DENUNCIA", uniqueConstraints = @UniqueConstraint(
        name = "uk_denuncia_post_usuario", columnNames = {"post_id", "usuario_id"}))
public class DenunciaModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CategoriaDenuncia categoriaIa;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private GravidadeDenuncia gravidadeIa;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AcaoModeracao acaoIa;

    @Column(length = 500)
    private String justificativaIa;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AcaoModeracao decisaoFinal;

    @Column(nullable = false)
    private Instant criadaEm;

    private Instant decididaEm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private PostMuralModel post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioModel usuario;
}
