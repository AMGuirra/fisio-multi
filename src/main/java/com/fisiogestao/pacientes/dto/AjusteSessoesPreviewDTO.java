package com.fisiogestao.pacientes.dto;

import java.util.UUID;

public record AjusteSessoesPreviewDTO(
        UUID pacienteId,
        UUID planoId,
        int consumoAtual,
        int totalContratado,
        int saldoDisponivel,
        int sessoesPendentes,
        int sessoesAjustaveis,
        int sessoesExcedentes,
        boolean sugerirNovoPlano
) {
    public boolean possuiPendencias() {
        return sessoesPendentes > 0;
    }
}
