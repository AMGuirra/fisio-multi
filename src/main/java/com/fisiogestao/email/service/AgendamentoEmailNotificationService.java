package com.fisiogestao.email.service;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.email.EmailLogType;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgendamentoEmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AgendamentoEmailNotificationService.class);

    private final SystemEmailService systemEmailService;
    private final ConfiguracaoClinicaRepository configuracaoClinicaRepository;

    public void notificarAguardandoConfirmacao(SolicitacaoAgendamentoEntity solicitacao) {
        if (!possuiEmail(solicitacao.getEmail())) {
            return;
        }
        enviar(
                solicitacao.getEmail(),
                EmailLogType.AGENDAMENTO_AGUARDANDO_CONFIRMACAO,
                "Recebemos sua solicitação de agendamento",
                "email/agendamento-aguardando-confirmacao",
                solicitacao.getId(),
                solicitacao.getNomePaciente(),
                solicitacao.getDataHoraInicio(),
                solicitacao.getObservacoes(),
                "Equipe da clínica",
                null,
                null
        );
    }

    public void notificarConfirmadoPorSolicitacao(SolicitacaoAgendamentoEntity solicitacao) {
        if (!possuiEmail(solicitacao.getEmail())) {
            return;
        }
        enviar(
                solicitacao.getEmail(),
                EmailLogType.AGENDAMENTO_CONFIRMADO,
                "Seu agendamento foi confirmado",
                "email/agendamento-confirmado",
                solicitacao.getId(),
                solicitacao.getNomePaciente(),
                solicitacao.getDataHoraInicio(),
                solicitacao.getObservacoes(),
                "Equipe da clínica",
                null,
                null
        );
    }

    public void notificarCancelado(SolicitacaoAgendamentoEntity solicitacao) {
        if (!possuiEmail(solicitacao.getEmail())) {
            return;
        }
        enviar(
                solicitacao.getEmail(),
                EmailLogType.AGENDAMENTO_CANCELADO,
                "Seu agendamento foi cancelado",
                "email/agendamento-cancelado",
                solicitacao.getId(),
                solicitacao.getNomePaciente(),
                solicitacao.getDataHoraInicio(),
                solicitacao.getObservacoes(),
                "Equipe da clínica",
                null,
                null
        );
    }

    public void notificarRecusado(SolicitacaoAgendamentoEntity solicitacao) {
        if (!possuiEmail(solicitacao.getEmail())) {
            return;
        }
        enviar(
                solicitacao.getEmail(),
                EmailLogType.AGENDAMENTO_RECUSADO,
                "Não foi possível confirmar seu agendamento",
                "email/agendamento-recusado",
                solicitacao.getId(),
                solicitacao.getNomePaciente(),
                solicitacao.getDataHoraInicio(),
                solicitacao.getObservacoes(),
                "Equipe da clínica",
                null,
                null
        );
    }

    public void notificarConfirmadoPorSessao(SessaoEntity sessao, String profissional) {
        if (!possuiEmail(sessao.getCliente().getEmail())) {
            return;
        }
        enviar(
                sessao.getCliente().getEmail(),
                EmailLogType.AGENDAMENTO_CONFIRMADO,
                "Agendamento confirmado - FisioGestao",
                "email/agendamento-confirmado",
                sessao.getId(),
                sessao.getCliente().getNomeCompleto(),
                sessao.getDataHoraInicio(),
                sessao.getObservacoes(),
                texto(profissional, "Equipe da clínica"),
                null,
                null
        );
    }

    public void notificarReagendado(SessaoEntity sessao, OffsetDateTime inicioAnterior, OffsetDateTime fimAnterior, String profissional) {
        if (!possuiEmail(sessao.getCliente().getEmail())) {
            return;
        }
        enviar(
                sessao.getCliente().getEmail(),
                EmailLogType.AGENDAMENTO_REAGENDADO,
                "Seu agendamento foi reagendado",
                "email/agendamento-reagendado",
                sessao.getId(),
                sessao.getCliente().getNomeCompleto(),
                sessao.getDataHoraInicio(),
                sessao.getObservacoes(),
                texto(profissional, "Equipe da clínica"),
                inicioAnterior,
                fimAnterior
        );
    }

    private void enviar(
            String destinatario,
            EmailLogType tipo,
            String assunto,
            String template,
            java.util.UUID entityId,
            String nomePaciente,
            OffsetDateTime inicio,
            String observacoes,
            String profissional,
            OffsetDateTime inicioAnterior,
            OffsetDateTime fimAnterior
    ) {
        try {
            var configuracao = configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc()
                    .orElseThrow(() -> new BusinessException("Configure a clínica antes de enviar notificações."));
            ZoneId zoneId = ZoneId.of(configuracao.getTimezoneId());
            systemEmailService.send(EmailRequest.builder()
                    .type(tipo)
                    .subject(assunto)
                    .to(destinatario)
                    .templateName(template)
                    .templateId(template)
                    .entityType("agendamento")
                    .entityId(entityId)
                    .triggeredBy("sistema")
                    .templateVariables(Map.ofEntries(
                            Map.entry("nomePaciente", nomePaciente),
                            Map.entry("dataHora", inicio.atZoneSameInstant(zoneId).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))),
                            Map.entry("hora", inicio.atZoneSameInstant(zoneId).format(DateTimeFormatter.ofPattern("HH:mm"))),
                            Map.entry("data", inicio.atZoneSameInstant(zoneId).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))),
                            Map.entry("local", configuracao.getNomeClinica()),
                            Map.entry("profissional", profissional),
                            Map.entry("observacoes", texto(observacoes, "Sem observações adicionais.")),
                            Map.entry("dataHoraAnterior", inicioAnterior == null ? "" : inicioAnterior.atZoneSameInstant(zoneId).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))),
                            Map.entry("horaAnterior", inicioAnterior == null ? "" : inicioAnterior.atZoneSameInstant(zoneId).format(DateTimeFormatter.ofPattern("HH:mm"))),
                            Map.entry("dataAnterior", inicioAnterior == null ? "" : inicioAnterior.atZoneSameInstant(zoneId).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))),
                            Map.entry("fimAnterior", fimAnterior == null ? "" : fimAnterior.atZoneSameInstant(zoneId).format(DateTimeFormatter.ofPattern("HH:mm")))
                    ))
                    .build());
        } catch (BusinessException ex) {
            log.warn("Notificação de e-mail não enviada (configuração ausente): {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("Falha inesperada ao enviar notificação de e-mail para {}: {}", destinatario, ex.getMessage(), ex);
        }
    }

    private boolean possuiEmail(String email) {
        return email != null && !email.isBlank() && email.contains("@");
    }

    private String texto(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor.trim();
    }
}
