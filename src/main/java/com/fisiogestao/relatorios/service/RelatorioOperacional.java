package com.fisiogestao.relatorios.service;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.financeiro.LancamentoFinanceiroEntity;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.relatorios.form.FiltroRelatorioForm;
import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;

import java.math.BigDecimal;
import java.util.List;

public record RelatorioOperacional(
        FiltroRelatorioForm filtro,
        List<ClienteEntity> pacientes,
        List<SessaoEntity> sessoes,
        List<LancamentoFinanceiroEntity> lancamentos,
        List<SolicitacaoAgendamentoEntity> solicitacoes,
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        RelatorioAnalytics analytics
) {
    public BigDecimal saldoFinanceiro() {
        return totalReceitas.subtract(totalDespesas);
    }
}
