package com.fisiogestao.config;

import com.fisiogestao.auth.InMemoryUserCatalog;
import com.fisiogestao.auth.UserMenuAccessService;
import com.fisiogestao.auth.UserRole;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class UserMenuAccessInterceptor implements HandlerInterceptor {

    private final InMemoryUserCatalog userCatalog;
    private final UserMenuAccessService userMenuAccessService;

    public UserMenuAccessInterceptor(InMemoryUserCatalog userCatalog, UserMenuAccessService userMenuAccessService) {
        this.userCatalog = userCatalog;
        this.userMenuAccessService = userMenuAccessService;
    }

    @Override
    @SuppressWarnings("null")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken || !authentication.isAuthenticated()) {
            return true;
        }

        UserRole role = userCatalog.buscarCredenciais(authentication.getName())
                .map(InMemoryUserCatalog.SystemUserCredentials::role)
                .orElse(null);

        if (userMenuAccessService.podeAcessar(role, request.getRequestURI())) {
            return true;
        }

        String destino = userMenuAccessService.resolverDestinoInicial(role);
        response.sendRedirect(destino + (destino.contains("?") ? "&" : "?") + "acessoNegado=1");
        return false;
    }
}
