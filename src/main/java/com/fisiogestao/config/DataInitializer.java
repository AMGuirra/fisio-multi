package com.fisiogestao.config;

import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ConfiguracaoClinicaRepository configuracaoClinicaRepository;

    @Override
    public void run(String... args) {
        if (configuracaoClinicaRepository.count() == 0) {
            ConfiguracaoClinicaEntity config = ConfiguracaoClinicaEntity.builder()
                    .nomeClinica("Clínica FisioGestao")
                    .timezoneId("America/Campo_Grande")
                    .horarioAbertura(LocalTime.of(7, 0))
                    .horarioFechamento(LocalTime.of(19, 0))
                    .duracaoSessaoMinutos(60)
                    .horarioInicioAlmoco(LocalTime.of(12, 0))
                    .horarioFimAlmoco(LocalTime.of(13, 0))
                    .diasFuncionamento("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
                    .ativo(true)
                    .build();
            configuracaoClinicaRepository.save(config);
            System.out.println("Configuração da clínica criada com sucesso!");
        }
    }
}
