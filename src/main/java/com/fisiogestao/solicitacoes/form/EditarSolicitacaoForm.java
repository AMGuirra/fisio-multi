package com.fisiogestao.solicitacoes.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class EditarSolicitacaoForm {

    @NotBlank(message = "Informe o nome do paciente.")
    @Size(max = 160)
    private String nomePaciente;

    @NotBlank(message = "Informe o telefone.")
    @Size(max = 20)
    private String telefone;

    @Size(max = 120)
    private String email;

    @NotNull(message = "Informe a data.")
    private LocalDate data;

    @NotNull(message = "Informe o horário.")
    private LocalTime horario;

    @NotBlank(message = "Informe o local de atendimento.")
    @Size(max = 20)
    private String localAtendimento;

    @Size(max = 2000)
    private String observacoes;

    @Size(max = 255)
    private String motivo;
}
