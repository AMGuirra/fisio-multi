package com.fisiogestao.pacientes.dto;

public record SessaoHistoricoDTO(
        String descricao,
        String dataHoraFormatada,
        boolean realizada,
        String tipoAtendimento,
        String observacoes,
        Integer feedbackEva,
        String feedbackObs
) {
}
