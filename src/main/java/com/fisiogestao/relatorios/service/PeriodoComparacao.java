package com.fisiogestao.relatorios.service;

import java.time.LocalDate;
import java.util.List;

record PeriodoComparacao(
        LocalDate atualInicio,
        LocalDate atualFimInclusive,
        LocalDate atualFimExclusivo,
        LocalDate anteriorInicio,
        LocalDate anteriorFimInclusive,
        LocalDate anteriorFimExclusivo,
        String labelAtual,
        String labelAnterior,
        long totalDias,
        List<LocalDate> datas
) {
}
