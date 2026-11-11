package br.com.valence.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ComentarioDto {

    @NotBlank(message = "O texto do comentário é obrigatório.")
    @Size(max = 500, message = "O comentário deve ter até 500 caracteres.")
    private String texto;
}
