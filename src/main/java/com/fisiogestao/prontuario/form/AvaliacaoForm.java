package com.fisiogestao.prontuario.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Registro clínico principal do paciente (Avaliação), em texto livre.
 */
@Getter
@Setter
public class AvaliacaoForm {

    @NotNull(message = "Selecione o paciente.")
    private UUID clienteId;

    @Size(max = 8000, message = "A avaliação deve ter no máximo 8000 caracteres.")
    private String texto;
}
