package com.fisiogestao.relatorios.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CobrancaEmailForm {

    @NotNull(message = "Selecione o lançamento financeiro.")
    private UUID lancamentoId;

    @NotBlank(message = "Informe o e-mail do destinatário.")
    @Email(message = "Informe um e-mail válido.")
    private String emailDestinatario;

    @NotBlank(message = "Informe o assunto.")
    @Size(max = 220, message = "O assunto deve ter até 220 caracteres.")
    private String assunto;

    @Size(max = 2000, message = "A mensagem deve ter até 2000 caracteres.")
    private String mensagem;
}
