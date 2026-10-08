package com.fisiogestao.auth;

import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserMenuAccessService {

    private final UserRoleMenuPermissionRepository permissionRepository;

    @Transactional
    public EnumSet<AppMenuItem> listarMenusLiberados(UserRole role) {
        if (role == null) {
            return EnumSet.noneOf(AppMenuItem.class);
        }
        if (role.isSuperAdmin()) {
            return menusSuperAdmin();
        }
        EnumSet<AppMenuItem> menus = role.isAdministrador()
                ? menusDeClinica()
                : carregarOuCriarPermissoes(role);
        return normalizarMenusLegados(menus);
    }

    public List<AppMenuItem> listarMenusConfiguraveis() {
        return AppMenuItem.configuraveisParaNaoAdmin();
    }

    @Transactional
    public List<RolePermissionsView> listarPermissoesPorFuncao() {
        return List.of(
                new RolePermissionsView(
                        UserRole.ADMIN_CLINICA,
                        UserRole.ADMIN_CLINICA.getLabel(),
                        filtrarMenusDisponiveisNaInterface(listarMenusLiberados(UserRole.ADMIN_CLINICA)),
                        true
                ),
                buildRolePermissionsView(UserRole.FISIOTERAPEUTA),
                buildRolePermissionsView(UserRole.RECEPCAO),
                buildRolePermissionsView(UserRole.ASSESSORIA),
                buildRolePermissionsView(UserRole.FINANCEIRO)
        );
    }

    @Transactional
    public void salvarPermissoes(UserRole role, Set<AppMenuItem> menusSolicitados) {
        if (role == null) {
            throw new BusinessException("Selecione a função que deseja configurar.");
        }
        if (role.isAdministrador()) {
            throw new BusinessException("O perfil Administrador possui acesso total fixo e não pode ser alterado.");
        }

        EnumSet<AppMenuItem> menusNormalizados = normalizarMenus(menusSolicitados);
        if (menusNormalizados.isEmpty()) {
            throw new BusinessException("Selecione ao menos um menu para " + role.getLabel() + ".");
        }
        boolean possuiDestinoInterno = menusNormalizados.stream().anyMatch(AppMenuItem::isRotaInterna);
        if (!possuiDestinoInterno) {
            throw new BusinessException("Selecione ao menos um menu interno para " + role.getLabel() + ".");
        }

        permissionRepository.deleteByRole(role);
        permissionRepository.flush();
        permissionRepository.saveAll(
                menusNormalizados.stream()
                        .map(menu -> UserRoleMenuPermissionEntity.builder()
                                .role(role)
                                .menuKey(menu)
                                .build())
                        .toList()
        );
    }

    @Transactional
    public List<SidebarNavItem> montarSidebarNavItems(UserRole role, String currentPath) {
        EnumSet<AppMenuItem> liberados = listarMenusLiberados(role);
        List<SidebarNavItem> result = new ArrayList<>();
        AppMenuItem.MenuSection currentSection = null;
        for (AppMenuItem menu : (role != null && role.isSuperAdmin() ? AppMenuItem.sidebarPlataforma() : AppMenuItem.sidebarPrincipais())) {
            if (!podeVisualizarMenu(role, liberados, menu)) continue;
            AppMenuItem.MenuSection section = menu.getSection();
            if (section != currentSection) {
                if (section.hasLabel()) {
                    result.add(SidebarNavItem.sectionHeader(section.getLabel()));
                }
                currentSection = section;
            }
            result.add(SidebarNavItem.menuItem(
                    menu.name(), menu.getLabel(), menu.getHref(), menu.getIcon(),
                    menu.matchesPath(currentPath), menu.isExternal()
            ));
        }
        return result;
    }

    @Transactional
    public List<SidebarMenuView> montarSidebarRodape(UserRole role, String currentPath) {
        EnumSet<AppMenuItem> liberados = listarMenusLiberados(role);
        return AppMenuItem.sidebarRodape().stream()
                .filter(menu -> podeVisualizarMenu(role, liberados, menu))
                .map(menu -> new SidebarMenuView(
                        menu.name(),
                        menu.getLabel(),
                        menu.getHref(),
                        menu.getIcon(),
                        menu.matchesPath(currentPath),
                        menu.isExternal()
                ))
                .toList();
    }

    @Transactional
    public Set<String> listarChavesMenuLiberadas(UserRole role) {
        return listarMenusLiberados(role).stream()
                .filter(AppMenuItem::isDisponivelNaInterface)
                .map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Transactional
    public boolean podeAcessar(UserRole role, String currentPath) {
        if (role == null) {
            return false;
        }
        var menuDaRota = AppMenuItem.fromPath(currentPath);
        if (role.isSuperAdmin()) {
            // O superadmin opera apenas a área Gestor (e o SMTP global): nunca páginas de clínica.
            return menuDaRota.map(menusSuperAdmin()::contains).orElse(false);
        }
        if (menuDaRota.map(AppMenuItem::isPlataforma).orElse(false)) {
            return false;
        }
        if (role.isAdministrador()) {
            return true;
        }

        return menuDaRota
                .map(menu -> {
                    if (menu.isAdminOnly()) {
                        return false;
                    }
                    if (!menu.isRotaInterna()) {
                        return true;
                    }
                    return listarMenusLiberados(role).contains(menu);
                })
                .orElse(false);
    }

    @Transactional
    public String resolverDestinoInicial(UserRole role) {
        if (role != null && role.isSuperAdmin()) {
            return "/gestor";
        }
        if (role == null || role.isAdministrador()) {
            return "/dashboard";
        }

        EnumSet<AppMenuItem> liberados = listarMenusLiberados(role);
        return AppMenuItem.sidebarPrincipais().stream()
                .filter(liberados::contains)
                .filter(AppMenuItem::isRotaInterna)
                .map(AppMenuItem::getHref)
                .findFirst()
                .orElse("/dashboard");
    }

    /** Todos os menus de clínica (sem os itens da plataforma). */
    private static EnumSet<AppMenuItem> menusDeClinica() {
        EnumSet<AppMenuItem> menus = EnumSet.allOf(AppMenuItem.class);
        menus.removeIf(AppMenuItem::isPlataforma);
        return menus;
    }

    private static EnumSet<AppMenuItem> menusSuperAdmin() {
        return EnumSet.copyOf(AppMenuItem.sidebarPlataforma());
    }

    private RolePermissionsView buildRolePermissionsView(UserRole role) {
        return new RolePermissionsView(
                role,
                role.getLabel(),
                filtrarMenusDisponiveisNaInterface(listarMenusLiberados(role)),
                false
        );
    }

    private boolean podeVisualizarMenu(UserRole role, EnumSet<AppMenuItem> menusLiberados, AppMenuItem menu) {
        if (role == null) {
            return false;
        }
        if (role.isSuperAdmin()) {
            return menusLiberados.contains(menu);
        }
        if (menu.isPlataforma()) {
            return false;
        }
        if (role.isAdministrador()) {
            return true;
        }
        if (menu.isAdminOnly()) {
            return false;
        }
        return menusLiberados.contains(menu);
    }

    private EnumSet<AppMenuItem> carregarOuCriarPermissoes(UserRole role) {
        List<UserRoleMenuPermissionEntity> permissoes = permissionRepository.findByRoleOrderByMenuKeyAsc(role);
        if (permissoes.isEmpty()) {
            EnumSet<AppMenuItem> defaults = defaultsPara(role);
            permissionRepository.saveAll(
                    defaults.stream()
                            .map(menu -> UserRoleMenuPermissionEntity.builder()
                                    .role(role)
                                    .menuKey(menu)
                                    .build())
                            .toList()
            );
            return defaults;
        }

        EnumSet<AppMenuItem> menus = EnumSet.noneOf(AppMenuItem.class);
        permissoes.stream()
                .map(UserRoleMenuPermissionEntity::getMenuKey)
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .forEach(menus::add);
        return menus;
    }

    private EnumSet<AppMenuItem> defaultsPara(UserRole role) {
        return switch (role) {
            case ADMIN_CLINICA -> menusDeClinica();
            case SUPER_ADMIN -> EnumSet.noneOf(AppMenuItem.class);
            case FISIOTERAPEUTA -> EnumSet.of(
                    AppMenuItem.DASHBOARD,
                    AppMenuItem.AGENDA,
                    AppMenuItem.BLOQUEIOS,
                    AppMenuItem.SOLICITACOES,
                    AppMenuItem.PACIENTES,
                    AppMenuItem.RELATORIOS
            );
            case RECEPCAO -> EnumSet.of(
                    AppMenuItem.DASHBOARD,
                    AppMenuItem.AGENDA,
                    AppMenuItem.BLOQUEIOS,
                    AppMenuItem.SOLICITACOES,
                    AppMenuItem.PLANOS_SESSOES,
                    AppMenuItem.PACIENTES,
                    AppMenuItem.AGENDAMENTO_ONLINE
            );
            case ASSESSORIA -> EnumSet.of(
                    AppMenuItem.DASHBOARD,
                    AppMenuItem.SOLICITACOES,
                    AppMenuItem.PACIENTES,
                    AppMenuItem.RELATORIOS
            );
            case FINANCEIRO -> EnumSet.of(
                    AppMenuItem.DASHBOARD,
                    AppMenuItem.PACIENTES,
                    AppMenuItem.FINANCEIRO,
                    AppMenuItem.CONTAS_RECEBER,
                    AppMenuItem.CONTAS_PAGAR,
                    AppMenuItem.FATURAMENTO,
                    AppMenuItem.RELATORIOS
            );
        };
    }

    private EnumSet<AppMenuItem> normalizarMenus(Set<AppMenuItem> menusSolicitados) {
        EnumSet<AppMenuItem> normalizados = EnumSet.noneOf(AppMenuItem.class);
        if (menusSolicitados == null || menusSolicitados.isEmpty()) {
            return normalizados;
        }
        menusSolicitados.stream()
                .map(menu -> menu == AppMenuItem.CALENDARIO ? AppMenuItem.AGENDA : menu)
                .filter(AppMenuItem::isConfiguravelParaNaoAdmin)
                .forEach(normalizados::add);
        return aplicarDependenciasDeMenu(normalizados);
    }

    private EnumSet<AppMenuItem> normalizarMenusLegados(Set<AppMenuItem> menus) {
        EnumSet<AppMenuItem> normalizados = EnumSet.noneOf(AppMenuItem.class);
        if (menus == null || menus.isEmpty()) {
            return normalizados;
        }
        menus.forEach(menu -> normalizados.add(menu == AppMenuItem.CALENDARIO ? AppMenuItem.AGENDA : menu));
        return aplicarDependenciasDeMenu(normalizados);
    }

    /**
     * O Relatório de Faturamento foi unificado dentro da Central de Relatórios (aba
     * "Faturamento detalhado" em {@code /relatorios}). Como a rota isolada
     * {@code /financeiro/relatorio-faturamento} agora apenas redireciona para lá, quem tem
     * FATURAMENTO mas não tem RELATORIOS ficaria bloqueado pelo {@link UserMenuAccessInterceptor}
     * logo após o redirecionamento (path {@code /relatorios} exige RELATORIOS). Para não deixar
     * essa combinação de permissões inacessível, FATURAMENTO passa a exigir RELATORIOS sempre
     * que for concedido — tanto ao salvar em "Funções de Usuários" quanto ao ler permissões já
     * persistidas antes desta regra existir.
     */
    private EnumSet<AppMenuItem> aplicarDependenciasDeMenu(EnumSet<AppMenuItem> menus) {
        if (menus.contains(AppMenuItem.FATURAMENTO)) {
            menus.add(AppMenuItem.RELATORIOS);
        }
        return menus;
    }

    private EnumSet<AppMenuItem> filtrarMenusDisponiveisNaInterface(Set<AppMenuItem> menus) {
        EnumSet<AppMenuItem> filtrados = EnumSet.noneOf(AppMenuItem.class);
        if (menus == null || menus.isEmpty()) {
            return filtrados;
        }
        menus.stream()
                .filter(AppMenuItem::isDisponivelNaInterface)
                .forEach(filtrados::add);
        return filtrados;
    }

    public record SidebarMenuView(
            String key,
            String label,
            String href,
            String icon,
            boolean active,
            boolean external
    ) {
    }

    public record SidebarNavItem(
            boolean section,
            String label,
            String href,
            String icon,
            boolean active,
            boolean external
    ) {
        public static SidebarNavItem sectionHeader(String label) {
            return new SidebarNavItem(true, label, null, null, false, false);
        }

        public static SidebarNavItem menuItem(
                String key, String label, String href, String icon, boolean active, boolean external) {
            return new SidebarNavItem(false, label, href, icon, active, external);
        }
    }

    public record RolePermissionsView(
            UserRole role,
            String label,
            Set<AppMenuItem> grantedMenus,
            boolean readOnly
    ) {
    }
}
