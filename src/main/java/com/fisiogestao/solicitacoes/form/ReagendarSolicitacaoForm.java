package com.fisiogestao.solicitacoes.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class ReagendarSolicitacaoForm {

    @NotNull(message = "Informe a nova data.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate data;

    @NotNull(message = "Informe o novo horário.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horario;

    @Size(max = 255)
    private String motivo;
}
