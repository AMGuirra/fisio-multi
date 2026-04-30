package com.fisiogestao.auditoria;

import lombok.Builder;

import java.util.Map;

@Builder
public record AuditEventCommand(
        AuditModule module,
        String entity,
        String entityId,
        String recordDescription,
        AuditActionType actionType,
        Map<String, Object> oldPayload,
        Map<String, Object> newPayload,
        String reason,
        String observation,
        AuditEventOrigin origin,
        AuditActor actor,
        String ipAddress,
        String userAgent,
        String unitId,
        Boolean success,
        Boolean critical
) {
}
