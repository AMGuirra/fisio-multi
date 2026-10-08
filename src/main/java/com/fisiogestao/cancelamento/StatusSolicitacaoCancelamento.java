package com.fisiogestao.cancelamento;

public enum StatusSolicitacaoCancelamento {
    PENDENTE("Pendente"),
    EM_ANALISE("Em análise"),
    CANCELAMENTO_AGENDADO("Cancelamento agendado"),
    CANCELADA("Cancelada"),
    RECUSADA("Recusada");

    private final String label;

    StatusSolicitacaoCancelamento(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isEncerrada() {
        return this == CANCELADA || this == RECUSADA;
    }
}
