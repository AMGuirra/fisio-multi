package com.fisiogestao.relatorios.service;

import java.util.List;

public record RelatorioAnalytics(
        String periodoAtualLabel,
        String periodoAnteriorLabel,
        List<Highlight> destaques,
        List<MetricCard> metricas,
        List<ComparisonRow> comparativos,
        List<AgendaDailyPoint> agendaDiaria,
        List<FinanceDailyPoint> financeiroDiario,
        DonutChart graficoSessoes,
        DonutChart graficoSolicitacoes,
        DonutChart graficoFinanceiro,
        List<PatientRankingRow> rankingPacientes,
        List<FinanceCategoryRow> categoriasFinanceiras,
        List<RecentPatientRow> pacientesRecentes
) {

    public record Highlight(
            String label,
            String value,
            String tone
    ) {
    }

    public record MetricCard(
            String icon,
            String title,
            String value,
            String detail,
            String changeLabel,
            String accent,
            String changeTone
    ) {
    }

    public record ComparisonRow(
            String indicador,
            String atual,
            String anterior,
            String variacao,
            String tone
    ) {
    }

    public record AgendaDailyPoint(
            String shortLabel,
            String fullLabel,
            String totalLabel,
            String detalhamentoLabel,
            double agendadasPercent,
            double realizadasPercent,
            double canceladasPercent
    ) {
    }

    public record FinanceDailyPoint(
            String shortLabel,
            String fullLabel,
            String receitasLabel,
            String despesasLabel,
            String saldoLabel,
            double receitasPercent,
            double despesasPercent,
            String saldoTone
    ) {
    }

    public record DonutChart(
            String title,
            String centerValue,
            String centerLabel,
            String gradientStyle,
            List<LegendItem> itens,
            boolean empty
    ) {
    }

    public record LegendItem(
            String label,
            String value,
            String percentLabel,
            String color
    ) {
    }

    public record PatientRankingRow(
            String nome,
            String totalSessoes,
            String sessoesRealizadas,
            String faturamento
    ) {
    }

    public record FinanceCategoryRow(
            String categoria,
            String receitas,
            String despesas,
            String saldo,
            String saldoTone
    ) {
    }

    public record RecentPatientRow(
            String nome,
            String cadastroEm,
            String contato,
            String email
    ) {
    }
}
