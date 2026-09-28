package com.fisiogestao.whatsapp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WhatsAppUrlBuilderTest {

    @Test
    void devePrefixarDdiBrasilEmNumeroNacionalComDdd() {
        assertEquals("5547999998888", WhatsAppUrlBuilder.normalizarNumero("(47) 99999-8888"));
        assertEquals("554733334444", WhatsAppUrlBuilder.normalizarNumero("47 3333-4444"));
    }

    @Test
    void devePrefixarDdiMesmoQuandoDddFor55() {
        // DDD 55 (Santa Maria/RS) não pode ser confundido com o DDI.
        assertEquals("5555999998888", WhatsAppUrlBuilder.normalizarNumero("(55) 99999-8888"));
    }

    @Test
    void deveManterNumeroQueJaPossuiDdi() {
        assertEquals("5547999998888", WhatsAppUrlBuilder.normalizarNumero("+55 47 99999-8888"));
    }

    @Test
    void deveRemoverZeroDeDiscagem() {
        assertEquals("5547999998888", WhatsAppUrlBuilder.normalizarNumero("047 99999-8888"));
        assertEquals("554733334444", WhatsAppUrlBuilder.normalizarNumero("047 3333-4444"));
    }

    @Test
    void deveDescartarCodigoDaOperadoraNaDiscagemNacional() {
        assertEquals("5547999998888", WhatsAppUrlBuilder.normalizarNumero("0 21 47 99999-8888"));
        assertEquals("554733334444", WhatsAppUrlBuilder.normalizarNumero("0 15 47 3333-4444"));
    }

    @Test
    void naoDeveInterpretarDiscagemInternacionalOuNacionalIncompleta() {
        assertNull(WhatsAppUrlBuilder.normalizarNumero("00 55 47 99999-8888"));
        assertNull(WhatsAppUrlBuilder.normalizarNumero("0 99999-8888"));
    }

    @Test
    void deveRetornarNullParaTelefoneAusenteOuInvalido() {
        assertNull(WhatsAppUrlBuilder.normalizarNumero(null));
        assertNull(WhatsAppUrlBuilder.normalizarNumero("   "));
        assertNull(WhatsAppUrlBuilder.normalizarNumero("99999-8888"));
        assertNull(WhatsAppUrlBuilder.montarUrl("123", "Olá"));
    }

    @Test
    void deveCodificarAcentosEspacosEQuebrasDeLinha() {
        String url = WhatsAppUrlBuilder.montarUrl("47999998888", "Olá João!\n1 - Sim");

        assertEquals("https://wa.me/5547999998888?text=Ol%C3%A1%20Jo%C3%A3o%21%0A1%20-%20Sim", url);
    }

    @Test
    void deveMontarUrlSemTextoQuandoMensagemVazia() {
        assertEquals("https://wa.me/5547999998888", WhatsAppUrlBuilder.montarUrl("47999998888", " "));
    }
}
