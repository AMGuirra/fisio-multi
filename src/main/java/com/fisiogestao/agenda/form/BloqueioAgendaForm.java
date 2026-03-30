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
public class BloqueioAgendaForm {

    @NotNull(message = "Informe a data do bloqueio.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate data;

    @NotNull(message = "Informe a hora inicial.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horaInicio;

    @NotNull(message = "Informe a hora final.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horaFim;

    @NotBlank(message = "Descreva o motivo do bloqueio.")
    @Size(max = 255)
    private String motivo;
}
