package com.fisiogestao.config;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

public final class ClinicTimezoneHolder {

    public static final String DEFAULT_TIMEZONE_ID = "America/Campo_Grande";

    private static final AtomicReference<ZoneId> CURRENT_ZONE = new AtomicReference<>(ZoneId.of(DEFAULT_TIMEZONE_ID));

    private ClinicTimezoneHolder() {
    }

    public static ZoneId zoneId() {
        return CURRENT_ZONE.get();
    }

    public static OffsetDateTime now() {
        return OffsetDateTime.now(zoneId());
    }

    public static LocalDate today() {
        return LocalDate.now(zoneId());
    }

    public static void update(String timezoneId) {
        String effectiveTimezone = timezoneId == null || timezoneId.isBlank()
                ? DEFAULT_TIMEZONE_ID
                : timezoneId.trim();
        ZoneId zoneId = ZoneId.of(effectiveTimezone);
        CURRENT_ZONE.set(zoneId);
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
    }
}
