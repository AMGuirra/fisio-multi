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
