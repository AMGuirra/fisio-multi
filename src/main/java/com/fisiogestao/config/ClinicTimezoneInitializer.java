package com.fisiogestao.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Carrega o fuso de cada clínica na inicialização (leitura técnica, sem dados de negócio). */
@Component
@Order(20)
@RequiredArgsConstructor
public class ClinicTimezoneInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.query(
                "SELECT empresa_id, timezone_id FROM configuracao_clinica WHERE ativo = TRUE ORDER BY created_at",
                rs -> {
                    ClinicTimezoneHolder.registrar(rs.getObject("empresa_id", UUID.class), rs.getString("timezone_id"));
                }
        );
    }
}
