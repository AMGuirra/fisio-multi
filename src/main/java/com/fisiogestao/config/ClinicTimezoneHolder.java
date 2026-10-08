package com.fisiogestao.config;

import com.fisiogestao.tenant.TenantContext;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Fuso horário efetivo da operação corrente. Cada clínica tem o seu (configurado em
 * Config. Clínica); fora de uma clínica, vale o fuso padrão da aplicação.
 */
public final class ClinicTimezoneHolder {

    public static final String DEFAULT_TIMEZONE_ID = "America/Campo_Grande";

    private static final AtomicReference<ZoneId> DEFAULT_ZONE = new AtomicReference<>(ZoneId.of(DEFAULT_TIMEZONE_ID));
    private static final Map<UUID, ZoneId> ZONAS_POR_EMPRESA = new ConcurrentHashMap<>();

    private ClinicTimezoneHolder() {
    }

    public static ZoneId zoneId() {
        UUID empresaId = TenantContext.empresaIdOuNull();
        if (empresaId != null) {
            ZoneId zona = ZONAS_POR_EMPRESA.get(empresaId);
            if (zona != null) {
                return zona;
            }
        }
        return DEFAULT_ZONE.get();
    }

    public static OffsetDateTime now() {
        return OffsetDateTime.now(zoneId());
    }

    public static LocalDate today() {
        return LocalDate.now(zoneId());
    }

    /** Define o fuso padrão da aplicação (usado na inicialização e fora de clínicas). */
    public static void update(String timezoneId) {
        ZoneId zoneId = parse(timezoneId);
        DEFAULT_ZONE.set(zoneId);
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
    }

    /** Registra o fuso de uma clínica sem afetar as demais. */
    public static void registrar(UUID empresaId, String timezoneId) {
        if (empresaId == null) {
            return;
        }
        ZONAS_POR_EMPRESA.put(empresaId, parse(timezoneId));
    }

    private static ZoneId parse(String timezoneId) {
        String effectiveTimezone = timezoneId == null || timezoneId.isBlank()
                ? DEFAULT_TIMEZONE_ID
                : timezoneId.trim();
        return ZoneId.of(effectiveTimezone);
    }
}
