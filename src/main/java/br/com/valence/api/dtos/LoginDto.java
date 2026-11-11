package br.com.valence.api.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginDto {

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 150, message = "O e-mail deve ter até 150 caracteres.")
    private String email;

    @NotBlank(message = "A senha é obrigatória.")
    @Size(max = 100, message = "A senha deve ter até 100 caracteres.")
    private String senha;
}
