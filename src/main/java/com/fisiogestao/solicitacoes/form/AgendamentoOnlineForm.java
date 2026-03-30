package com.fisiogestao.solicitacoes.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class AgendamentoOnlineForm {

    @NotNull(message = "Selecione a data.")
    @FutureOrPresent(message = "Escolha uma data atual ou futura.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate data;

    @NotBlank(message = "Selecione um horário.")
    private String horario;

    @NotBlank(message = "Informe o nome do paciente.")
    @Size(max = 160)
    private String nomePaciente;

    @NotBlank(message = "Informe o telefone/WhatsApp.")
    @Size(max = 20)
    private String telefone;

    @Email(message = "Informe um e-mail válido.")
    @Size(max = 120)
    private String email;

    @Size(max = 2000)
    private String observacoes;
}
