package com.fisiogestao.relatorios.service;

import com.fisiogestao.config.ClinicTimezoneHolder;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class RelatorioFormatUtil {

    private static final Locale LOCALE_PT_BR = Locale.forLanguageTag("pt-BR");
    private static final String TONE_SUCCESS = "success";
    private static final String TONE_DANGER  = "danger";
    private static final String TONE_NEUTRAL  = "neutral";

    private RelatorioFormatUtil() {
    }

    static ZoneId clinicZone() {
        return ClinicTimezoneHolder.zoneId();
    }

    static LocalDate paraLocalDate(OffsetDateTime valor) {
        return valor.atZoneSameInstant(clinicZone()).toLocalDate();
    }

    static String formatDateTime(OffsetDateTime dataHora) {
        return dataHora.atZoneSameInstant(clinicZone())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    static String formatDateShort(LocalDate data) {
        return data.format(DateTimeFormatter.ofPattern("dd/MM"));
    }

    static String formatDateFull(LocalDate data) {
        return data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    static String construirLabelPeriodo(LocalDate inicio, LocalDate fim) {
        return formatDateFull(inicio) + " a " + formatDateFull(fim);
    }

    static List<LocalDate> construirListaDatas(LocalDate inicio, LocalDate fim) {
        List<LocalDate> datas = new ArrayList<>();
        LocalDate cursor = inicio;
        while (!cursor.isAfter(fim)) {
            datas.add(cursor);
            cursor = cursor.plusDays(1);
        }
        return datas;
    }

    static String formatCurrency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(LOCALE_PT_BR).format(value);
    }

    static String formatInteiro(long value) {
        return NumberFormat.getIntegerInstance(LOCALE_PT_BR).format(value);
    }

    static String formatNumero(double value, int escala) {
        NumberFormat format = NumberFormat.getNumberInstance(LOCALE_PT_BR);
        format.setMinimumFractionDigits(escala);
        format.setMaximumFractionDigits(escala);
        return format.format(value);
    }

    static String formatNumeroCru(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    static String formatPercent(double value) {
        return formatNumero(value, 1) + "%";
    }

    static String texto(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor.trim();
    }

    static String toneBySign(BigDecimal valor) {
        int comparacao = valor.compareTo(BigDecimal.ZERO);
        if (comparacao > 0) return TONE_SUCCESS;
        if (comparacao < 0) return TONE_DANGER;
        return TONE_NEUTRAL;
    }
}
