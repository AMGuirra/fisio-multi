package com.fisiogestao.pacientes.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class AtendimentoAvulsoFormDTO {

    private String procedimento;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataAtendimento;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horario;

    private BigDecimal valorCobrado;
    private String formaPagamento;
    private String observacoes;
}
