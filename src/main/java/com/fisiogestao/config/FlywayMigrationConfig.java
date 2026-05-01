package com.fisiogestao.config;

import lombok.RequiredArgsConstructor;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(FlywayMigrationConfig.FlywayRepairProperties.class)
public class FlywayMigrationConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayMigrationConfig.class);

    private final FlywayRepairProperties properties;

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            if (properties.repairOnStartup()) {
                log.warn("Executando Flyway repair antes das migrations por configuracao app.flyway.repair-on-startup=true.");
                flyway.repair();
            }
            try {
                flyway.migrate();
            } catch (FlywayValidateException ex) {
                log.warn("Validacao do Flyway falhou. Executando repair automatico e tentando migrar novamente.", ex);
                flyway.repair();
                flyway.migrate();
            }
        };
    }

    @ConfigurationProperties(prefix = "app.flyway")
    public record FlywayRepairProperties(boolean repairOnStartup) {
    }
}
