package com.fisiogestao.financeiro.form;

import com.fisiogestao.financeiro.TipoLancamentoFinanceiro;
import jakarta.validation.constraints.DecimalMin;
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
public class LancamentoFinanceiroForm {

    @NotNull(message = "Selecione o tipo do lançamento.")
    private TipoLancamentoFinanceiro tipo;

    @NotBlank(message = "Informe a descrição.")
    @Size(max = 180)
    private String descricao;

    @NotBlank(message = "Informe a categoria.")
    @Size(max = 80)
    private String categoria;

    @NotNull(message = "Informe o valor.")
    @DecimalMin(value = "0.01", message = "O valor deve ser positivo.")
    private BigDecimal valor;

    @NotNull(message = "Informe a data de competência.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataCompetencia;
}
