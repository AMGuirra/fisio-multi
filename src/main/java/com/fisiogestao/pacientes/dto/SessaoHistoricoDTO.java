package com.fisiogestao.pacientes.dto;

import java.util.UUID;

public record SessaoHistoricoDTO(
        UUID id,
        String descricao,
        String dataHoraFormatada,
        boolean realizada,
        String tipoAtendimento,
        String observacoes,
        Integer feedbackEva,
        String feedbackObs
) {
}
