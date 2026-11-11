package br.com.valence.api.dtos.ia;

import br.com.valence.api.enums.AcaoModeracao;
import br.com.valence.api.enums.CategoriaDenuncia;
import br.com.valence.api.enums.GravidadeDenuncia;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnaliseDenunciaIaDto(
        @JsonPropertyDescription("Categoria do problema encontrado no post")
        @NotNull CategoriaDenuncia categoria,

        @JsonPropertyDescription("Gravidade do problema: BAIXA, MEDIA ou ALTA")
        @NotNull GravidadeDenuncia gravidade,

        @JsonPropertyDescription("MANTER ou OCULTAR_E_REVISAR. Nunca REMOVER: a remoção é sempre humana")
        @NotNull AcaoModeracao acao,

        @JsonPropertyDescription("Justificativa curta, em português, sem repetir dados pessoais")
        @NotBlank @Size(max = 500) String justificativa) {
}
