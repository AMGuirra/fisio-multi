package com.fisiogestao.config;

import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.auth.InMemoryUserCatalog;
import com.fisiogestao.auth.UserMenuAccessService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuditAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuditService auditService;
    private final InMemoryUserCatalog userCatalog;
    private final UserMenuAccessService userMenuAccessService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        auditService.recordManual(
                AuditModule.AUTENTICACAO,
                "sessao_usuario",
                authentication != null ? authentication.getName() : null,
                "Login no sistema",
                AuditActionType.LOGIN,
                null,
                Map.of("username", authentication != null ? authentication.getName() : "desconhecido"),
                null,
                "Autenticação concluída com sucesso.",
                AuditEventOrigin.MANUAL
        );
        var role = authentication != null
                ? userCatalog.buscarRole(authentication.getName()).orElse(null)
                : null;
        response.sendRedirect(userMenuAccessService.resolverDestinoInicial(role));
    }
}
