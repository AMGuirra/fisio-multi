package com.fisiogestao.status;

public enum SystemStatusType {
    ONLINE("Online", "status-online"),
    MANUTENCAO("Em Manutenção", "status-manutencao"),
    INSTABILIDADE("Instabilidade", "status-instabilidade");

    private final String label;
    private final String cssClass;

    SystemStatusType(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    public String getLabel() {
        return label;
    }

    public String getCssClass() {
        return cssClass;
    }
}