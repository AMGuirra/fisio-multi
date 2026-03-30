package com.fisiogestao.agenda.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
public class AgendaManualForm {

    @NotNull(message = "Selecione o paciente.")
    private UUID clienteId;

    private UUID pacoteSessaoId;

    @NotNull(message = "Informe a data.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate data;

    @NotNull(message = "Informe o horário.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horario;

    @Size(max = 80)
    private String tipoAtendimento;

    @Size(max = 2000)
    private String observacoes;
}
