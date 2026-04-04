package com.fisiogestao.auth;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum AppMenuItem {
    DASHBOARD("Dashboard", "/dashboard", "layout-dashboard", false, false, true, false, List.of("/", "/dashboard")),
    AGENDA("Agenda", "/agenda", "calendar-days", false, false, true, false, List.of("/agenda")),
    CALENDARIO("Calendário", "/calendario", "calendar-range", false, false, true, false, List.of("/calendario")),
    BLOQUEIOS("Bloqueios", "/bloqueios", "calendar-x-2", false, false, true, false, List.of("/bloqueios")),
    SOLICITACOES("Solicitações", "/solicitacoes", "calendar-plus", false, false, true, false, List.of("/solicitacoes", "/api/solicitacoes")),
    WHATSAPP("Fila WhatsApp", "/whatsapp", "messages-square", true, false, true, false, List.of("/whatsapp")),
    PACIENTES("Pacientes", "/pacientes", "stethoscope", false, false, true, false, List.of("/pacientes")),
    PACOTES_SESSOES("Planos de Tratamentos", "/pacotes-consumo", "package-open", false, false, true, false, List.of("/pacotes-consumo")),
    PRONTUARIO("Prontuário", "/prontuario", "clipboard-pen-line", false, false, true, false, List.of("/prontuario")),
    FINANCEIRO("Financeiro", "/financeiro", "wallet", false, false, true, false, List.of("/financeiro")),
    RELATORIOS("Relatórios", "/relatorios", "file-text", false, false, true, false, List.of("/relatorios")),
    CONFIG_CLINICA("Config. Clínica", "/configuracoes/clinica", "building-2", false, false, false, true, List.of("/configuracoes/clinica", "/configuracao/clinica")),
    USUARIOS("Usuários", "/usuarios", "users", false, false, false, true, List.of("/usuarios")),
    FUNCOES_USUARIOS("Funções de Usuários", "/funcoes-usuarios", "sliders-horizontal", false, false, false, true, List.of("/funcoes-usuarios")),
    CONFIG_EMAIL("Config. E-mail", "/configuracoes/email", "mail-check", true, false, true, false, List.of("/configuracoes/email", "/configuracao/email")),
    AUDITORIA("Auditoria", "/auditoria", "shield-check", true, false, true, false, List.of("/auditoria")),
    AGENDAMENTO_ONLINE("Agendamento Online", "/agendamento-online", "external-link", true, true, true, false, List.of("/agendamento-online"));

    private final String label;
    private final String href;
    private final String icon;
    private final boolean footer;
    private final boolean external;
    private final boolean configuravelParaNaoAdmin;
    private final boolean adminOnly;
    private final List<String> pathPrefixes;

    AppMenuItem(
            String label,
            String href,
            String icon,
            boolean footer,
            boolean external,
            boolean configuravelParaNaoAdmin,
            boolean adminOnly,
            List<String> pathPrefixes
    ) {
        this.label = label;
        this.href = href;
        this.icon = icon;
        this.footer = footer;
        this.external = external;
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

    public boolean isExternal() {
        return external;
    }

    public boolean isConfiguravelParaNaoAdmin() {
        return configuravelParaNaoAdmin;
    }

    public boolean isAdminOnly() {
        return adminOnly;
    }

    public boolean isRotaInterna() {
        return !external;
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
        return List.of(
                DASHBOARD,
                SOLICITACOES,
                AGENDA,
                CALENDARIO,
                BLOQUEIOS,
                PACIENTES,
                PACOTES_SESSOES,
                PRONTUARIO,
                FINANCEIRO,
                RELATORIOS,
                CONFIG_CLINICA,
                USUARIOS,
                FUNCOES_USUARIOS
        );
    }

    public static List<AppMenuItem> sidebarRodape() {
        return List.of(
                CONFIG_EMAIL,
                WHATSAPP,
                AUDITORIA,
                AGENDAMENTO_ONLINE
        );
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
