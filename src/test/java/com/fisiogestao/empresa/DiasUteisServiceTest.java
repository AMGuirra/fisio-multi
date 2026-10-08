package com.fisiogestao.empresa;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Requisito 14: o teste conta apenas dias úteis (sem fins de semana e feriados nacionais). */
class DiasUteisServiceTest {

    private final DiasUteisService service = new DiasUteisService();

    @Test
    void sabadoEDomingoNaoSaoDiasUteis() {
        assertFalse(service.isDiaUtil(LocalDate.of(2026, 10, 10))); // sábado
        assertFalse(service.isDiaUtil(LocalDate.of(2026, 10, 11))); // domingo
        assertTrue(service.isDiaUtil(LocalDate.of(2026, 10, 13)));  // terça
    }

    @Test
    void feriadosNacionaisFixosEMoveisNaoSaoDiasUteis() {
        assertFalse(service.isDiaUtil(LocalDate.of(2026, 1, 1)));   // Confraternização (quinta)
        assertFalse(service.isDiaUtil(LocalDate.of(2026, 4, 3)));   // Sexta-feira Santa 2026
        assertFalse(service.isDiaUtil(LocalDate.of(2026, 4, 21)));  // Tiradentes (terça)
        assertFalse(service.isDiaUtil(LocalDate.of(2026, 9, 7)));   // Independência (segunda)
        assertFalse(service.isDiaUtil(LocalDate.of(2026, 10, 12))); // Aparecida (segunda)
        assertFalse(service.isDiaUtil(LocalDate.of(2026, 11, 2)));  // Finados (segunda)
        assertFalse(service.isDiaUtil(LocalDate.of(2026, 11, 20))); // Consciência Negra (sexta)
        assertFalse(service.isDiaUtil(LocalDate.of(2026, 12, 25))); // Natal (sexta)
        assertEquals(LocalDate.of(2025, 4, 20), DiasUteisService.domingoDePascoa(2025));
        assertEquals(LocalDate.of(2027, 3, 28), DiasUteisService.domingoDePascoa(2027));
    }

    @Test
    void consciencianegraSoEhFeriadoNacionalAPartirDe2024() {
        assertTrue(service.isFeriadoNacional(LocalDate.of(2024, 11, 20)));
        assertFalse(service.isFeriadoNacional(LocalDate.of(2023, 11, 20)));
    }

    @Test
    void calculaDataFinalPulandoFimDeSemanaEFeriado() {
        // Quinta 08/10/2026: 08(1), 09(2), [10 e 11 fim de semana], [12 feriado], 13(3)
        assertEquals(LocalDate.of(2026, 10, 13), service.calcularDataFinal(LocalDate.of(2026, 10, 8), 3));
        // Começando num sábado, o primeiro dia útil é a terça (segunda é feriado).
        assertEquals(LocalDate.of(2026, 10, 13), service.calcularDataFinal(LocalDate.of(2026, 10, 10), 1));
        // A partir de 01/12/2026 (terça): 15 dias úteis terminam em 21/12; 20 pulam o Natal (25/12).
        assertEquals(LocalDate.of(2026, 12, 21), service.calcularDataFinal(LocalDate.of(2026, 12, 1), 15));
        assertEquals(LocalDate.of(2026, 12, 29), service.calcularDataFinal(LocalDate.of(2026, 12, 1), 20));
    }

    @Test
    void contaDiasUteisNoIntervalo() {
        assertEquals(3, service.contarDiasUteis(LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 13)));
        assertEquals(0, service.contarDiasUteis(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12)));
        assertEquals(1, service.diasUteisRestantes(LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 13)));
    }

    @Test
    void quantidadeInvalidaEhRecusada() {
        assertThrows(IllegalArgumentException.class, () -> service.calcularDataFinal(LocalDate.now(), 0));
    }
}
