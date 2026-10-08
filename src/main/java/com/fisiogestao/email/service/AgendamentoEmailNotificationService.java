package com.fisiogestao.email.service;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.email.EmailLogType;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgendamentoEmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AgendamentoEmailNotificationService.class);

    private final SystemEmailService systemEmailService;
    private final ConfiguracaoClinicaRepository configuracaoClinicaRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

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

    public void notificarCanceladoPorSessao(SessaoEntity sessao, String motivo, String profissional) {
        if (!possuiEmail(sessao.getCliente().getEmail())) {
            return;
        }
        enviar(
                sessao.getCliente().getEmail(),
                EmailLogType.AGENDAMENTO_CANCELADO,
                "Seu agendamento foi cancelado",
                "email/agendamento-cancelado",
                sessao.getId(),
                sessao.getCliente().getNomeCompleto(),
                sessao.getDataHoraInicio(),
                motivo,
                texto(profissional, "Equipe da clínica"),
                null,
                null
        );
    }

    public void notificarPrimeiroAcesso(SolicitacaoAgendamentoEntity solicitacao, String senhaEmTexto) {
        if (!possuiEmail(solicitacao.getEmail())) {
            return;
        }
        try {
            var configuracao = configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc()
                    .orElseThrow(() -> new BusinessException("Configure a clínica antes de enviar notificações."));
            ZoneId zoneId = ZoneId.of(configuracao.getTimezoneId());
            String portalUrl = appBaseUrl + "/paciente/" + solicitacao.getCliente().getPortalToken();
            systemEmailService.send(EmailRequest.builder()
                    .type(EmailLogType.AGENDAMENTO_PRIMEIRO_ACESSO)
                    .subject("Solicitação recebida + acesse seu portal")
                    .to(solicitacao.getEmail())
                    .templateName("email/agendamento-primeiro-acesso")
                    .templateId("email/agendamento-primeiro-acesso")
                    .entityType("agendamento")
                    .entityId(solicitacao.getId())
                    .triggeredBy("sistema")
                    .templateVariables(Map.of(
                            "nomePaciente", solicitacao.getNomePaciente(),
                            "data", solicitacao.getDataHoraInicio().atZoneSameInstant(zoneId).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            "hora", solicitacao.getDataHoraInicio().atZoneSameInstant(zoneId).format(DateTimeFormatter.ofPattern("HH:mm")),
                            "local", configuracao.getNomeClinica(),
                            "observacoes", texto(solicitacao.getObservacoes(), "Sem observações adicionais."),
                            "portalUrl", portalUrl,
                            "senha", senhaEmTexto,
                            "nomeClinica", configuracao.getNomeClinica()
                    ))
                    .build());
        } catch (BusinessException ex) {
            log.warn("Notificação de e-mail não enviada (configuração ausente): {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("Falha inesperada ao enviar e-mail de primeiro acesso para {}: {}", solicitacao.getEmail(), ex.getMessage(), ex);
        }
    }

    public void notificarAcessoPortal(ClienteEntity paciente, String senhaEmTexto) {
        if (!possuiEmail(paciente.getEmail())) {
            return;
        }
        String portalUrl = appBaseUrl + "/paciente/" + paciente.getPortalToken();
        try {
            var configuracao = configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc().orElse(null);
            String nomeClinica = configuracao != null ? configuracao.getNomeClinica() : "FisioGestao";
            systemEmailService.send(EmailRequest.builder()
                    .type(EmailLogType.PORTAL_ACESSO)
                    .subject("Acesse seu portal de acompanhamento")
                    .to(paciente.getEmail())
                    .templateName("email/portal-acesso-paciente")
                    .templateId("email/portal-acesso-paciente")
                    .entityType("cliente")
                    .entityId(paciente.getId())
                    .triggeredBy("sistema")
                    .templateVariables(Map.of(
                            "nomePaciente", paciente.getNomeCompleto(),
                            "portalUrl", portalUrl,
                            "senha", senhaEmTexto,
                            "nomeClinica", nomeClinica
                    ))
                    .build());
        } catch (Exception ex) {
            log.error("Falha ao enviar e-mail de acesso ao portal para {}: {}", paciente.getEmail(), ex.getMessage(), ex);
        }
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
