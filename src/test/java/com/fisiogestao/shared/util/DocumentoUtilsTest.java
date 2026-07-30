package com.fisiogestao.shared.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentoUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {"123.456.789-09", "12345678909", "123 456 789 09", "123/456/789-09"})
    void deveNormalizarCpfParaOsMesmosOnzeDigitos(String entrada) {
        assertEquals("12345678909", DocumentoUtils.normalizarCpf(entrada));
    }

    @Test
    void deveAceitarCpfValido() {
        assertTrue(DocumentoUtils.cpfValido("12345678909"));
    }

    @Test
    void deveRejeitarCpfComDigitosVerificadoresInvalidos() {
        assertFalse(DocumentoUtils.cpfValido("12345678900"));
    }

    @Test
    void deveRejeitarCpfComTodosDigitosIguais() {
        assertFalse(DocumentoUtils.cpfValido("11111111111"));
    }

    @Test
    void deveRejeitarCpfComTamanhoInvalido() {
        assertFalse(DocumentoUtils.cpfValido("123456789"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"(67) 99999-9999", "67999999999", "67 99999 9999", "67/99999-9999"})
    void deveNormalizarTelefoneParaOsMesmosDigitos(String entrada) {
        assertEquals("67999999999", DocumentoUtils.normalizarTelefone(entrada));
    }

    @Test
    void deveAceitarTelefoneComDezDigitos() {
        assertTrue(DocumentoUtils.telefoneTemTamanhoValido("6799999999"));
    }

    @Test
    void deveAceitarTelefoneComOnzeDigitos() {
        assertTrue(DocumentoUtils.telefoneTemTamanhoValido("67999999999"));
    }

    @Test
    void deveRejeitarTelefoneComTamanhoInvalido() {
        assertFalse(DocumentoUtils.telefoneTemTamanhoValido("999999"));
    }

    @Test
    void deveRetornarNuloParaEntradaVaziaOuNula() {
        assertNull(DocumentoUtils.normalizarCpf(null));
        assertNull(DocumentoUtils.normalizarCpf(""));
        assertNull(DocumentoUtils.normalizarCpf("   "));
        assertNull(DocumentoUtils.normalizarTelefone(null));
    }
}
