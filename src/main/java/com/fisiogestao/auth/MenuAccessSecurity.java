package com.fisiogestao.auth;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Ponte entre a autorização em profundidade ({@code @PreAuthorize} nos services) e as
 * permissões configuráveis pelo administrador na tela "Funções de Usuários"
 * ({@link UserMenuAccessService}). Sem isso, uma role liberada para um menu pelo admin
 * (ex.: Financeiro liberado para Fisioterapeuta) continuaria batendo em um
 * {@code hasAnyRole(...)} fixo no código e recebendo 403 mesmo com o menu liberado.
 *
 * <p>A role é lida diretamente das authorities da sessão autenticada (formato
 * {@code ROLE_<UserRole>}, atribuído em {@code SecurityConfig#userDetailsService}),
 * e não por uma nova consulta ao catálogo de usuários — mantém o comportamento
 * consistente com o restante do Spring Security (inclusive em testes com
 * {@code @WithMockUser}).</p>
 */
@Component("menuAccessSecurity")
public class MenuAccessSecurity {

    private final UserMenuAccessService userMenuAccessService;

    public MenuAccessSecurity(UserMenuAccessService userMenuAccessService) {
        this.userMenuAccessService = userMenuAccessService;
    }

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
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .map(this::parseRole)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private UserRole parseRole(String name) {
        try {
            return UserRole.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
