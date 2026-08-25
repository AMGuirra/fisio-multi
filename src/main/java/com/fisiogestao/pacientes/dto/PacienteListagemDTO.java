package com.fisiogestao.pacientes.dto;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.PlanoSessaoEntity;

/** Dados já carregados para uma linha da listagem de pacientes. */
public record PacienteListagemDTO(
        ClienteEntity cliente,
        PlanoSessaoEntity planoAtivo,
        SessaoEntity ultimaSessao,
        SessaoEntity proximaSessao,
        String cpfFormatado,
        String telefoneFormatado
) {
}
