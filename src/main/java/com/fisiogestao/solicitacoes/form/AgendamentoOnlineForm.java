package com.fisiogestao.solicitacoes.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class AgendamentoOnlineForm {

    @NotNull(message = "Selecione a data.")
    @FutureOrPresent(message = "Escolha uma data atual ou futura.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate data;

    @NotBlank(message = "Selecione um horário.")
    private String horario;

    @NotBlank(message = "Informe o nome do paciente.")
    @Size(max = 160)
    private String nomePaciente;

    @NotBlank(message = "Informe o telefone/WhatsApp.")
    @Size(max = 20)
    private String telefone;

    @NotBlank(message = "Informe o CPF.")
    @Size(min = 11, max = 14, message = "Informe um CPF válido.")
    private String cpf;

    @Email(message = "Informe um e-mail válido.")
    @Size(max = 120)
    private String email;

    @NotBlank(message = "Selecione onde deseja o atendimento.")
    private String localAtendimento;

    @Size(max = 9, message = "Informe um CEP válido.")
    private String cep;

    @Size(max = 160)
    private String rua;

    @Size(max = 20)
    private String numero;

    @Size(max = 120)
    private String bairro;

    @Size(max = 120)
    private String cidade;

    @Size(max = 2)
    private String estado;

    @Size(max = 2000)
    private String observacoes;
}
