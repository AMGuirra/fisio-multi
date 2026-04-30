package com.fisiogestao.agenda.dto;

import com.fisiogestao.agenda.DiaSemana;
import com.fisiogestao.agenda.Frequencia;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RecorrenciaRequestDto {

    private Frequencia frequencia = Frequencia.SEMANAL;

    private List<DiaSemana> diasSemana = new ArrayList<>();

    private Integer intervalo = 1;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFim;

    private Integer maxOcorrencias;
}
