package com.fisiogestao.auth.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordForm {

    @NotBlank(message = "Informe o e-mail cadastrado.")
    @Email(message = "Informe um e-mail válido.")
    private String email;

    @Size(max = 80, message = "Código da clínica inválido.")
    private String clinica;
}
