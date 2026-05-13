package com.fisiogestao.pacientes.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PlanoResumoDTO(
        UUID id,
        String tipoPlano,
        int totalSessoes,
        int sessoesUsadas,
        int sessoesRestantes,
        int percentualUso,
        String valorFormatado,
        String corBarra,
        LocalDate inicioVigencia,
        LocalDate fimVigencia,
        String status
) {
}
