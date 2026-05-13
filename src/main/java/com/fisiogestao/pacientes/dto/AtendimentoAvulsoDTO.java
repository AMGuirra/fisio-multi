package com.fisiogestao.pacientes.dto;

public record AtendimentoAvulsoDTO(
        String procedimento,
        String dataFormatada,
        String valorFormatado
) {
}
