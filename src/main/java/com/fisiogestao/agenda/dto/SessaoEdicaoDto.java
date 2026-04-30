package com.fisiogestao.agenda.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record SessaoEdicaoDto(
    UUID id,
    UUID clienteId,
    String clienteNome,
    UUID pacoteSessaoId,
    String pacoteSessaoNome,
    LocalDate data,
    LocalTime horario,
    String tipoAtendimento,
    String tipoAgendamento,
    BigDecimal valorAvulso,
    boolean recorrente,
    String observacoes,
    String status
) {
    public static SessaoEdicaoDto of(
            UUID id,
            UUID clienteId,
            String clienteNome,
            UUID pacoteSessaoId,
            String pacoteSessaoNome,
            LocalDate data,
            LocalTime horario,
            String tipoAtendimento,
            String tipoAgendamento,
            BigDecimal valorAvulso,
            boolean recorrente,
            String observacoes,
            String status
    ) {
        return new SessaoEdicaoDto(
                id, clienteId, clienteNome, pacoteSessaoId,
                pacoteSessaoNome, data, horario, tipoAtendimento, tipoAgendamento, valorAvulso, recorrente, observacoes, status
        );
    }
}
