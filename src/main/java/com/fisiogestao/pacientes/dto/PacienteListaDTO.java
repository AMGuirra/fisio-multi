package com.fisiogestao.pacientes.dto;

import java.util.UUID;

public record PacienteListaDTO(
        UUID id,
        String nomeCompleto,
        String iniciais,
        String dataNascimentoFormatada,
        String corAvatar,
        String corTextoAvatar,
        boolean possuiPlanoAtivo,
        int sessoesUsadas,
        int sessoesTotal,
        int sessoesRestantes,
        String tipoPlano,
        String telefone,
        String email
) {
}
