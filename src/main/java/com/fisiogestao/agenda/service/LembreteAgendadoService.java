package com.fisiogestao.agenda.service;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.config.ClinicTimezoneHolder;
import com.fisiogestao.email.EmailLogType;
import com.fisiogestao.email.service.EmailRequest;
import com.fisiogestao.email.service.SystemEmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LembreteAgendadoService {

    private static final Logger log = LoggerFactory.getLogger(LembreteAgendadoService.class);

    private final SessaoRepository sessaoRepository;
    private final SystemEmailService systemEmailService;
    private final ConfiguracaoClinicaRepository configuracaoClinicaRepository;

    /** Executa a cada hora, na marca cheia, para enviar lembretes de sessões nas próximas 24-25 horas. */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void enviarLembretes() {
        OffsetDateTime agora = ClinicTimezoneHolder.now();
        OffsetDateTime de  = agora.plusHours(24);
        OffsetDateTime ate = agora.plusHours(25);

        List<SessaoEntity> sessoes = sessaoRepository.findSessoesParaLembrete(de, ate);
        if (sessoes.isEmpty()) {
            return;
        }

        var clinicaOpt = configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc();
        String nomeClinica = clinicaOpt.map(c -> c.getNomeClinica()).orElse("Fisioterapia");
        ZoneId zone = clinicaOpt.map(c -> ZoneId.of(c.getTimezoneId())).orElse(ClinicTimezoneHolder.zoneId());

        for (SessaoEntity sessao : sessoes) {
            String email = sessao.getCliente().getEmail();
            if (email == null || email.isBlank() || !email.contains("@")) {
                continue;
            }
            try {
                systemEmailService.send(EmailRequest.builder()
                        .type(EmailLogType.LEMBRETE_SESSAO)
                        .subject("Lembrete: sua sessão é amanhã — " + nomeClinica)
                        .to(email)
                        .templateName("email/lembrete-sessao")
                        .templateId("email/lembrete-sessao")
                        .entityType("sessao")
                        .entityId(sessao.getId())
                        .triggeredBy("sistema")
                        .templateVariables(Map.of(
                                "nomePaciente", sessao.getCliente().getNomeCompleto(),
                                "data", sessao.getDataHoraInicio().atZoneSameInstant(zone)
                                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                "hora", sessao.getDataHoraInicio().atZoneSameInstant(zone)
                                        .format(DateTimeFormatter.ofPattern("HH:mm")),
                                "local", nomeClinica,
                                "profissional", sessao.getTipoAtendimento() != null
                                        && !sessao.getTipoAtendimento().isBlank()
                                        ? sessao.getTipoAtendimento() : "Equipe da clínica"
                        ))
                        .build());

                sessao.setLembreteEnviado(true);
                sessaoRepository.save(sessao);
            } catch (Exception ex) {
                log.error("Falha ao enviar lembrete para sessão {}: {}", sessao.getId(), ex.getMessage());
            }
        }

        log.info("Lembretes de sessão enviados: {} de {} encontradas.", sessoes.size(), sessoes.size());
    }
}
