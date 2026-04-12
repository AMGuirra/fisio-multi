package com.fisiogestao.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimezoneConfig {

    public static final String DEFAULT_TIMEZONE_ID = ClinicTimezoneHolder.DEFAULT_TIMEZONE_ID;

    @PostConstruct
    void configureDefaultTimezone() {
        ClinicTimezoneHolder.update(DEFAULT_TIMEZONE_ID);
    }
}
