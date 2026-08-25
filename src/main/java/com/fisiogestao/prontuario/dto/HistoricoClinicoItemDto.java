package com.fisiogestao.prontuario.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HistoricoClinicoItemDto(
        UUID id,
        TipoHistoricoClinico tipo,
        OffsetDateTime dataHora,
        String titulo,
        String resumo,
        String autor,
        String iconeLucide
) { }
