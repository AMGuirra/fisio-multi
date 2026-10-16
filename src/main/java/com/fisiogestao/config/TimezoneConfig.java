package com.fisiogestao.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimezoneConfig {

    public static final String DEFAULT_TIMEZONE_ID = ClinicTimezoneHolder.DEFAULT_TIMEZONE_ID;

    @PostConstruct
    void configureDefaultTimezone() {
        ClinicTimezoneHolder.update(DEFAULT_TIMEZONE_ID);
    }

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
