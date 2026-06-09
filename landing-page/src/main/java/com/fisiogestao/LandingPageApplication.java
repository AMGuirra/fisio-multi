package com.fisiogestao;

import com.fisiogestao.config.ClinicTimezoneHolder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LandingPageApplication {

    public static void main(String[] args) {
        ClinicTimezoneHolder.update(ClinicTimezoneHolder.DEFAULT_TIMEZONE_ID);
        SpringApplication.run(LandingPageApplication.class, args);
    }
}
