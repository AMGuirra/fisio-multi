package com.fisiogestao.agenda.form;

import com.fisiogestao.agenda.TipoAgendamento;
import com.fisiogestao.agenda.dto.RecorrenciaRequestDto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
public class AgendaManualForm {

    @NotNull(message = "Selecione o paciente.")
    private UUID clienteId;

    private UUID pacoteSessaoId;

    @NotNull(message = "Informe a data.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate data;

    @NotNull(message = "Informe o horário.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horario;

    @Size(max = 80)
    private String tipoAtendimento;

    private TipoAgendamento tipoAgendamento = TipoAgendamento.PACOTE;

    private BigDecimal valorAvulso;

    private Integer duracaoMinutos;

    private Boolean recorrente = Boolean.FALSE;

    private RecorrenciaRequestDto recorrencia = new RecorrenciaRequestDto();

    @Size(max = 2000)
    private String observacoes;
}
