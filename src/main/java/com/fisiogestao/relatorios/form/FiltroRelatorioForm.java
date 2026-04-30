package com.fisiogestao.relatorios.form;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class FiltroRelatorioForm {

    @NotNull(message = "Informe a data inicial.")
    private LocalDate dataInicial;

    @NotNull(message = "Informe a data final.")
    private LocalDate dataFinal;

    private boolean incluirPacientes = true;
    private boolean incluirAgenda = true;
    private boolean incluirFinanceiro = true;
    private boolean incluirSolicitacoes = true;
}
