package br.com.valence.api.dtos;

import br.com.valence.api.enums.AcaoModeracao;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ModeracaoDto {

    @NotNull(message = "A decisão é obrigatória: MANTER ou REMOVER.")
    private AcaoModeracao decisao;
}
