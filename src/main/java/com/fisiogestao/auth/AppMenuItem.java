package com.fisiogestao.auth;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum AppMenuItem {
    DASHBOARD("Dashboard",           "/dashboard",           "layout-dashboard",    false, false, true,  false, MenuSection.NONE,   List.of("/", "/dashboard", "/api/dashboard")),
    AGENDA("Agenda",                 "/calendario",          "calendar-days",       false, false, true,  false, MenuSection.AGENDA, List.of("/agenda", "/calendario", "/api/agendamentos")),
    CALENDARIO("Calendário",         "/calendario",          "calendar-range",      false, false, false, false, MenuSection.AGENDA, List.of("/calendario")),
    BLOQUEIOS("Bloqueios",           "/bloqueios",            "calendar-x-2",      false, false, true,  false, MenuSection.ADMIN, List.of("/bloqueios")),
    SOLICITACOES("Solicitações",     "/solicitacoes",        "calendar-plus",       false, false, true,  false, MenuSection.AGENDA, List.of("/solicitacoes", "/api/solicitacoes")),
    PACIENTES("Pacientes",           "/pacientes",           "stethoscope",         false, false, true,  false, MenuSection.CLINICO, List.of("/pacientes", "/api/pacientes")),
    PLANOS_SESSOES("Planos de Tratamentos", "/planos-consumo", "package-open",    false, false, true,  false, MenuSection.CLINICO, List.of("/planos-consumo")),
    PRONTUARIO("Prontuário",         "/prontuario",          "clipboard-pen-line",  false, false, true,  false, MenuSection.CLINICO, List.of("/prontuario")),
    FINANCEIRO("Financeiro",         "/financeiro",          "wallet",              false, false, true,  false, MenuSection.GESTAO,  List.of("/financeiro")),
    CONTAS_RECEBER("Contas a Receber", "/financeiro/contas-a-receber", "badge-dollar-sign", false, false, true, false, MenuSection.GESTAO, List.of("/financeiro/contas-a-receber")),
    CONTAS_PAGAR("Contas a Pagar",   "/financeiro/contas-a-pagar", "receipt",       false, false, true,  false, MenuSection.GESTAO,  List.of("/financeiro/contas-a-pagar")),
    FATURAMENTO("Faturamento",       "/financeiro/relatorio-faturamento", "bar-chart-3", false, false, true, false, MenuSection.GESTAO, List.of("/financeiro/relatorio-faturamento")),
    RELATORIOS("Relatórios",         "/relatorios",          "file-text",           false, false, true,  false, MenuSection.GESTAO,  List.of("/relatorios", "/relatorio")),
    CONFIG_CLINICA("Config. Clínica",  "/configuracoes/clinica", "building-2",      false, false, true,  false, MenuSection.ADMIN,   List.of("/configuracoes/clinica", "/configuracao/clinica")),
    USUARIOS("Usuários",             "/usuarios",            "users",               false, false, true,  false, MenuSection.ADMIN,   List.of("/usuarios")),
    FUNCOES_USUARIOS("Funções de Usuários", "/funcoes-usuarios", "sliders-horizontal", false, false, true, false, MenuSection.ADMIN, List.of("/funcoes-usuarios")),
    CONFIG_EMAIL("Config. E-mail",   "/configuracoes/email", "mail-check",          false, false, true,  false, MenuSection.ADMIN,   List.of("/configuracoes/email", "/configuracao/email")),
    AUDITORIA("Auditoria",           "/auditoria",           "shield-check",        false, false, true,  false, MenuSection.ADMIN,   List.of("/auditoria")),
    AGENDAMENTO_ONLINE("Agendamento Online", "/agendamento-online", "external-link", true, true, true,  false, MenuSection.NONE,   List.of("/agendamento-online")),
    GESTOR("Painel Gestor",            "/gestor",              "gauge",              false, false, false, false, MenuSection.PLATAFORMA, List.of("/gestor/dashboard", "/gestor/notificacoes")),
    GESTOR_EMPRESAS("Empresas",          "/gestor/empresas",     "building",           false, false, false, false, MenuSection.PLATAFORMA, List.of("/gestor/empresas")),
    GESTOR_CANCELAMENTOS("Cancelamentos", "/gestor/cancelamentos", "user-x", false, false, false, false, MenuSection.PLATAFORMA, List.of("/gestor/cancelamentos")),
    GESTOR_SUPORTE("Suporte",            "/gestor/suporte",      "life-buoy",          false, false, false, false, MenuSection.PLATAFORMA, List.of("/gestor/suporte")),
    GESTOR_AUDITORIA("Auditoria global", "/gestor/auditoria",    "shield-check",       false, false, false, false, MenuSection.PLATAFORMA, List.of("/gestor/auditoria")),
    GESTOR_CONFIGURACOES("Configurações globais", "/gestor/configuracoes", "settings", false, false, false, false, MenuSection.PLATAFORMA, List.of("/gestor/configuracoes"));

    public enum MenuSection {
        NONE(null),
        AGENDA("Agenda"),
        CLINICO("Clínico"),
        GESTAO("Gestão"),
        ADMIN("Administração"),
        PLATAFORMA("Plataforma");

        private final String label;

        MenuSection(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public boolean hasLabel() {
            return label != null;
        }
    }

    private final String label;
    private final String href;
    private final String icon;
    private final boolean footer;
    private final boolean external;
    private final boolean configuravelParaNaoAdmin;
    private final boolean adminOnly;
    private final MenuSection section;
    private final List<String> pathPrefixes;

    AppMenuItem(
            String label,
            String href,
            String icon,
            boolean footer,
            boolean external,
            boolean configuravelParaNaoAdmin,
            boolean adminOnly,
            MenuSection section,
            List<String> pathPrefixes
    ) {
        this.label = label;
        this.href = href;
        this.icon = icon;
        this.footer = footer;
        this.external = external;
        this.configuravelParaNaoAdmin = configuravelParaNaoAdmin;
        this.adminOnly = adminOnly;
        this.section = section;
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

    public MenuSection getSection() {
        return section;
    }

    /** Itens da área Gestor: exclusivos do SUPER_ADMIN e nunca liberáveis para clínicas. */
    public boolean isPlataforma() {
        return section == MenuSection.PLATAFORMA;
    }

    public boolean isRotaInterna() {
        return !external;
    }

    public boolean isDisponivelNaInterface() {
        return true;
    }

    public String getDescription() {
        switch (this) {
            case DASHBOARD:
                return "Mostra o painel geral com os principais indicadores e atalhos do sistema.";
            case AGENDA:
                return "Permite acompanhar o calendario mensal e organizar as sessoes do dia em uma unica tela.";
            case CALENDARIO:
                return "Alias legado do modulo unificado da agenda.";
            case BLOQUEIOS:
                return "Permite cadastrar indisponibilidades e bloqueios de horarios da agenda.";
            case SOLICITACOES:
                return "Centraliza pedidos de agendamento e outras solicitacoes recebidas.";
            case PACIENTES:
                return "Permite cadastrar, localizar e acompanhar os pacientes da clinica.";
            case PLANOS_SESSOES:
                return "Gerencia planos de tratamento e consumos de sessoes.";
            case PRONTUARIO:
                return "Da acesso ao registro clinico e evolucao dos atendimentos.";
            case FINANCEIRO:
                return "Controla lancamentos, recebimentos, despesas e fluxo financeiro.";
            case CONTAS_RECEBER:
                return "Exibe receitas pendentes e vencidas para acompanhamento de cobrancas.";
            case CONTAS_PAGAR:
                return "Exibe despesas pendentes e vencidas para controle de pagamentos.";
            case FATURAMENTO:
                return "Apresenta o relatorio de faturamento por periodo e agrupamentos financeiros.";
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
            case GESTOR:
                return "Painel global da plataforma com indicadores de todas as clinicas.";
            case GESTOR_EMPRESAS:
                return "Cadastro, status e periodo de teste das clinicas.";
            case GESTOR_CANCELAMENTOS:
                return "Solicitacoes de cancelamento enviadas pelas clinicas.";
            case GESTOR_SUPORTE:
                return "Solicitacoes de suporte recebidas pela plataforma.";
            case GESTOR_AUDITORIA:
                return "Auditoria das acoes administrativas da plataforma.";
            case GESTOR_CONFIGURACOES:
                return "Configuracoes globais da plataforma.";
            default:
                return label;
        }
    }

    public boolean matchesPath(String currentPath) {
        if (currentPath == null || currentPath.isBlank()) {
            return false;
        }
        if (this == GESTOR && "/gestor".equals(currentPath)) {
            return true;
        }
        if (this == FINANCEIRO) {
            return "/financeiro".equals(currentPath) || currentPath.startsWith("/financeiro/lancamentos");
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
                AGENDA,
                SOLICITACOES,
                PACIENTES,
                PLANOS_SESSOES,
                PRONTUARIO,
                FINANCEIRO,
                CONTAS_RECEBER,
                CONTAS_PAGAR,
                RELATORIOS,
                CONFIG_CLINICA,
                BLOQUEIOS,
                USUARIOS,
                FUNCOES_USUARIOS,
                CONFIG_EMAIL,
                AUDITORIA
        );
    }

    public static List<AppMenuItem> sidebarPlataforma() {
        return List.of(
                GESTOR,
                GESTOR_EMPRESAS,
                GESTOR_CANCELAMENTOS,
                GESTOR_SUPORTE,
                GESTOR_AUDITORIA,
                GESTOR_CONFIGURACOES,
                CONFIG_EMAIL
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
                .filter(AppMenuItem::isDisponivelNaInterface)
                .toList();
    }

    private static boolean matchesPrefix(String currentPath, String prefix) {
        if ("/".equals(prefix)) {
            return "/".equals(currentPath);
        }
        return currentPath.equals(prefix) || currentPath.startsWith(prefix + "/");
    }
}
