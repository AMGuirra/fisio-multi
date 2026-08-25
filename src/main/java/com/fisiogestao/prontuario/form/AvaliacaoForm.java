package com.fisiogestao.prontuario.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Registro clínico principal estruturado do paciente.
 */
@Getter
@Setter
public class AvaliacaoForm {

    @NotNull(message = "Selecione o paciente.")
    private UUID clienteId;

    @Size(max = 3000) private String queixaPrincipal;
    @Size(max = 4000) private String avaliacaoFisica;
    @Size(max = 200) private String regiaoCorporal;
    @Size(max = 4000) private String avaliacaoFuncional;
    @Size(max = 2000) private String objetivosTratamento;
    @Size(max = 3000) private String planoTerapeutico;
    @Size(max = 3000) private String observacoesAvaliacao;
}
