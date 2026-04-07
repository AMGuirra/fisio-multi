package com.fisiogestao;

import com.fisiogestao.config.TimezoneConfig;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.TimeZone;

@SpringBootApplication
public class FisioGestaoApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(TimezoneConfig.CLINIC_TIMEZONE));
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