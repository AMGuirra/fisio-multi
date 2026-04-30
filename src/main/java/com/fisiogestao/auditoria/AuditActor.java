package com.fisiogestao.auditoria;

import com.fisiogestao.auth.UsuarioSistemaEntity;
import lombok.Builder;

import java.util.UUID;

@Builder
public record AuditActor(
        UUID userId,
        String username,
        String userName,
        String userRole
) {
    public static AuditActor fromUser(UsuarioSistemaEntity user) {
        if (user == null) {
            return null;
        }
        return AuditActor.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .userName(user.getNome())
                .userRole(user.getRole() != null ? user.getRole().name() : null)
                .build();
    }
}
