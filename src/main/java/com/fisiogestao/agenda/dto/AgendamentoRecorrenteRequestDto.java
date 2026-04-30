package com.fisiogestao.agenda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgendamentoRecorrenteRequestDto extends AgendamentoRequestDto {

    @Valid
    @NotNull
    private RecorrenciaRequestDto recorrencia = new RecorrenciaRequestDto();

    @Override
    public boolean recorrente() {
        return true;
    }
}
