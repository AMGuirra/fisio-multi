package com.fisiogestao.email.service;

import com.fisiogestao.tenant.TenantContext;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SystemEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemEmailService.class);

    private final EmailConfiguracaoRepository emailConfiguracaoRepository;
    private final EmailLogRepository emailLogRepository;
    private final EmailTemplateService emailTemplateService;
    private final SecretCryptoService secretCryptoService;
    private final AuditService auditService;
    private final SmtpEndpointValidator smtpEndpointValidator;

    /**
     * Envia usando o SMTP da clínica do contexto atual; se a clínica não tiver SMTP ativo,
     * usa o SMTP global da plataforma. Fora de uma clínica, usa apenas o SMTP global.
     */
    public EmailDispatchResult send(EmailRequest request) {
        Optional<EmailConfiguracaoEntity> configuracao = resolverConfiguracaoAtiva();
        if (configuracao.isEmpty()) {
            String mensagemErro = "Nenhuma configuração SMTP ativa encontrada.";
            registrarLog(null, request, EmailLogStatus.FALHA, mensagemErro);
            registrarAuditoria(request, EmailLogStatus.FALHA, mensagemErro, false);
            return EmailDispatchResult.failure("Nenhuma configuração SMTP ativa encontrada.");
        }
        return sendWithConfiguration(configuracao.get(), request);
    }

    /** Notificação administrativa da plataforma: sempre pelo SMTP global e com log sem empresa. */
    public EmailDispatchResult sendPlataforma(EmailRequest request) {
        return TenantContext.executarSemEmpresa(() -> send(request));
    }

    public Optional<EmailConfiguracaoEntity> resolverConfiguracaoAtiva() {
        UUID empresaId = TenantContext.empresaIdOuNull();
        if (empresaId != null) {
            Optional<EmailConfiguracaoEntity> daClinica =
                    emailConfiguracaoRepository.findFirstByEmpresaIdAndAtivoTrueOrderByUpdatedAtDesc(empresaId);
            if (daClinica.isPresent()) {
                return daClinica;
            }
        }
        return emailConfiguracaoRepository.findFirstByEmpresaIdIsNullAndAtivoTrueOrderByUpdatedAtDesc();
    }

    @Transactional
    public EmailDispatchResult sendWithConfiguration(EmailConfiguracaoEntity configuracao, EmailRequest request) {
        List<String> destinatarios = sanitizarDestinatarios(request.to());
        if (destinatarios.isEmpty()) {
            registrarAuditoria(request, EmailLogStatus.FALHA, "Informe pelo menos um destinatário.", false);
            return EmailDispatchResult.failure("Informe pelo menos um destinatário.");
        }

        try {
            SmtpEndpointValidator.SmtpEndpoint endpoint = smtpEndpointValidator.validarEResolver(
                    configuracao.getSmtpHost(), configuracao.getSmtpPort());
            JavaMailSenderImpl mailSender = buildMailSender(configuracao, endpoint);
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
            String incidentId = UUID.randomUUID().toString();
            LOGGER.error("Falha SMTP. incidente={} tipo={} destinatarios={}",
                    incidentId, request.type(), destinatarios.stream().map(this::mascararEmail).toList());
            String mensagemErro = "Falha no envio. incidente=" + incidentId;
            registrarLog(configuracao, request, EmailLogStatus.FALHA, mensagemErro);
            registrarAuditoria(request, EmailLogStatus.FALHA, mensagemErro, false);
            return EmailDispatchResult.failure("Nao foi possivel enviar o e-mail. Referencia: " + incidentId);
        }
    }

    private JavaMailSenderImpl buildMailSender(
            EmailConfiguracaoEntity configuracao,
            SmtpEndpointValidator.SmtpEndpoint endpoint
    ) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(endpoint.host());
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
        properties.put("mail.smtp.socketFactory", new PinnedSmtpSocketFactory(endpoint.pinnedAddress()));
        properties.put("mail.smtp.socketFactory.fallback", "false");
        properties.put("mail.smtp.ssl.checkserveridentity", "true");
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
                .empresaId(TenantContext.empresaIdOuNull())
                .tipo(request.type())
                .destinatarioPrincipal(request.to() == null || request.to().isEmpty()
                        ? null : mascararEmail(request.to().getFirst()))
                .destinatarios(request.to() == null
                        ? null : request.to().stream().map(this::mascararEmail).collect(java.util.stream.Collectors.joining(", ")))
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

    private String mascararEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return "[EMAIL_REDACTED]";
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at + 1);
        if (local.length() <= 2) {
            return local.charAt(0) + "***@" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
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
        payload.put("destinatarios", mascararDestinatarios(request.to()));
        payload.put("cc", mascararDestinatarios(request.cc()));
        payload.put("bcc", mascararDestinatarios(request.bcc()));
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

    private List<String> mascararDestinatarios(List<String> destinatarios) {
        return destinatarios == null ? List.of() : destinatarios.stream().map(this::mascararEmail).toList();
    }
}
