package com.fisiogestao.prontuario.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ProntuarioSoapForm {

    @NotNull(message = "Selecione o paciente.")
    private UUID clienteId;

    private UUID sessaoId;

    @NotBlank(message = "Preencha o campo Subjetivo.")
    @Size(max = 5000)
    private String subjetivo;

    @NotBlank(message = "Preencha o campo Objetivo.")
    @Size(max = 5000)
    private String objetivo;

    @NotBlank(message = "Preencha o campo Avaliação.")
    @Size(max = 5000)
    private String avaliacao;

    @NotBlank(message = "Preencha o campo Plano.")
    @Size(max = 5000)
    private String plano;
}
