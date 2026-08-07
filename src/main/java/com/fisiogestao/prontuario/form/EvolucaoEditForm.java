package com.fisiogestao.prontuario.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Edição do texto de uma entrada já registrada no diário clínico (Evolução).
 */
@Getter
@Setter
public class EvolucaoEditForm {

    @NotNull(message = "Selecione o paciente.")
    private UUID clienteId;

    @Size(max = 8000, message = "A evolução deve ter no máximo 8000 caracteres.")
    private String texto;
}
