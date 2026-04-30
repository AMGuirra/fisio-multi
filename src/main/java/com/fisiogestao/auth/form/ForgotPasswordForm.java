package com.fisiogestao.auth.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordForm {

    @NotBlank(message = "Informe o e-mail cadastrado.")
    @Email(message = "Informe um e-mail válido.")
    private String email;
}
