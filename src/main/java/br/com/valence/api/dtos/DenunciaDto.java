package br.com.valence.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DenunciaDto {

    @NotBlank(message = "O motivo da denúncia é obrigatório.")
    @Size(max = 500, message = "O motivo deve ter até 500 caracteres.")
    private String motivo;
}
