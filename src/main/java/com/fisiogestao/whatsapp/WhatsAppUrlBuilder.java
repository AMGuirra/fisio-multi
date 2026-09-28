package com.fisiogestao.whatsapp;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Monta links nativos do WhatsApp (wa.me) sem depender de API externa.
 * O envio acontece no navegador/dispositivo de quem clica no link.
 */
public final class WhatsAppUrlBuilder {

    private static final String BASE_URL = "https://wa.me/";
    private static final String DDI_BRASIL = "55";

    private WhatsAppUrlBuilder() {
    }

    /**
     * Normaliza o telefone para o formato internacional aceito pelo wa.me (apenas dígitos, com DDI).
     * Números nacionais com DDD (10 ou 11 dígitos) recebem o DDI 55. Números começando com 0 seguem a
     * discagem nacional (0 + DDD ou 0 + operadora + DDD) e também recebem o DDI 55. Números com 12 a 15
     * dígitos sem o 0 inicial são considerados já com DDI. Qualquer outro formato é tratado como inválido.
     *
     * @return o número normalizado ou {@code null} quando ausente/inválido
     */
    public static String normalizarNumero(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            return null;
        }
        String digitos = telefone.replaceAll("[^0-9]", "");
        if (digitos.startsWith("0")) {
            return normalizarDiscagemNacional(digitos);
        }
        if (digitos.length() == 10 || digitos.length() == 11) {
            return DDI_BRASIL + digitos;
        }
        if (digitos.length() >= 12 && digitos.length() <= 15) {
            return digitos;
        }
        return null;
    }

    /**
     * Discagem nacional: "0 + DDD + número" (ex.: 047 99999-8888) ou "0 + operadora + DDD + número"
     * (ex.: 0 21 47 99999-8888). O código da operadora é descartado. Discagem internacional
     * ("00...") não é interpretada, para não gerar link para um número errado.
     */
    private static String normalizarDiscagemNacional(String digitos) {
        if (digitos.startsWith("00")) {
            return null;
        }
        String semZero = digitos.substring(1);
        if (semZero.length() == 12 || semZero.length() == 13) {
            semZero = semZero.substring(2);
        }
        if (semZero.length() == 10 || semZero.length() == 11) {
            return DDI_BRASIL + semZero;
        }
        return null;
    }

    /**
     * @return a URL pronta ({@code https://wa.me/55XXXXXXXXXXX?text=...}) ou {@code null}
     * quando o telefone estiver ausente ou inválido
     */
    public static String montarUrl(String telefone, String mensagem) {
        String numero = normalizarNumero(telefone);
        if (numero == null) {
            return null;
        }
        if (mensagem == null || mensagem.isBlank()) {
            return BASE_URL + numero;
        }
        // URLEncoder usa "+" para espaço; alguns clientes do WhatsApp exibem o "+" literal, então usamos %20.
        String texto = URLEncoder.encode(mensagem, StandardCharsets.UTF_8).replace("+", "%20");
        return BASE_URL + numero + "?text=" + texto;
    }
}
