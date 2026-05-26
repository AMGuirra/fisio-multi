package com.fisiogestao.pacientes.dto;

import java.util.UUID;

public record ReconciliacaoPreviewDTO(
        UUID planoId,
        int sessoesConsumidasAtual,
        int sessoesConsumidasCorreto,
        int sessoesNaoRegistradas,
        boolean possuiDiscrepancia
) {}
