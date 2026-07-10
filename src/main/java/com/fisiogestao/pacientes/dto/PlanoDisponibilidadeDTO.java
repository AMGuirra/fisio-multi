package com.fisiogestao.pacientes.dto;

import com.fisiogestao.pacientes.PlanoSessaoStatus;

import java.time.LocalDate;
import java.util.UUID;

public record PlanoDisponibilidadeDTO(
        UUID planoId,
        UUID clienteId,
        String nomePlano,
        PlanoSessaoStatus status,
        LocalDate dataInicio,
        LocalDate dataValidade,
        int sessoesContratadas,
        int sessoesConsumidas,
        int sessoesRestantes,
        int sessoesAgendadas,
        int sessoesLivresParaAgenda,
        boolean disponivelParaUso,
        boolean disponivelParaAgenda,
        String motivoIndisponibilidade
) {
}
