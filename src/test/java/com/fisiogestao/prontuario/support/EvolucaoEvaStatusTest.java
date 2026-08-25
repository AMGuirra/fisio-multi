package com.fisiogestao.prontuario.support;

import com.fisiogestao.prontuario.ProntuarioSoapEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EvolucaoEvaStatusTest {

    @Test
    void calculaStatusPulandoEvolucoesSemEva() {
        ProntuarioSoapEntity inicial = evolucao(5);
        ProntuarioSoapEntity melhora = evolucao(3);
        ProntuarioSoapEntity semEva = evolucao(null);
        ProntuarioSoapEntity moderado = evolucao(3);
        ProntuarioSoapEntity piora = evolucao(6);

        var status = EvolucaoEvaStatus.calcular(List.of(inicial, melhora, semEva, moderado, piora));

        assertThat(status.get(inicial.getId())).isEqualTo(EvolucaoEvaStatus.INICIAL);
        assertThat(status.get(melhora.getId())).isEqualTo(EvolucaoEvaStatus.MELHORA);
        assertThat(status.get(semEva.getId())).isEqualTo(EvolucaoEvaStatus.INDEFINIDO);
        assertThat(status.get(moderado.getId())).isEqualTo(EvolucaoEvaStatus.MODERADO);
        assertThat(status.get(piora.getId())).isEqualTo(EvolucaoEvaStatus.PIORA);
    }

    private ProntuarioSoapEntity evolucao(Integer eva) {
        ProntuarioSoapEntity item = new ProntuarioSoapEntity();
        item.setId(UUID.randomUUID());
        item.setEscalaEvaDor(eva);
        return item;
    }
}
