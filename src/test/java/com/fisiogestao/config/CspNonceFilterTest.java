package com.fisiogestao.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class CspNonceFilterTest {

    @Test
    void deveGerarNonceEProibirUnsafeInlineParaScripts() throws Exception {
        var request = new MockHttpServletRequest("GET", "/login");
        var response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        new CspNonceFilter().doFilterInternal(request, response, chain);

        String nonce = (String) request.getAttribute(CspNonceFilter.REQUEST_ATTRIBUTE);
        String policy = response.getHeader("Content-Security-Policy");
        assertNotNull(nonce);
        assertTrue(nonce.length() >= 20);
        assertTrue(policy.contains("script-src 'self' 'nonce-" + nonce + "'"));
        assertFalse(policy.substring(policy.indexOf("script-src"), policy.indexOf("style-src"))
                .contains("'unsafe-inline'"));
    }

    /**
     * Trava a diretiva {@code img-src} exatamente em {@code 'self' data: blob:} e confere,
     * diretiva a diretiva, que nada mais na CSP foi ampliado por essa mudança (regressão que
     * mudanças futuras em img-src poderiam introduzir sem serem notadas nos outros dois testes).
     */
    @Test
    void devePermitirBlobSomenteEmImgSrcSemAlargarOutrasDiretivas() throws Exception {
        var request = new MockHttpServletRequest("GET", "/prontuario");
        var response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        new CspNonceFilter().doFilterInternal(request, response, chain);

        String nonce = (String) request.getAttribute(CspNonceFilter.REQUEST_ATTRIBUTE);
        Map<String, String> diretivas = parsearDiretivas(response.getHeader("Content-Security-Policy"));

        assertEquals("'self' data: blob:", diretivas.get("img-src"));

        assertEquals("'self'", diretivas.get("default-src"));
        assertEquals("'self' 'nonce-" + nonce + "'", diretivas.get("script-src"));
        assertEquals("'self' 'unsafe-inline' https://fonts.googleapis.com", diretivas.get("style-src"));
        assertEquals("'self' https://fonts.gstatic.com", diretivas.get("font-src"));
        assertEquals(
                "'self' https://servicodados.ibge.gov.br https://viacep.com.br",
                diretivas.get("connect-src")
        );
        assertEquals("'none'", diretivas.get("object-src"));
        assertEquals("'self'", diretivas.get("base-uri"));
        assertEquals("'self'", diretivas.get("frame-ancestors"));
        assertEquals("'self'", diretivas.get("form-action"));

        assertFalse(diretivas.get("script-src").contains("blob:"), "blob: nao pode vazar para script-src");
        assertFalse(diretivas.get("default-src").contains("blob:"), "blob: nao pode vazar para default-src");
    }

    private Map<String, String> parsearDiretivas(String policy) {
        Map<String, String> diretivas = new LinkedHashMap<>();
        for (String parte : policy.split(";\\s*")) {
            String trecho = parte.trim();
            if (trecho.isEmpty()) {
                continue;
            }
            int separador = trecho.indexOf(' ');
            String nome = separador < 0 ? trecho : trecho.substring(0, separador);
            String valor = separador < 0 ? "" : trecho.substring(separador + 1);
            diretivas.put(nome, valor);
        }
        return diretivas;
    }
}
