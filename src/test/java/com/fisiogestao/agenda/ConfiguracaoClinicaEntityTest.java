package com.fisiogestao.agenda;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfiguracaoClinicaEntityTest {

    @Test
    void deveAssumirDiasUteisQuandoDiasFuncionamentoNaoEstiverPreenchido() {
        ConfiguracaoClinicaEntity entity = new ConfiguracaoClinicaEntity();
        entity.setDiasFuncionamento("   ");

        Set<DayOfWeek> dias = entity.diasFuncionamentoSet();

        assertEquals(
                Set.of(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY
                ),
                dias
        );
    }

    @Test
    void deveRespeitarDiasConfiguradosExplicitamente() {
        ConfiguracaoClinicaEntity entity = new ConfiguracaoClinicaEntity();
        entity.setDiasFuncionamento("MONDAY,WEDNESDAY,FRIDAY");

        Set<DayOfWeek> dias = entity.diasFuncionamentoSet();

        assertEquals(
                Set.of(
                        DayOfWeek.MONDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.FRIDAY
                ),
                dias
        );
    }
}
