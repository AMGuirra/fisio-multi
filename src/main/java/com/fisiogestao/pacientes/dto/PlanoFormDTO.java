package com.fisiogestao.pacientes.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PlanoFormDTO {

    private String tipoPlano;
    private int totalSessoes;
    private BigDecimal valorTotal;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate inicioVigencia;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fimVigencia;

    private String status;
}
