package com.fisiogestao.auth;

import java.util.Arrays;
import java.util.List;

public enum UserRole {
    ADMIN("Administrador"),
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
            case ADMIN:
                return "Tem acesso total ao sistema e pode administrar usuarios, configuracoes e permissoes.";
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

    public boolean isAdministrador() {
        return this == ADMIN;
    }

    public static List<UserRole> configuraveisPeloAdministrador() {
        return Arrays.stream(values())
                .filter(role -> !role.isAdministrador())
                .toList();
    }
}
