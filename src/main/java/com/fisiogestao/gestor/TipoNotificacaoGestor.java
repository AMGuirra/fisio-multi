package com.fisiogestao.gestor;

public enum TipoNotificacaoGestor {
    NOVO_CADASTRO("Novo cadastro"),
    SUPORTE("Solicitação de suporte"),
    CANCELAMENTO("Solicitação de cancelamento"),
    TESTE_VENCENDO("Teste próximo do fim"),
    TESTE_EXPIRADO("Teste expirado"),
    STATUS_EMPRESA("Status alterado");

    private final String label;

    TipoNotificacaoGestor(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
