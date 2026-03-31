package com.fisiogestao.auth;

public enum UserRole {
    ADMIN("Administrador"),
    ADMINISTRATIVO("Administrativo"),
    FISIOTERAPEUTA("Fisioterapeuta");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
