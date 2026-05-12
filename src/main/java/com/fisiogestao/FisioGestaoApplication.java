package com.fisiogestao;

import com.fisiogestao.config.ClinicTimezoneHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FisioGestaoApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(FisioGestaoApplication.class);

    public static void main(String[] args) {
        ClinicTimezoneHolder.update(ClinicTimezoneHolder.DEFAULT_TIMEZONE_ID);
        SpringApplication.run(FisioGestaoApplication.class, args);
    }

    @Bean
    public CommandLineRunner run() {
        return args -> LOGGER.info("FisioGestao iniciado com sucesso.");
    }
}
