package com.fisiogestao.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Ponte entre a autorização em profundidade ({@code @PreAuthorize} nos services) e as
 * permissões configuráveis pelo administrador na tela "Funções de Usuários"
 * ({@link UserMenuAccessService}). Sem isso, uma role liberada para um menu pelo admin
 * (ex.: Financeiro liberado para Fisioterapeuta) continuaria batendo em um
 * {@code hasAnyRole(...)} fixo no código e recebendo 403 mesmo com o menu liberado.
 */
@Component("menuAccessSecurity")
@RequiredArgsConstructor
public class MenuAccessSecurity {

    private final InMemoryUserCatalog userCatalog;
    private final UserMenuAccessService userMenuAccessService;

    public boolean canAccess(String menuItem) {
        return canAccessAny(menuItem);
    }

    public boolean canAccessAny(String... menuItens) {
        UserRole role = currentRole();
        if (role == null) {
            return false;
        }
        if (role.isAdministrador()) {
            return true;
        }
        var liberados = userMenuAccessService.listarMenusLiberados(role);
        return Arrays.stream(menuItens)
                .map(this::parseMenuItem)
                .filter(java.util.Objects::nonNull)
                .anyMatch(liberados::contains);
    }

    private AppMenuItem parseMenuItem(String menuItem) {
        try {
            return AppMenuItem.valueOf(menuItem);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private UserRole currentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken || !authentication.isAuthenticated()) {
            return null;
        }
        return userCatalog.buscarCredenciais(authentication.getName())
                .map(InMemoryUserCatalog.SystemUserCredentials::role)
                .orElse(null);
    }
}
