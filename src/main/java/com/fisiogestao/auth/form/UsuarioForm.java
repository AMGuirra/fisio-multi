package com.fisiogestao.auth.form;

import com.fisiogestao.auth.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioForm {

    @NotBlank(message = "Informe o nome de usuário.")
    @Size(max = 40, message = "O nome de usuário deve ter até 40 caracteres.")
    private String username;

    @NotBlank(message = "Informe o e-mail.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 160, message = "O e-mail deve ter até 160 caracteres.")
    private String email;

    @Size(max = 120, message = "A senha deve ter no máximo 120 caracteres.")
    private String password;

    @NotNull(message = "Selecione o tipo de usuário.")
    private UserRole role;
}
