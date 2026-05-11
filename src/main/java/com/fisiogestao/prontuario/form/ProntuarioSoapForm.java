package com.fisiogestao.prontuario.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    // --- Campos SOAP ---

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

    // --- Campos específicos de fisioterapia ---

    /** Escala EVA de dor (0–10). Opcional. */
    @Min(value = 0, message = "A escala EVA deve ser entre 0 e 10.")
    @Max(value = 10, message = "A escala EVA deve ser entre 0 e 10.")
    private Integer escalaEvaDor;

    /** Região corporal principal tratada. */
    @Size(max = 120, message = "Região corporal deve ter no máximo 120 caracteres.")
    private String regiaoCorporal;

    /** Técnicas e recursos utilizados na sessão. */
    @Size(max = 2000, message = "Técnicas utilizadas devem ter no máximo 2000 caracteres.")
    private String tecnicasUtilizadas;

    /** Observações de evolução comparadas com sessões anteriores. */
    @Size(max = 3000, message = "Observações de evolução devem ter no máximo 3000 caracteres.")
    private String obsEvolucao;
}
