package com.fisiogestao.email.service;

import com.fisiogestao.shared.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SmtpEndpointValidatorTest {

    private final SmtpEndpointValidator validator = new SmtpEndpointValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "localhost", "127.0.0.1", "10.1.2.3", "192.168.1.10",
            "169.254.169.254", "100.64.1.2", "224.0.0.1", "::1", "fc00::1"
    })
    void deveBloquearDestinosInternosELoopback(String host) {
        assertThrows(BusinessException.class, () -> validator.validar(host, 587));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 25, 80, 2525, 65535})
    void deveBloquearPortasForaDe465E587(int port) {
        assertThrows(BusinessException.class, () -> validator.validar("203.0.113.10", port));
    }

    @Test
    void deveAceitarPortaSeguraEmEnderecoPublico() {
        assertEquals("203.0.113.10", validator.validar("203.0.113.10", 465));
    }

    @Test
    void deveRetornarMesmoEnderecoPublicoParaFixacaoDaConexao() {
        var endpoint = validator.validarEResolver("203.0.113.10", 587);

        assertEquals("203.0.113.10", endpoint.host());
        assertEquals("203.0.113.10", endpoint.pinnedAddress().getHostAddress());
    }
}
