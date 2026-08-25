package com.fisiogestao.prontuario.support;

import com.fisiogestao.prontuario.ProntuarioSoapEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Status visual derivado da comparação da EVA com a última evolução comparável. */
public enum EvolucaoEvaStatus {
    MELHORA("Melhora", "status-success"),
    MODERADO("Moderado", "status-warning"),
    PIORA("Piora", "status-danger"),
    INICIAL("Inicial", "status-info"),
    INDEFINIDO("EVA não informada", null);

    private final String label;
    private final String cssClass;

    EvolucaoEvaStatus(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    public String getLabel() { return label; }
    public String getCssClass() { return cssClass; }

    public static Map<UUID, EvolucaoEvaStatus> calcular(List<ProntuarioSoapEntity> evolucoesCronologicas) {
        Map<UUID, EvolucaoEvaStatus> resultado = new LinkedHashMap<>();
        Integer ultimoEva = null;
        for (ProntuarioSoapEntity evolucao : evolucoesCronologicas) {
            Integer eva = evolucao.getEscalaEvaDor();
            EvolucaoEvaStatus status;
            if (eva == null) {
                status = INDEFINIDO;
            } else if (ultimoEva == null) {
                status = INICIAL;
                ultimoEva = eva;
            } else if (eva < ultimoEva) {
                status = MELHORA;
                ultimoEva = eva;
            } else if (eva.equals(ultimoEva)) {
                status = MODERADO;
                ultimoEva = eva;
            } else {
                status = PIORA;
                ultimoEva = eva;
            }
            if (evolucao.getId() != null) resultado.put(evolucao.getId(), status);
        }
        return resultado;
    }
}
