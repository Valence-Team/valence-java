package br.com.valence.api.dtos;

import br.com.valence.api.enums.Cultura;
import br.com.valence.api.utils.Validacoes;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class PropriedadeDto {

    @NotBlank(message = "O nome da propriedade é obrigatório.")
    @Size(max = 100, message = "O nome deve ter até 100 caracteres.")
    private String nome;

    @NotBlank(message = "O município é obrigatório.")
    @Size(max = 100, message = "O município deve ter até 100 caracteres.")
    private String municipio;

    @NotBlank(message = "A UF é obrigatória.")
    @Pattern(regexp = Validacoes.UF_REGEX, flags = Pattern.Flag.CASE_INSENSITIVE, message = "Informe uma UF válida, como SP.")
    private String uf;

    @NotNull(message = "A latitude é obrigatória.")
    @DecimalMin(value = "-90.0", message = "A latitude deve estar entre -90 e 90.")
    @DecimalMax(value = "90.0", message = "A latitude deve estar entre -90 e 90.")
    private Double latitude;

    @NotNull(message = "A longitude é obrigatória.")
    @DecimalMin(value = "-180.0", message = "A longitude deve estar entre -180 e 180.")
    @DecimalMax(value = "180.0", message = "A longitude deve estar entre -180 e 180.")
    private Double longitude;

    @NotNull(message = "A área em hectares é obrigatória.")
    @Positive(message = "A área deve ser maior que zero.")
    @DecimalMax(value = "1000000.0", message = "A área deve ter até 1.000.000 de hectares.")
    private Double areaHectares;

    @NotEmpty(message = "Informe ao menos uma cultura: MILHO, TOMATE ou BATATA.")
    private Set<Cultura> culturas;
}
