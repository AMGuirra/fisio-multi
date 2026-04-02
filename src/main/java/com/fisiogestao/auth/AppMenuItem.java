package com.fisiogestao.auth;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum AppMenuItem {
    DASHBOARD("Dashboard", "/dashboard", "layout-dashboard", false, true, false, List.of("/", "/dashboard")),
    AGENDA("Agenda", "/agenda", "calendar-days", false, true, false, List.of("/agenda")),
    CALENDARIO("Calendário", "/calendario", "calendar-range", false, true, false, List.of("/calendario")),
    BLOQUEIOS("Bloqueios", "/bloqueios", "calendar-x-2", false, true, false, List.of("/bloqueios")),
    SOLICITACOES("Solicitações", "/solicitacoes", "calendar-plus", false, true, false, List.of("/solicitacoes", "/api/solicitacoes")),
    PACIENTES("Pacientes", "/pacientes", "stethoscope", false, true, false, List.of("/pacientes")),
    PACOTES_SESSOES("Planos de Tratamentos", "/pacotes-consumo", "package-open", false, true, false, List.of("/pacotes-consumo")),
    PRONTUARIO("Prontuário", "/prontuario", "clipboard-pen-line", false, true, false, List.of("/prontuario")),
    FINANCEIRO("Financeiro", "/financeiro", "wallet", false, true, false, List.of("/financeiro")),
    RELATORIOS("Relatórios", "/relatorios", "file-text", false, true, false, List.of("/relatorios")),
    USUARIOS("Usuários", "/usuarios", "users", false, false, true, List.of("/usuarios")),
    FUNCOES_USUARIOS("Funções de Usuários", "/funcoes-usuarios", "sliders-horizontal", false, false, true, List.of("/funcoes-usuarios")),
    CONFIG_EMAIL("Config. E-mail", "/configuracoes/email", "mail-check", false, true, false, List.of("/configuracoes/email", "/configuracao/email")),
    AUDITORIA("Auditoria", "/auditoria", "shield-check", false, true, false, List.of("/auditoria")),
    AGENDAMENTO_ONLINE("Agendamento Online", "/agendamento-online", "external-link", true, true, false, List.of("/agendamento-online"));

    private final String label;
    private final String href;
    private final String icon;
    private final boolean footer;
    private final boolean configuravelParaNaoAdmin;
    private final boolean adminOnly;
    private final List<String> pathPrefixes;

    AppMenuItem(
            String label,
            String href,
            String icon,
            boolean footer,
            boolean configuravelParaNaoAdmin,
            boolean adminOnly,
            List<String> pathPrefixes
    ) {
        this.label = label;
        this.href = href;
        this.icon = icon;
        this.footer = footer;
        this.configuravelParaNaoAdmin = configuravelParaNaoAdmin;
        this.adminOnly = adminOnly;
        this.pathPrefixes = pathPrefixes;
    }

    public String getLabel() {
        return label;
    }

    public String getHref() {
        return href;
    }

    public String getIcon() {
        return icon;
    }

    public boolean isFooter() {
        return footer;
    }

    public boolean isConfiguravelParaNaoAdmin() {
        return configuravelParaNaoAdmin;
    }

    public boolean isAdminOnly() {
        return adminOnly;
    }

    public boolean isRotaInterna() {
        return !footer;
    }

    public boolean matchesPath(String currentPath) {
        if (currentPath == null || currentPath.isBlank()) {
            return false;
        }
        return pathPrefixes.stream().anyMatch(prefix -> matchesPrefix(currentPath, prefix));
    }

    public static Optional<AppMenuItem> fromPath(String currentPath) {
        if (currentPath == null || currentPath.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(item -> item.matchesPath(currentPath))
                .findFirst();
    }

    public static List<AppMenuItem> sidebarPrincipais() {
        return Arrays.stream(values())
                .filter(item -> !item.isFooter())
                .toList();
    }

    public static List<AppMenuItem> sidebarRodape() {
        return Arrays.stream(values())
                .filter(AppMenuItem::isFooter)
                .toList();
    }

    public static List<AppMenuItem> configuraveisParaNaoAdmin() {
        return Arrays.stream(values())
                .filter(AppMenuItem::isConfiguravelParaNaoAdmin)
                .toList();
    }

    private static boolean matchesPrefix(String currentPath, String prefix) {
        if ("/".equals(prefix)) {
            return "/".equals(currentPath);
        }
        return currentPath.equals(prefix) || currentPath.startsWith(prefix + "/");
    }
}
