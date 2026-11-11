package br.com.valence.api.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "TB_EVENTO_PROCESSADO")
public class EventoProcessadoModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private UUID eventoId;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(nullable = false)
    private Instant processadoEm;
}
