package com.fisiogestao.email.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TesteEmailForm {

    @NotBlank(message = "Informe o e-mail de destino para o teste.")
    @Email(message = "Informe um e-mail válido.")
    private String destinatario;
}
