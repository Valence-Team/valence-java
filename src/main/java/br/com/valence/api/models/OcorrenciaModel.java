package br.com.valence.api.models;

import br.com.valence.api.enums.Cultura;
import br.com.valence.api.enums.StatusOcorrencia;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "TB_OCORRENCIA", uniqueConstraints = @UniqueConstraint(
        name = "uk_ocorrencia_usuario_chave", columnNames = {"usuario_id", "idempotency_key"}))
public class OcorrenciaModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Cultura cultura;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOcorrencia status;

    private Integer severidade;

    @Column(nullable = false, length = 36)
    private String idempotencyKey;

    @Column(nullable = false)
    private Instant dataRegistro;

    @Column(nullable = false)
    private Instant atualizadoEm;

    @Version
    private Long versao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "propriedade_id", nullable = false)
    private PropriedadeModel propriedade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioModel usuario;

    @OrderBy("enviadaEm ASC")
    @OneToMany(mappedBy = "ocorrencia", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FotoOcorrenciaModel> fotos = new ArrayList<>();

    @OneToOne(mappedBy = "ocorrencia", cascade = CascadeType.ALL, orphanRemoval = true)
    private TriagemModel triagem;

    @OrderBy("emitidoEm DESC")
    @OneToMany(mappedBy = "ocorrencia", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LaudoModel> laudos = new ArrayList<>();
}
