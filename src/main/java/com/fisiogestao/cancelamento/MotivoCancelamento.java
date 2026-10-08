package com.fisiogestao.cancelamento;

public enum MotivoCancelamento {
    CUSTO("Custo"),
    NAO_ATENDE_NECESSIDADES("O sistema não atende às necessidades"),
    DIFICULDADE_DE_USO("Dificuldade de uso"),
    ENCERRAMENTO_ATIVIDADES("Encerramento das atividades"),
    OUTRO_SISTEMA("Mudança para outro sistema"),
    OUTRO("Outro motivo");

    private final String label;

    MotivoCancelamento(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
