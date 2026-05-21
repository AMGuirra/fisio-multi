package com.fisiogestao.financeiro;

public enum MetodoPagamento {
    PIX("PIX"),
    DINHEIRO("Dinheiro"),
    CARTAO_CREDITO("Cartão de crédito"),
    CARTAO_DEBITO("Cartão de débito"),
    TRANSFERENCIA("Transferência bancária"),
    CHEQUE("Cheque"),
    CONVENIO("Convênio"),
    OUTRO("Outro");

    private final String label;

    MetodoPagamento(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
