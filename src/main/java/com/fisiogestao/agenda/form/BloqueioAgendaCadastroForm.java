package com.fisiogestao.agenda.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class BloqueioAgendaCadastroForm {

    @NotNull(message = "Informe a data inicial.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicial;

    @NotNull(message = "Informe a data final.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFinal;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horaInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horaFim;

    private boolean bloqueioDiaInteiro;

    @NotBlank(message = "Informe o motivo do bloqueio.")
    @Size(max = 255, message = "O motivo deve ter até 255 caracteres.")
    private String motivo;

    @Size(max = 2000, message = "A observação deve ter até 2000 caracteres.")
    private String observacao;
}
