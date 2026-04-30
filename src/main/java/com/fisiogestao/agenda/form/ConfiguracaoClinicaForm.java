package com.fisiogestao.agenda.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
public class ConfiguracaoClinicaForm {

    private String id;

    @NotBlank(message = "Informe o nome da clínica.")
    @Size(max = 150, message = "O nome da clínica deve ter até 150 caracteres.")
    private String nomeClinica;

    @NotEmpty(message = "Selecione ao menos um dia de funcionamento.")
    private Set<DayOfWeek> diasFuncionamento = new LinkedHashSet<>();

    @NotNull(message = "Informe o horário de abertura.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horarioAbertura;

    @NotNull(message = "Informe o horário de fechamento.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horarioFechamento;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horarioInicioAlmoco;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horarioFimAlmoco;

    @NotNull(message = "Informe a duração padrão da sessão.")
    @Min(value = 15, message = "A duração da sessão deve ser de pelo menos 15 minutos.")
    @Max(value = 240, message = "A duração da sessão deve ser de no máximo 240 minutos.")
    private Integer duracaoSessaoMinutos;

    @NotBlank(message = "Selecione o fuso horário da clínica.")
    @Size(max = 60, message = "O fuso horário deve ter até 60 caracteres.")
    private String timezoneId;

    @Size(max = 20, message = "O telefone/WhatsApp deve ter até 20 caracteres.")
    private String telefoneWhatsapp;

    @Email(message = "Informe um e-mail de contato válido.")
    @Size(max = 120, message = "O e-mail de contato deve ter até 120 caracteres.")
    private String emailContato;
}
