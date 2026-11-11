package br.com.valence.api.dtos;

import br.com.valence.api.utils.Validacoes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostMuralDto {

    @NotBlank(message = "O texto é obrigatório.")
    @Size(max = 1000, message = "O texto deve ter até 1000 caracteres.")
    private String texto;

    @NotBlank(message = "O município é obrigatório.")
    @Size(max = 100, message = "O município deve ter até 100 caracteres.")
    private String municipio;

    @NotBlank(message = "A UF é obrigatória.")
    @Pattern(regexp = Validacoes.UF_REGEX, flags = Pattern.Flag.CASE_INSENSITIVE, message = "Informe uma UF válida, como SP.")
    private String uf;
}
