package com.fisiogestao.config;

import com.fisiogestao.auth.InMemoryUserCatalog;
import com.fisiogestao.auth.UserThemePreference;
import com.fisiogestao.auth.UserMenuAccessService;
import com.fisiogestao.auth.UserRole;
import com.fisiogestao.status.SystemStatusEntity;
import com.fisiogestao.status.SystemStatusService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@ControllerAdvice
public class GlobalViewAttributes {

    private final InMemoryUserCatalog userCatalog;
    private final UserMenuAccessService userMenuAccessService;
    private final SystemStatusService systemStatusService;

    public GlobalViewAttributes(
            InMemoryUserCatalog userCatalog,
            UserMenuAccessService userMenuAccessService,
            SystemStatusService systemStatusService
    ) {
        this.userCatalog = userCatalog;
        this.userMenuAccessService = userMenuAccessService;
        this.systemStatusService = systemStatusService;
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

    @ModelAttribute("loggedUserDisplayName")
    public String loggedUserDisplayName(Authentication authentication) {
        return resolveDisplayName(authentication);
    }

    @ModelAttribute("loggedUserInitial")
    public String loggedUserInitial(Authentication authentication) {
        String displayName = resolveDisplayName(authentication);
        if (displayName == null || displayName.isBlank()) {
            return "U";
        }
        return displayName.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    @ModelAttribute("loggedUserRole")
    public String loggedUserRole(Authentication authentication, HttpServletRequest request) {
        UserRole role = resolveRole(authentication, request);
        return role != null ? role.name() : null;
    }

    @ModelAttribute("loggedUserRoleLabel")
    public String loggedUserRoleLabel(Authentication authentication, HttpServletRequest request) {
        UserRole role = resolveRole(authentication, request);
        return role != null ? role.getLabel() : null;
    }

    @ModelAttribute("loggedUserTheme")
    public UserThemePreference loggedUserTheme(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return UserThemePreference.LIGHT;
        }
        return userCatalog.buscarTema(authentication.getName())
                .orElse(UserThemePreference.LIGHT);
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : "";
    }

    @ModelAttribute("sidebarMainMenus")
    public List<UserMenuAccessService.SidebarMenuView> sidebarMainMenus(
            Authentication authentication,
            HttpServletRequest request
    ) {
        return userMenuAccessService.montarSidebarPrincipal(resolveRole(authentication, request), currentPath(request));
    }

    @ModelAttribute("sidebarFooterMenus")
    public List<UserMenuAccessService.SidebarMenuView> sidebarFooterMenus(
            Authentication authentication,
            HttpServletRequest request
    ) {
        return userMenuAccessService.montarSidebarRodape(resolveRole(authentication, request), currentPath(request));
    }

    @ModelAttribute("accessibleMenuKeys")
    public Set<String> accessibleMenuKeys(Authentication authentication, HttpServletRequest request) {
        return userMenuAccessService.listarChavesMenuLiberadas(resolveRole(authentication, request));
    }

    private String resolveDisplayName(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return "Usuário";
        }

        String username = authentication.getName();
        if (username == null || username.isBlank()) {
            return "Usuário";
        }

        return userCatalog.buscarNomeExibicao(username)
                .orElse(username);
    }

    private UserRole resolveRole(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object cached = request != null ? request.getAttribute("loggedUserRoleResolved") : null;
        if (cached instanceof UserRole role) {
            return role;
        }
        UserRole role = userCatalog.buscarRole(authentication.getName()).orElse(null);
        if (request != null && role != null) {
            request.setAttribute("loggedUserRoleResolved", role);
        }
        return role;
    }

    @ModelAttribute("systemStatusAtual")
    public SystemStatusEntity systemStatusAtual() {
        return systemStatusService.getStatusAtual();
    }
}
