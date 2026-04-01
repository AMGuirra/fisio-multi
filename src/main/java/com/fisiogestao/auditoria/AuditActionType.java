package com.fisiogestao.auditoria;

public enum AuditActionType {
    CREATE("Inclusão", "status-success", false),
    UPDATE("Edição", "status-info", false),
    DELETE_LOGICAL("Exclusão lógica", "status-danger", true),
    DELETE_PHYSICAL("Exclusão física", "status-danger", true),
    CANCEL("Cancelamento", "status-danger", true),
    CONFIRM("Confirmação", "status-success", false),
    REJECT("Recusa", "status-danger", true),
    RESCHEDULE("Reagendamento", "status-warning", true),
    COMPLETE("Conclusão", "status-success", false),
    NO_SHOW("Falta", "status-danger", true),
    INACTIVATE("Inativação", "status-warning", true),
    ACTIVATE("Reativação", "status-success", false),
    RESTORE("Restauração", "status-success", true),
    EXPORT_PDF("Exportação PDF", "status-info", false),
    EXPORT_EXCEL("Exportação Excel", "status-info", false),
    SEND_EMAIL("Envio de e-mail", "status-info", false),
    LOGIN("Login", "status-info", false),
    LOGOUT("Logout", "status-info", false),
    RESET_PASSWORD("Reset de senha", "status-warning", true),
    MARK_AS_PAID("Baixa financeira", "status-success", true);

    private final String label;
    private final String badgeClass;
    private final boolean critical;

    AuditActionType(String label, String badgeClass, boolean critical) {
        this.label = label;
        this.badgeClass = badgeClass;
        this.critical = critical;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public boolean isCritical() {
        return critical;
    }
}
