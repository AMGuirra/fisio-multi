package com.fisiogestao.pacientes.dto;

import java.util.List;
import java.util.UUID;

public record PacienteExclusaoPendencias(
        UUID pacienteId,
        String nomePaciente,
        List<Pendencia> pendencias,
        String destinoPrimario
) {

    public PacienteExclusaoPendencias {
        pendencias = pendencias == null ? List.of() : List.copyOf(pendencias);
    }

    public boolean bloqueada() {
        return !pendencias.isEmpty();
    }

    public String mensagemPrincipal() {
        return bloqueada()
                ? pendencias.get(0).mensagemBloqueio()
                : null;
    }

    public record Pendencia(
            String tipo,
            String rotulo,
            String descricao,
            String destino,
            String mensagemBloqueio
    ) {
    }
}
