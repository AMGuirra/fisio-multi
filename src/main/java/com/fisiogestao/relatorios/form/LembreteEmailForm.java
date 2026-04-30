package com.fisiogestao.relatorios.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
public class LembreteEmailForm {

    private UUID pacienteId;
    @Size(max = 160, message = "O nome deve ter até 160 caracteres.")
    private String nomeDestinatario;

    @NotBlank(message = "Informe o e-mail do destinatário.")
    @Email(message = "Informe um e-mail válido.")
    private String emailDestinatario;

    @NotNull(message = "Selecione o tipo do lembrete.")
    private TipoLembrete tipo;

    private LocalDate dataReferencia;
    private LocalTime horaReferencia;

    @NotBlank(message = "Informe o assunto.")
    @Size(max = 220, message = "O assunto deve ter até 220 caracteres.")
    private String assunto;

    @Size(max = 2000, message = "A mensagem deve ter até 2000 caracteres.")
    private String mensagem;
}
