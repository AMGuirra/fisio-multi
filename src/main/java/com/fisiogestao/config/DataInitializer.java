package com.fisiogestao.config;

import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataInitializer.class);

    private final ConfiguracaoClinicaRepository configuracaoClinicaRepository;

    @Override
    public void run(String... args) {
        if (configuracaoClinicaRepository.count() == 0) {
            ConfiguracaoClinicaEntity config = ConfiguracaoClinicaEntity.builder()
                    .nomeClinica("Clínica FisioGestao")
                    .timezoneId(TimezoneConfig.CLINIC_TIMEZONE)
                    .horarioAbertura(LocalTime.of(7, 0))
                    .horarioFechamento(LocalTime.of(19, 0))
                    .duracaoSessaoMinutos(60)
                    .horarioInicioAlmoco(LocalTime.of(12, 0))
                    .horarioFimAlmoco(LocalTime.of(13, 0))
                    .diasFuncionamento("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
                    .ativo(true)
                    .build();
            configuracaoClinicaRepository.save(config);
            LOGGER.info("Configuracao padrao da clinica criada com sucesso.");
        }
    }
}
