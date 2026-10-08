package com.fisiogestao.suporte;

public enum StatusSolicitacaoSuporte {
    PENDENTE("Pendente"),
    EM_ANALISE("Em análise"),
    RESPONDIDA("Respondida"),
    ENCERRADA("Encerrada");

    private final String label;

    StatusSolicitacaoSuporte(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
