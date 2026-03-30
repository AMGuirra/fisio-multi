package com.fisiogestao.pacientes.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PacoteSessaoForm {

    @NotBlank(message = "Informe o nome do pacote.")
    @Size(max = 120)
    private String nomePacote;

    @NotNull(message = "Informe a quantidade de sessões.")
    @Min(value = 1, message = "O pacote precisa ter ao menos uma sessão.")
    private Integer sessoesContratadas;

    @NotNull(message = "Informe o valor do pacote.")
    @DecimalMin(value = "0.01", message = "O valor do pacote deve ser positivo.")
    private BigDecimal valorTotal;

    @NotNull(message = "Informe a data inicial do pacote.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataValidade;
}
