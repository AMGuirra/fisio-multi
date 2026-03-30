package com.fisiogestao.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class TimezoneConfig {

    public static final String CLINIC_TIMEZONE = "America/Campo_Grande";

    @PostConstruct
    void configureDefaultTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone(CLINIC_TIMEZONE));
    }
}
