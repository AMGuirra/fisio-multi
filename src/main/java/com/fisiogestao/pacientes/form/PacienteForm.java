package com.fisiogestao.pacientes.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class PacienteForm {

    @NotBlank(message = "Informe o nome do paciente.")
    @Size(max = 160)
    private String nomeCompleto;

    @Size(max = 20)
    private String telefone;

    @Email(message = "Informe um e-mail válido.")
    @Size(max = 120)
    private String email;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataNascimento;

    @Size(max = 2000)
    private String observacoes;
}
