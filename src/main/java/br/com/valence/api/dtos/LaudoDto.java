package br.com.valence.api.dtos;

import br.com.valence.api.enums.DecisaoLaudo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LaudoDto {

    @NotNull(message = "A decisão é obrigatória: CONFIRMADA, DESCARTADA ou MAIS_INFORMACOES.")
    private DecisaoLaudo decisao;

    @Size(max = 150, message = "A praga confirmada deve ter até 150 caracteres.")
    private String pragaConfirmada;

    @NotBlank(message = "O parecer é obrigatório.")
    @Size(max = 2000, message = "O parecer deve ter até 2000 caracteres.")
    private String parecer;
}
