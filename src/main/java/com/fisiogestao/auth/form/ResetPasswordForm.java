package com.fisiogestao.auth.form;

import com.fisiogestao.auth.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordForm {

    private String token;

    @NotBlank(message = "Informe a nova senha.")
    @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH, message = PasswordPolicy.RANGE_MESSAGE)
    private String password;

    @NotBlank(message = "Confirme a nova senha.")
    private String confirmPassword;
}
