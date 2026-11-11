package br.com.valence.api.dtos.ia;

import br.com.valence.api.dtos.HipoteseDto;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TriagemIaDto(
        @JsonPropertyDescription("De 1 a 3 hipóteses compatíveis com a cultura, da mais provável para a menos provável")
        @NotEmpty @Size(max = 3) List<@Valid HipoteseDto> hipoteses,

        @JsonPropertyDescription("Severidade de 1 (leve) a 5 (muito grave)")
        @NotNull @Min(1) @Max(5) Integer severidade,

        @JsonPropertyDescription("Até 4 cuidados imediatos seguros, sem citar agrotóxico, produto ou dose")
        @NotNull @Size(max = 4) List<@NotBlank String> cuidadosImediatos,

        @JsonPropertyDescription("Frase de até 200 caracteres para avisar os vizinhos, sem dados pessoais")
        @NotBlank @Size(max = 200) String textoAlerta,

        @JsonPropertyDescription("true quando o caso precisa de avaliação prioritária do agrônomo")
        @NotNull Boolean encaminharAgronomo) {
}
