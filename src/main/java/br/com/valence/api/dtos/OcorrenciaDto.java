package br.com.valence.api.dtos;

import br.com.valence.api.enums.Cultura;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OcorrenciaDto {

    @NotNull(message = "A propriedade é obrigatória.")
    private Long propriedadeId;

    @NotNull(message = "A cultura é obrigatória: MILHO, TOMATE ou BATATA.")
    private Cultura cultura;

    @NotBlank(message = "A descrição é obrigatória.")
    @Size(max = 1000, message = "A descrição deve ter até 1000 caracteres.")
    private String descricao;
}
