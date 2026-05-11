package com.fisiogestao.email.service;

import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditEventCommand;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.email.EmailConfiguracaoEntity;
import com.fisiogestao.email.EmailLogEntity;
import com.fisiogestao.email.EmailLogStatus;
import com.fisiogestao.email.repository.EmailConfiguracaoRepository;
import com.fisiogestao.email.repository.EmailLogRepository;
import com.fisiogestao.auditoria.service.AuditService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class SystemEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemEmailService.class);

    private final EmailConfiguracaoRepository emailConfiguracaoRepository;
    private final EmailLogRepository emailLogRepository;
    private final EmailTemplateService emailTemplateService;
    private final SecretCryptoService secretCryptoService;
    private final AuditService auditService;

    public EmailDispatchResult send(EmailRequest request) {
        Optional<EmailConfiguracaoEntity> configuracao = emailConfiguracaoRepository.findFirstByAtivoTrueOrderByUpdatedAtDesc();
        if (configuracao.isEmpty()) {
            String mensagemErro = "Nenhuma configuração SMTP ativa encontrada.";
            registrarLog(null, request, EmailLogStatus.FALHA, mensagemErro);
            registrarAuditoria(request, EmailLogStatus.FALHA, mensagemErro, false);
            return EmailDispatchResult.failure("Nenhuma configuração SMTP ativa encontrada.");
        }
        return sendWithConfiguration(configuracao.get(), request);
    }

    @Transactional
    public EmailDispatchResult sendWithConfiguration(EmailConfiguracaoEntity configuracao, EmailRequest request) {
        List<String> destinatarios = sanitizarDestinatarios(request.to());
        if (destinatarios.isEmpty()) {
            registrarAuditoria(request, EmailLogStatus.FALHA, "Informe pelo menos um destinatário.", false);
            return EmailDispatchResult.failure("Informe pelo menos um destinatário.");
        }

        try {
            JavaMailSenderImpl mailSender = buildMailSender(configuracao);
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

            String emailRemetente = Objects.requireNonNull(configuracao.getEmailRemetente(), "E-mail remetente não configurado.");
            String nomeRemetente = Objects.requireNonNullElse(configuracao.getNomeRemetente(), "FisioGestao");
            helper.setFrom(emailRemetente, nomeRemetente);
            helper.setTo(destinatarios.toArray(String[]::new));
            List<String> cc = sanitizarDestinatarios(request.cc());
            if (!cc.isEmpty()) {
                helper.setCc(cc.toArray(String[]::new));
            }
            List<String> bcc = sanitizarDestinatarios(request.bcc());
            if (!bcc.isEmpty()) {
                helper.setBcc(bcc.toArray(String[]::new));
            }
            helper.setSubject(Objects.requireNonNullElse(request.subject(), "(sem assunto)"));

            String replyTo = request.replyToOverride() != null && !request.replyToOverride().isBlank()
                    ? request.replyToOverride().trim()
                    : configuracao.getReplyTo();
            if (replyTo != null && !replyTo.isBlank()) {
                helper.setReplyTo(replyTo);
            }

            String html = request.htmlBody();
            if ((html == null || html.isBlank()) && request.templateName() != null && !request.templateName().isBlank()) {
                html = emailTemplateService.render(request.templateName(), request.templateVariables());
            }
            String text = request.textBody() == null ? "" : request.textBody();
            helper.setText(text, html != null ? html : text);

            if (request.attachments() != null) {
                for (EmailAttachment attachment : request.attachments()) {
                    if (attachment == null) {
                        continue;
                    }
                    helper.addAttachment(
                            Objects.requireNonNullElse(attachment.fileName(), "anexo.bin"),
                            new ByteArrayResource(attachment.content() != null ? attachment.content() : new byte[0]),
                            Objects.requireNonNullElse(attachment.contentType(), "application/octet-stream")
                    );
                }
            }

            mailSender.send(mimeMessage);
            registrarLog(configuracao, request, EmailLogStatus.ENVIADO, null);
            registrarAuditoria(request, EmailLogStatus.ENVIADO, null, true);
            return EmailDispatchResult.success("E-mail enviado com sucesso.");
        } catch (Exception ex) {
            LOGGER.error("Falha ao enviar e-mail tipo {} para {}", request.type(), destinatarios, ex);
            String mensagemErro = mensagemLimpa(ex);
            registrarLog(configuracao, request, EmailLogStatus.FALHA, mensagemErro);
            registrarAuditoria(request, EmailLogStatus.FALHA, mensagemErro, false);
            return EmailDispatchResult.failure("Falha SMTP: " + mensagemErro);
        }
    }

    private JavaMailSenderImpl buildMailSender(EmailConfiguracaoEntity configuracao) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(configuracao.getSmtpHost());
        sender.setPort(configuracao.getSmtpPort());
        sender.setUsername(configuracao.getEmailRemetente());
        sender.setPassword(secretCryptoService.decrypt(configuracao.getSenhaAppCriptografada()));
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        // STARTTLS (porta 587) e SSL direto (porta 465) são mutuamente exclusivos.
        // Quando TLS (STARTTLS) está habilitado, SSL direto deve ser desabilitado.
        // Importante: Properties.getProperty() só lê Strings — usar String.valueOf() para garantir.
        boolean usaTls = Boolean.TRUE.equals(configuracao.getUsaTls());
        boolean usaSsl = !usaTls && Boolean.TRUE.equals(configuracao.getUsaSsl());
        boolean usaAuth = Boolean.TRUE.equals(configuracao.getUsaAutenticacao());

        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.auth", String.valueOf(usaAuth));
        properties.put("mail.smtp.starttls.enable", String.valueOf(usaTls));
        properties.put("mail.smtp.starttls.required", String.valueOf(usaTls));
        properties.put("mail.smtp.ssl.enable", String.valueOf(usaSsl));
        if (usaSsl) {
            properties.put("mail.smtp.ssl.trust", configuracao.getSmtpHost());
        }
        properties.put("mail.debug", "false");
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");
        return sender;
    }

    private void registrarLog(
            EmailConfiguracaoEntity configuracao,
            EmailRequest request,
            EmailLogStatus status,
            String mensagemErro
    ) {
        EmailLogEntity entity = EmailLogEntity.builder()
                .tipo(request.type())
                .destinatarioPrincipal(request.to() == null || request.to().isEmpty() ? null : request.to().getFirst())
                .destinatarios(request.to() == null ? null : String.join(", ", request.to()))
                .assunto(request.subject())
                .status(status)
                .mensagemErro(mensagemErro)
                .templateId(request.templateId() != null ? request.templateId() : request.templateName())
                .usuarioDisparador(request.triggeredBy())
                .entidadeTipo(request.entityType())
                .entidadeId(request.entityId())
                .observacoes(request.notes())
                .build();
        emailLogRepository.save(Objects.requireNonNull(entity));
    }

    private List<String> sanitizarDestinatarios(List<String> destinatarios) {
        if (destinatarios == null) {
            return List.of();
        }
        return destinatarios.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(item -> item.trim().toLowerCase())
                .distinct()
                .toList();
    }

    private String mensagemLimpa(Exception ex) {
        String mensagem = ex.getMessage();
        if (mensagem == null || mensagem.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return mensagem.length() > 400 ? mensagem.substring(0, 400) : mensagem;
    }

    private void registrarAuditoria(
            EmailRequest request,
            EmailLogStatus status,
            String mensagemErro,
            boolean sucesso
    ) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("tipoEmail", request.type() != null ? request.type().name() : null);
        payload.put("assunto", request.subject());
        payload.put("destinatarios", request.to());
        payload.put("cc", request.cc());
        payload.put("bcc", request.bcc());
        payload.put("template", request.templateId() != null ? request.templateId() : request.templateName());
        payload.put("entityType", request.entityType());
        payload.put("entityId", request.entityId());
        payload.put("observacoes", request.notes());
        payload.put("statusEnvio", status != null ? status.name() : null);
        payload.put("erro", mensagemErro);

        auditService.record(AuditEventCommand.builder()
                .module(resolveModule(request))
                .entity(request.entityType() != null && !request.entityType().isBlank() ? request.entityType() : "email")
                .entityId(request.entityId() != null ? request.entityId().toString() : null)
                .recordDescription(request.subject())
                .actionType(AuditActionType.SEND_EMAIL)
                .newPayload(payload)
                .reason(mensagemErro)
                .observation(sucesso ? "Envio de e-mail processado com sucesso." : "Falha no envio de e-mail.")
                .origin(resolveOrigin(request))
                .success(sucesso)
                .build());
    }

    private AuditModule resolveModule(EmailRequest request) {
        String entityType = request.entityType();
        if (entityType == null || entityType.isBlank()) {
            return AuditModule.EMAIL;
        }
        return switch (entityType) {
            case "agendamento", "sessao" -> AuditModule.AGENDA;
            case "usuario_sistema" -> AuditModule.USUARIOS;
            case "lancamento_financeiro" -> AuditModule.FINANCEIRO;
            case "relatorio_operacional", "lembrete" -> AuditModule.RELATORIOS;
            case "email_configuracao" -> AuditModule.CONFIGURACOES;
            default -> AuditModule.EMAIL;
        };
    }

    private AuditEventOrigin resolveOrigin(EmailRequest request) {
        if (request.triggeredBy() == null || request.triggeredBy().isBlank() || "sistema".equalsIgnoreCase(request.triggeredBy())) {
            return AuditEventOrigin.SISTEMA;
        }
        return AuditEventOrigin.MANUAL;
    }
}
