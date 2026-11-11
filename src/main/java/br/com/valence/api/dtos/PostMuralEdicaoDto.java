package br.com.valence.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostMuralEdicaoDto {

    @NotBlank(message = "O texto é obrigatório.")
    @Size(max = 1000, message = "O texto deve ter até 1000 caracteres.")
    private String texto;
}
