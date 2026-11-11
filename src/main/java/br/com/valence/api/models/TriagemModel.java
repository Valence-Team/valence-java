package br.com.valence.api.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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
@Table(name = "TB_TRIAGEM")
public class TriagemModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String hipoteses;

    private Double confianca;

    private Integer severidade;

    @Column(nullable = false, length = 2000)
    private String cuidadosImediatos;

    @Column(length = 255)
    private String textoAlerta;

    @Column(nullable = false)
    private boolean encaminharAgronomo;

    @Column(nullable = false)
    private Instant geradaEm;

    @Column(length = 1500)
    private String explicacao;

    @Column(nullable = false, length = 4000)
    private String fontes;

    @Column(length = 40)
    private String motivoImagem;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ocorrencia_id", nullable = false, unique = true)
    private OcorrenciaModel ocorrencia;
}
