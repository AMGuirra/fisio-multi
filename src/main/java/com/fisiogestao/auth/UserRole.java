package com.fisiogestao.auth;

import java.util.Arrays;
import java.util.List;

public enum UserRole {
    SUPER_ADMIN("Superadministrador"),
    ADMIN_CLINICA("Administrador da clínica"),
    FISIOTERAPEUTA("Fisioterapeuta"),
    RECEPCAO("Recepção"),
    ASSESSORIA("Assessoria"),
    FINANCEIRO("Financeiro");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        switch (this) {
            case SUPER_ADMIN:
                return "Usuario global da plataforma: acessa apenas a area Gestor, sem vinculo com clinicas.";
            case ADMIN_CLINICA:
                return "Tem acesso total a clinica e pode administrar usuarios, configuracoes e permissoes.";
            case FISIOTERAPEUTA:
                return "Perfil voltado ao atendimento clinico, evolucao e acompanhamento dos pacientes.";
            case RECEPCAO:
                return "Perfil focado em agenda, atendimento inicial e suporte operacional da clinica.";
            case ASSESSORIA:
                return "Perfil de apoio para acompanhar demandas, solicitacoes e atividades administrativas.";
            case FINANCEIRO:
                return "Perfil responsavel por cobrancas, lancamentos e acompanhamento financeiro.";
            default:
                return label;
        }
    }

    /** Administrador da clínica: acesso total aos módulos da própria empresa. */
    public boolean isAdministrador() {
        return this == ADMIN_CLINICA;
    }

    public boolean isSuperAdmin() {
        return this == SUPER_ADMIN;
    }

    /** Papéis que pertencem a uma clínica (todos, exceto o superadmin da plataforma). */
    public static List<UserRole> papeisDeClinica() {
        return Arrays.stream(values())
                .filter(role -> !role.isSuperAdmin())
                .toList();
    }

    public static List<UserRole> configuraveisPeloAdministrador() {
        return Arrays.stream(values())
                .filter(role -> !role.isAdministrador() && !role.isSuperAdmin())
                .toList();
    }
}
