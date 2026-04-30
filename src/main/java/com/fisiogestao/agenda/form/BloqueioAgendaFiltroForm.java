package com.fisiogestao.agenda.form;

import com.fisiogestao.agenda.BloqueioAgendaStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class BloqueioAgendaFiltroForm {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicial;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFinal;

    private String motivo;

    private BloqueioAgendaStatus status;

    private String tipo;

    private String periodo = "FUTUROS";

    private boolean apenasAtivos = true;
}
