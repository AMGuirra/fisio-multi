package com.fisiogestao.email.service;

import com.fisiogestao.email.EmailConfiguracaoEntity;
import com.fisiogestao.email.EmailLogEntity;
import com.fisiogestao.email.EmailLogStatus;
import com.fisiogestao.email.repository.EmailConfiguracaoRepository;
import com.fisiogestao.email.repository.EmailLogRepository;
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
import java.util.List;
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

    public EmailDispatchResult send(EmailRequest request) {
        Optional<EmailConfiguracaoEntity> configuracao = emailConfiguracaoRepository.findFirstByAtivoTrueOrderByUpdatedAtDesc();
        if (configuracao.isEmpty()) {
            registrarLog(null, request, EmailLogStatus.FALHA, "Nenhuma configuração SMTP ativa encontrada.");
            return EmailDispatchResult.failure("Nenhuma configuração SMTP ativa encontrada.");
        }
        return sendWithConfiguration(configuracao.get(), request);
    }

    @Transactional
    public EmailDispatchResult sendWithConfiguration(EmailConfiguracaoEntity configuracao, EmailRequest request) {
        List<String> destinatarios = sanitizarDestinatarios(request.to());
        if (destinatarios.isEmpty()) {
            return EmailDispatchResult.failure("Informe pelo menos um destinatário.");
        }

        try {
            JavaMailSenderImpl mailSender = buildMailSender(configuracao);
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

            helper.setFrom(configuracao.getEmailRemetente(), configuracao.getNomeRemetente());
            helper.setTo(destinatarios.toArray(String[]::new));
            if (request.cc() != null && !request.cc().isEmpty()) {
                helper.setCc(sanitizarDestinatarios(request.cc()).toArray(String[]::new));
            }
            if (request.bcc() != null && !request.bcc().isEmpty()) {
                helper.setBcc(sanitizarDestinatarios(request.bcc()).toArray(String[]::new));
            }
            helper.setSubject(request.subject());

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
                    helper.addAttachment(
                            attachment.fileName(),
                            new ByteArrayResource(attachment.content()),
                            attachment.contentType()
                    );
                }
            }

            mailSender.send(mimeMessage);
            registrarLog(configuracao, request, EmailLogStatus.ENVIADO, null);
            return EmailDispatchResult.success("E-mail enviado com sucesso.");
        } catch (Exception ex) {
            LOGGER.error("Falha ao enviar e-mail tipo {} para {}", request.type(), destinatarios, ex);
            registrarLog(configuracao, request, EmailLogStatus.FALHA, mensagemLimpa(ex));
            return EmailDispatchResult.failure("Não foi possível enviar o e-mail. Verifique a configuração SMTP e tente novamente.");
        }
    }

    private JavaMailSenderImpl buildMailSender(EmailConfiguracaoEntity configuracao) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(configuracao.getSmtpHost());
        sender.setPort(configuracao.getSmtpPort());
        sender.setUsername(configuracao.getEmailRemetente());
        sender.setPassword(secretCryptoService.decrypt(configuracao.getSenhaAppCriptografada()));
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.auth", Boolean.TRUE.equals(configuracao.getUsaAutenticacao()));
        properties.put("mail.smtp.starttls.enable", Boolean.TRUE.equals(configuracao.getUsaTls()));
        properties.put("mail.smtp.ssl.enable", Boolean.TRUE.equals(configuracao.getUsaSsl()));
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
        emailLogRepository.save(entity);
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
}
