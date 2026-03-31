package com.fisiogestao.relatorios.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnvioRelatorioForm {

    @NotBlank(message = "Informe o destinatário.")
    @Email(message = "Informe um e-mail válido.")
    private String destinatario;

    @NotNull(message = "Selecione o formato do anexo.")
    private FormatoRelatorio formato;

    @NotBlank(message = "Informe o assunto.")
    @Size(max = 220, message = "O assunto deve ter até 220 caracteres.")
    private String assunto;

    @Size(max = 2000, message = "A mensagem deve ter até 2000 caracteres.")
    private String mensagem;
}
