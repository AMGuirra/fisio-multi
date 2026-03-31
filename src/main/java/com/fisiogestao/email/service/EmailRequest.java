package com.fisiogestao.email.service;

import com.fisiogestao.email.EmailLogType;
import lombok.Builder;
import lombok.Singular;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
public record EmailRequest(
        EmailLogType type,
        String subject,
        @Singular("to") List<String> to,
        @Singular("cc") List<String> cc,
        @Singular("bcc") List<String> bcc,
        String textBody,
        String htmlBody,
        String templateName,
        @Builder.Default Map<String, Object> templateVariables,
        @Singular("attachment") List<EmailAttachment> attachments,
        String triggeredBy,
        String entityType,
        UUID entityId,
        String templateId,
        String replyToOverride,
        String notes
) {
}
