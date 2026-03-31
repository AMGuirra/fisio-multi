package com.fisiogestao.config;

import com.fisiogestao.auth.InMemoryUserCatalog;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalViewAttributes {

    private final InMemoryUserCatalog userCatalog;

    public GlobalViewAttributes(InMemoryUserCatalog userCatalog) {
        this.userCatalog = userCatalog;
    }

    @ModelAttribute("loggedUsername")
    public String loggedUsername(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return "Usuário";
        }

        String username = authentication.getName();
        if (username == null || username.isBlank()) {
            return "Usuário";
        }

        return username;
    }

    @ModelAttribute("loggedUserRole")
    public String loggedUserRole(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userCatalog.buscarCredenciais(authentication.getName())
                .map(item -> item.role().name())
                .orElse(null);
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : "";
    }
}
