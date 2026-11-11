package br.com.valence.api.dtos;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HipoteseDto(
        @JsonPropertyDescription("Nome popular da praga ou doença")
        @NotBlank String praga,
        @JsonPropertyDescription("Probabilidade entre 0 e 1")
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double probabilidade) {
}
