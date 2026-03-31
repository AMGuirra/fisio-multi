package com.fisiogestao.auth.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordForm {

    private String token;

    @NotBlank(message = "Informe a nova senha.")
    @Size(min = 4, max = 120, message = "A senha deve ter entre 4 e 120 caracteres.")
    private String password;

    @NotBlank(message = "Confirme a nova senha.")
    private String confirmPassword;
}
