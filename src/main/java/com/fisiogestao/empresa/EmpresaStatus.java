package com.fisiogestao.empresa;

public enum EmpresaStatus {
    ATIVO("Ativo", "Acesso liberado por tempo indeterminado."),
    TESTE("Em teste", "Acesso liberado até a data final do período de teste."),
    BLOQUEADO("Bloqueado", "Acesso negado até desbloqueio."),
    INATIVO("Inativo", "Acesso negado; a clínica foi inativada.");

    private final String label;
    private final String descricao;

    EmpresaStatus(String label, String descricao) {
        this.label = label;
        this.descricao = descricao;
    }

    public String getLabel() {
        return label;
    }

    public String getDescricao() {
        return descricao;
    }
}
