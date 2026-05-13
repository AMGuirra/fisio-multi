package com.fisiogestao.pacientes.dto;

public record ResumoAvulsoDTO(
        String procedimento,
        String dataHora,
        String formaPagamento,
        String valorFormatado
) {
}
