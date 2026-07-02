package com.fisiogestao.agenda.service;

import com.fisiogestao.agenda.DiaSemana;
import com.fisiogestao.agenda.Frequencia;
import com.fisiogestao.agenda.dto.RecorrenciaRequestDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecorrenciaServiceTest {

    private final RecorrenciaService recorrenciaService = new RecorrenciaService();

    @Test
    void deveGerarRecorrenciaSemanalSegQuaSex() {
        RecorrenciaRequestDto regra = new RecorrenciaRequestDto();
        regra.setFrequencia(Frequencia.SEMANAL);
        regra.setDiasSemana(List.of(DiaSemana.SEG, DiaSemana.QUA, DiaSemana.SEX));
        regra.setIntervalo(1);
        regra.setDataFim(LocalDate.of(2026, 5, 8));
        regra.setMaxOcorrencias(10);

        List<LocalDateTime> ocorrencias = recorrenciaService.gerarOcorrencias(
                LocalDateTime.of(2026, 5, 4, 9, 0),
                regra
        );

        assertEquals(List.of(
                LocalDateTime.of(2026, 5, 4, 9, 0),
                LocalDateTime.of(2026, 5, 6, 9, 0),
                LocalDateTime.of(2026, 5, 8, 9, 0)
        ), ocorrencias);
    }

    @Test
    void deveGerarRecorrenciaSemanalSegAQuiDentroDoMes() {
        RecorrenciaRequestDto regra = new RecorrenciaRequestDto();
        regra.setFrequencia(Frequencia.SEMANAL);
        regra.setDiasSemana(List.of(DiaSemana.SEG, DiaSemana.TER, DiaSemana.QUA, DiaSemana.QUI));
        regra.setIntervalo(1);
        regra.setDataFim(LocalDate.of(2026, 7, 31));
        regra.setMaxOcorrencias(20);

        List<LocalDateTime> ocorrencias = recorrenciaService.gerarOcorrencias(
                LocalDateTime.of(2026, 7, 2, 15, 0),
                regra
        );

        assertEquals(List.of(
                LocalDateTime.of(2026, 7, 2, 15, 0),
                LocalDateTime.of(2026, 7, 6, 15, 0),
                LocalDateTime.of(2026, 7, 7, 15, 0),
                LocalDateTime.of(2026, 7, 8, 15, 0),
                LocalDateTime.of(2026, 7, 9, 15, 0),
                LocalDateTime.of(2026, 7, 13, 15, 0),
                LocalDateTime.of(2026, 7, 14, 15, 0),
                LocalDateTime.of(2026, 7, 15, 15, 0),
                LocalDateTime.of(2026, 7, 16, 15, 0),
                LocalDateTime.of(2026, 7, 20, 15, 0),
                LocalDateTime.of(2026, 7, 21, 15, 0),
                LocalDateTime.of(2026, 7, 22, 15, 0),
                LocalDateTime.of(2026, 7, 23, 15, 0),
                LocalDateTime.of(2026, 7, 27, 15, 0),
                LocalDateTime.of(2026, 7, 28, 15, 0),
                LocalDateTime.of(2026, 7, 29, 15, 0),
                LocalDateTime.of(2026, 7, 30, 15, 0)
        ), ocorrencias);
    }

    @Test
    void deveLimitarEmCinquentaEDuasOcorrenciasQuandoNaoInformado() {
        RecorrenciaRequestDto regra = new RecorrenciaRequestDto();
        regra.setFrequencia(Frequencia.MENSAL);
        regra.setIntervalo(1);

        List<LocalDateTime> ocorrencias = recorrenciaService.gerarOcorrencias(
                LocalDateTime.of(2026, 1, 10, 14, 0),
                regra
        );

        assertEquals(52, ocorrencias.size());
    }
}
