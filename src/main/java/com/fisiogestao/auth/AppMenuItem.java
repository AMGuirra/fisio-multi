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
    WHATSAPP("Fila WhatsApp", "/whatsapp", "messages-square", false, false, true, false, List.of("/whatsapp")),
    PACIENTES("Pacientes", "/pacientes", "stethoscope", false, false, true, false, List.of("/pacientes")),
    PACOTES_SESSOES("Planos de Tratamentos", "/pacotes-consumo", "package-open", false, false, true, false, List.of("/pacotes-consumo")),
    PRONTUARIO("Prontuário", "/prontuario", "clipboard-pen-line", false, false, true, false, List.of("/prontuario")),
    FINANCEIRO("Financeiro", "/financeiro", "wallet", false, false, true, false, List.of("/financeiro")),
    RELATORIOS("Relatórios", "/relatorios", "file-text", false, false, true, false, List.of("/relatorios")),
    CONFIG_CLINICA("Config. Clínica", "/configuracoes/clinica", "building-2", false, false, false, true, List.of("/configuracoes/clinica", "/configuracao/clinica")),
    USUARIOS("Usuários", "/usuarios", "users", false, false, false, true, List.of("/usuarios")),
    FUNCOES_USUARIOS("Funções de Usuários", "/funcoes-usuarios", "sliders-horizontal", false, false, false, true, List.of("/funcoes-usuarios")),
    CONFIG_EMAIL("Config. E-mail", "/configuracoes/email", "mail-check", false, false, true, false, List.of("/configuracoes/email", "/configuracao/email")),
    AUDITORIA("Auditoria", "/auditoria", "shield-check", false, false, true, false, List.of("/auditoria")),
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

    public String getDescription() {
        switch (this) {
            case DASHBOARD:
                return "Mostra o painel geral com os principais indicadores e atalhos do sistema.";
            case AGENDA:
                return "Permite visualizar e organizar os agendamentos do dia a dia.";
            case CALENDARIO:
                return "Exibe a agenda em formato de calendario para planejar os horarios.";
            case BLOQUEIOS:
                return "Controla bloqueios de horarios e indisponibilidades da agenda.";
            case SOLICITACOES:
                return "Centraliza pedidos de agendamento e outras solicitacoes recebidas.";
            case WHATSAPP:
                return "Mostra a fila de mensagens e atendimentos vinculados ao WhatsApp.";
            case PACIENTES:
                return "Permite cadastrar, localizar e acompanhar os pacientes da clinica.";
            case PACOTES_SESSOES:
                return "Gerencia planos de tratamento, pacotes contratados e consumos de sessoes.";
            case PRONTUARIO:
                return "Da acesso ao registro clinico e evolucao dos atendimentos.";
            case FINANCEIRO:
                return "Controla lancamentos, recebimentos, despesas e fluxo financeiro.";
            case RELATORIOS:
                return "Reune relatorios e consultas gerenciais do sistema.";
            case CONFIG_CLINICA:
                return "Permite ajustar dados e configuracoes gerais da clinica.";
            case USUARIOS:
                return "Gerencia o cadastro de usuarios que acessam o sistema.";
            case FUNCOES_USUARIOS:
                return "Define quais modulos cada funcao de usuario pode acessar.";
            case CONFIG_EMAIL:
                return "Configura envio de e-mails e parametros de comunicacao.";
            case AUDITORIA:
                return "Permite consultar o historico de acoes realizadas no sistema.";
            case AGENDAMENTO_ONLINE:
                return "Disponibiliza o link e o acesso ao agendamento online da clinica.";
            default:
                return label;
        }
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
                FUNCOES_USUARIOS,
                CONFIG_EMAIL,
                WHATSAPP,
                AUDITORIA
        );
    }

    public static List<AppMenuItem> sidebarRodape() {
        return List.of(
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
