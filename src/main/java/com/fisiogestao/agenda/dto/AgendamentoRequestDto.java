package com.fisiogestao.agenda.dto;

import com.fisiogestao.agenda.TipoAgendamento;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class AgendamentoRequestDto {

    @NotNull
    private UUID pacienteId;

    private TipoAgendamento tipo = TipoAgendamento.PACOTE;

    private UUID pacoteId;

    private BigDecimal valorAvulso;

    @NotNull
    private LocalDateTime dataHoraInicio;

    private Integer duracaoMinutos;

    private String tipoAtendimento;

    private String observacao;

    public boolean recorrente() {
        return false;
    }

    @AssertTrue(message = "Quando tipo = PACOTE, informe o pacoteId do plano de tratamento.")
    public boolean isPacoteObrigatorioQuandoUsarPlano() {
        return tipo != TipoAgendamento.PACOTE || pacoteId != null;
    }
}
