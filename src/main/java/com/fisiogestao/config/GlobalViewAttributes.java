package com.fisiogestao.config;

import com.fisiogestao.auth.InMemoryUserCatalog;
import com.fisiogestao.auth.UserThemePreference;
import com.fisiogestao.auth.UserMenuAccessService;
import com.fisiogestao.auth.UserRole;
import com.fisiogestao.auth.UsuarioAutenticado;
import com.fisiogestao.tenant.PublicTenantFilter;
import com.fisiogestao.tenant.TenantContext;
import com.fisiogestao.listaespera.ListaEsperaRepository;
import com.fisiogestao.listaespera.StatusListaEspera;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@ControllerAdvice
public class GlobalViewAttributes {

    private final InMemoryUserCatalog userCatalog;
    private final UserMenuAccessService userMenuAccessService;
    private final ListaEsperaRepository listaEsperaRepository;

    public GlobalViewAttributes(
            InMemoryUserCatalog userCatalog,
            UserMenuAccessService userMenuAccessService,
            ListaEsperaRepository listaEsperaRepository
    ) {
        this.userCatalog = userCatalog;
        this.userMenuAccessService = userMenuAccessService;
        this.listaEsperaRepository = listaEsperaRepository;
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
    public String loggedUserDisplayName(Authentication authentication, HttpServletRequest request) {
        return resolveDisplayName(authentication, request);
    }

    @ModelAttribute("loggedUserInitial")
    public String loggedUserInitial(Authentication authentication, HttpServletRequest request) {
        String displayName = resolveDisplayName(authentication, request);
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
    public UserThemePreference loggedUserTheme(Authentication authentication, HttpServletRequest request) {
        return resolveSessionUser(authentication, request)
                .map(InMemoryUserCatalog.SystemUserSessionView::temaInterface)
                .orElse(UserThemePreference.LIGHT);
    }

    @ModelAttribute("hasAuthenticatedTheme")
    public boolean hasAuthenticatedTheme(Authentication authentication, HttpServletRequest request) {
        return resolveSessionUser(authentication, request).isPresent();
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : "";
    }

    @ModelAttribute("sidebarNavItems")
    public List<UserMenuAccessService.SidebarNavItem> sidebarNavItems(
            Authentication authentication,
            HttpServletRequest request
    ) {
        return userMenuAccessService.montarSidebarNavItems(resolveRole(authentication, request), currentPath(request));
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

    private String resolveDisplayName(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return "Usuário";
        }

        String username = authentication.getName();
        if (username == null || username.isBlank()) {
            return "Usuário";
        }

        return resolveSessionUser(authentication, request)
                .map(user -> user.nome() != null && !user.nome().isBlank() ? user.nome().trim() : user.username())
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
        UserRole role = resolveSessionUser(authentication, request)
                .map(InMemoryUserCatalog.SystemUserSessionView::role)
                .orElse(null);
        if (request != null && role != null) {
            request.setAttribute("loggedUserRoleResolved", role);
        }
        return role;
    }

    private Optional<InMemoryUserCatalog.SystemUserSessionView> resolveSessionUser(
            Authentication authentication,
            HttpServletRequest request
    ) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        String username = authentication.getName();
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        Object cached = request != null ? request.getAttribute("loggedUserSessionResolved") : null;
        if (cached instanceof InMemoryUserCatalog.SystemUserSessionView user) {
            return Optional.of(user);
        }

        Optional<InMemoryUserCatalog.SystemUserSessionView> resolved = userCatalog.buscarSessao(username);
        if (request != null) {
            resolved.ifPresent(user -> request.setAttribute("loggedUserSessionResolved", user));
        }
        return resolved;
    }

    /**
     * Código (slug) da clínica para montar links públicos: vem do recurso público resolvido no
     * servidor (slug/token) ou da empresa do usuário autenticado — nunca de parâmetro livre.
     */
    @ModelAttribute("clinicaSlug")
    public String clinicaSlug(HttpServletRequest request) {
        Object publica = request != null ? request.getAttribute(PublicTenantFilter.ATRIBUTO_EMPRESA_PUBLICA) : null;
        if (publica instanceof PublicTenantFilter.EmpresaPublica empresa) {
            return empresa.slug();
        }
        return TenantContext.usuarioAutenticado().map(UsuarioAutenticado::getEmpresaSlug).orElse(null);
    }

    @ModelAttribute("empresaPublica")
    public PublicTenantFilter.EmpresaPublica empresaPublica(HttpServletRequest request) {
        Object publica = request != null ? request.getAttribute(PublicTenantFilter.ATRIBUTO_EMPRESA_PUBLICA) : null;
        return publica instanceof PublicTenantFilter.EmpresaPublica empresa ? empresa : null;
    }

    @ModelAttribute("isSuperAdmin")
    public boolean isSuperAdmin() {
        return TenantContext.usuarioAutenticado().map(UsuarioAutenticado::isSuperAdmin).orElse(false);
    }

    @ModelAttribute("aguardandoListaEspera")
    public long aguardandoListaEspera(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return 0;
        }
        if (!TenantContext.possuiEmpresa()) {
            return 0;
        }
        try {
            return listaEsperaRepository.countByStatus(StatusListaEspera.AGUARDANDO);
        } catch (Exception e) {
            return 0;
        }
    }
}
