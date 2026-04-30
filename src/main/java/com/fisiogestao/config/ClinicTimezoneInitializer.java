package com.fisiogestao.config;

import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClinicTimezoneInitializer implements ApplicationRunner {

    private final ConfiguracaoClinicaRepository configuracaoClinicaRepository;

    @Override
    public void run(ApplicationArguments args) {
        configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc()
                .map(config -> config.getTimezoneId())
                .ifPresent(ClinicTimezoneHolder::update);
    }
}
