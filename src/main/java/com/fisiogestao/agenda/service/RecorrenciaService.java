package com.fisiogestao.agenda.service;

import com.fisiogestao.agenda.DiaSemana;
import com.fisiogestao.agenda.dto.RecorrenciaRequestDto;
import com.fisiogestao.shared.web.BusinessException;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RecorrenciaService {

    private static final int LIMITE_PADRAO_OCORRENCIAS = 52;
    private static final int LIMITE_MAXIMO_OCORRENCIAS = 365;

    public List<LocalDateTime> gerarOcorrencias(LocalDateTime inicio, RecorrenciaRequestDto regra) {
        if (inicio == null) {
            throw new BusinessException("Informe a data e hora inicial da recorrência.");
        }
        if (regra == null || regra.getFrequencia() == null) {
            throw new BusinessException("Informe a regra de recorrência.");
        }

        int intervalo = regra.getIntervalo() == null ? 1 : regra.getIntervalo();
        if (intervalo < 1) {
            throw new BusinessException("O intervalo da recorrência deve ser maior que zero.");
        }

        int limite = regra.getMaxOcorrencias() == null ? LIMITE_PADRAO_OCORRENCIAS : regra.getMaxOcorrencias();
        if (limite < 1) {
            throw new BusinessException("A quantidade máxima de ocorrências deve ser maior que zero.");
        }
        limite = Math.min(limite, LIMITE_MAXIMO_OCORRENCIAS);

        if (regra.getDataFim() != null && regra.getDataFim().isBefore(inicio.toLocalDate())) {
            throw new BusinessException("A data final da recorrência não pode ser anterior à primeira ocorrência.");
        }

        return switch (regra.getFrequencia()) {
            case SEMANAL -> gerarSemanais(inicio, regra, intervalo, limite);
            case MENSAL -> gerarPorPeriodo(inicio, regra.getDataFim(), limite, cursor -> cursor.plusMonths(intervalo));
            case ANUAL -> gerarPorPeriodo(inicio, regra.getDataFim(), limite, cursor -> cursor.plusYears(intervalo));
        };
    }

    private List<LocalDateTime> gerarSemanais(
            LocalDateTime inicio,
            RecorrenciaRequestDto regra,
            int intervalo,
            int limite
    ) {
        Set<DayOfWeek> diasSelecionados = diasSelecionados(regra, inicio.toLocalDate().getDayOfWeek());
        LinkedHashSet<LocalDateTime> ocorrencias = new LinkedHashSet<>();
        LocalDate semanaBase = inicio.toLocalDate();
        LocalDate limiteData = regra.getDataFim();

        int semanaOffset = 0;
        while (ocorrencias.size() < limite) {
            LocalDate inicioSemana = semanaBase.plusWeeks((long) semanaOffset * intervalo)
                    .minusDays(semanaBase.getDayOfWeek().getValue() - 1L);

            boolean ultrapassouFim = false;
            for (DayOfWeek dia : diasSelecionados) {
                LocalDate data = inicioSemana.plusDays(dia.getValue() - 1L);
                if (data.isBefore(inicio.toLocalDate())) {
                    continue;
                }
                if (limiteData != null && data.isAfter(limiteData)) {
                    ultrapassouFim = true;
                    continue;
                }
                ocorrencias.add(data.atTime(inicio.toLocalTime()));
                if (ocorrencias.size() >= limite) {
                    break;
                }
            }

            if (limiteData != null && ultrapassouFim && inicioSemana.isAfter(limiteData)) {
                break;
            }
            if (limiteData != null && inicioSemana.plusWeeks(intervalo).isAfter(limiteData.plusDays(6))) {
                break;
            }
            semanaOffset++;
        }

        return ocorrencias.stream()
                .sorted()
                .limit(limite)
                .toList();
    }

    private List<LocalDateTime> gerarPorPeriodo(
            LocalDateTime inicio,
            LocalDate dataFim,
            int limite,
            java.util.function.UnaryOperator<LocalDateTime> proximo
    ) {
        LinkedHashSet<LocalDateTime> ocorrencias = new LinkedHashSet<>();
        LocalDateTime cursor = inicio;
        while (ocorrencias.size() < limite) {
            if (dataFim != null && cursor.toLocalDate().isAfter(dataFim)) {
                break;
            }
            ocorrencias.add(cursor);
            cursor = proximo.apply(cursor);
        }
        return List.copyOf(ocorrencias);
    }

    private Set<DayOfWeek> diasSelecionados(RecorrenciaRequestDto regra, DayOfWeek fallback) {
        List<DiaSemana> dias = regra.getDiasSemana();
        if (dias == null || dias.isEmpty()) {
            return Set.of(fallback);
        }
        return dias.stream()
                .map(DiaSemana::toDayOfWeek)
                .sorted(Comparator.comparingInt(DayOfWeek::getValue))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
