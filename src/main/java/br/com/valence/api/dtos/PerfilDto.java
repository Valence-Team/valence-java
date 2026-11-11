package br.com.valence.api.dtos;

import br.com.valence.api.utils.Validacoes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PerfilDto {

    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 100, message = "O nome deve ter até 100 caracteres.")
    private String nome;

    @Size(max = 20, message = "O telefone deve ter até 20 caracteres.")
    @Pattern(regexp = Validacoes.TELEFONE_REGEX, message = "O telefone aceita só números, espaços, parênteses, + e -.")
    private String telefone;

    @NotBlank(message = "O município é obrigatório.")
    @Size(max = 100, message = "O município deve ter até 100 caracteres.")
    private String municipio;

    @NotBlank(message = "A UF é obrigatória.")
    @Pattern(regexp = Validacoes.UF_REGEX, flags = Pattern.Flag.CASE_INSENSITIVE, message = "Informe uma UF válida, como SP.")
    private String uf;
}
