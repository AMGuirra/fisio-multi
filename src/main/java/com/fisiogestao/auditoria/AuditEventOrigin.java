package com.fisiogestao.auditoria;

public enum AuditEventOrigin {
    MANUAL("Manual"),
    SISTEMA("Sistema"),
    INTEGRACAO("Integração"),
    ONLINE("Online"),
    IMPORTACAO("Importação");

    private final String label;

    AuditEventOrigin(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
