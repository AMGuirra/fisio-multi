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

    public boolean isAdministrador() {
        return this == ADMIN;
    }

    public static List<UserRole> configuraveisPeloAdministrador() {
        return Arrays.stream(values())
                .filter(role -> !role.isAdministrador())
                .toList();
    }
}
