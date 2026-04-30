package com.fisiogestao;

import com.fisiogestao.config.ClinicTimezoneHolder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FisioGestaoApplication {

    public static void main(String[] args) {
        ClinicTimezoneHolder.update(ClinicTimezoneHolder.DEFAULT_TIMEZONE_ID);
        SpringApplication.run(FisioGestaoApplication.class, args);
    }

    @Bean
    public CommandLineRunner run() {
        return args -> {
            System.out.println("""

                    ========================================
                    🚀 FISIOGESTÃO INICIADO COM SUCESSO
                    ✅ OK, sistema funcionando!
                    ========================================

                    """);
        };
    }
}