package com.fisiogestao.pacientes.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record PlanoListItemDTO(
        UUID id,
        String nomePlano,
        String status,
        LocalDate dataInicio,
        LocalDate dataValidade,
        int sessoesContratadas,
        int sessoesConsumidas,
        int sessoesRestantes,
        String valorFormatado
) {
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String dataInicioFormatada() {
        return dataInicio != null ? dataInicio.format(DATA_BR) : "";
    }

    public String dataValidadeFormatada() {
        return dataValidade != null ? dataValidade.format(DATA_BR) : "Sem vencimento";
    }
}
