package com.fisiogestao.config;

import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuditLogoutHandler implements LogoutHandler {

    private final AuditService auditService;

    @Override
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return;
        }
        auditService.recordManual(
                AuditModule.AUTENTICACAO,
                "sessao_usuario",
                authentication.getName(),
                "Logout do sistema",
                AuditActionType.LOGOUT,
                Map.of("username", authentication.getName()),
                null,
                null,
                "Sessão encerrada pelo usuário.",
                AuditEventOrigin.MANUAL
        );
    }
}
