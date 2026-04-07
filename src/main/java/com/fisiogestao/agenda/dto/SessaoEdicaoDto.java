package com.fisiogestao.agenda.dto;

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
            String observacoes,
            String status
    ) {
        return new SessaoEdicaoDto(
                id, clienteId, clienteNome, pacoteSessaoId,
                pacoteSessaoNome, data, horario, tipoAtendimento, observacoes, status
        );
    }
}