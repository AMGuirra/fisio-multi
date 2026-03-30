package com.fisiogestao.config;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalViewAttributes {

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
}
