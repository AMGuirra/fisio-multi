package com.fisiogestao.relatorios.service;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.financeiro.LancamentoFinanceiroEntity;
import com.fisiogestao.financeiro.TipoLancamentoFinanceiro;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import com.fisiogestao.solicitacoes.SolicitacaoStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RelatorioAnalyticsBuilder {

    private static final String TONE_SUCCESS = "success";
    private static final String TONE_INFO    = "info";
    private static final String TONE_WARNING = "warning";
    private static final String TONE_DANGER  = "danger";
    private static final String TONE_NEUTRAL = "neutral";

    public RelatorioAnalytics construir(
            PeriodoComparacao periodo,
            long totalPacientesAtivos,
            long novosPacientesAtual,
            long novosPacientesAnterior,
            List<ClienteEntity> pacientesRecentes,
            List<SessaoEntity> sessoes,
            List<SessaoEntity> sessoesAnterior,
            List<LancamentoFinanceiroEntity> lancamentos,
            List<LancamentoFinanceiroEntity> lancamentosAnterior,
            List<SolicitacaoAgendamentoEntity> solicitacoes,
            List<SolicitacaoAgendamentoEntity> solicitacoesAnterior,
            BigDecimal totalReceitas,
            BigDecimal totalDespesas
    ) {
        long sessoesRealizadasAtual    = contarSessoesPorStatus(sessoes, SessaoStatus.REALIZADA);
        long sessoesCanceladasAtual    = contarSessoesPorStatus(sessoes, SessaoStatus.CANCELADA);
        long sessoesRealizadasAnterior = contarSessoesPorStatus(sessoesAnterior, SessaoStatus.REALIZADA);
        double taxaConclusaoAtual      = percentual(sessoesRealizadasAtual, sessoes.size());
        double taxaConclusaoAnterior   = percentual(sessoesRealizadasAnterior, sessoesAnterior.size());

        BigDecimal totalReceitasAnterior = somarPorTipo(lancamentosAnterior, TipoLancamentoFinanceiro.RECEITA);
        BigDecimal totalDespesasAnterior = somarPorTipo(lancamentosAnterior, TipoLancamentoFinanceiro.DESPESA);
        BigDecimal saldoAtual   = totalReceitas.subtract(totalDespesas);
        BigDecimal saldoAnterior = totalReceitasAnterior.subtract(totalDespesasAnterior);

        long solicitacoesAprovadasAtual    = contarSolicitacoesPorStatus(solicitacoes, SolicitacaoStatus.APROVADA);
        long solicitacoesPendentesAtual    = contarSolicitacoesPorStatus(solicitacoes, SolicitacaoStatus.PENDENTE);
        long solicitacoesAprovadasAnterior = contarSolicitacoesPorStatus(solicitacoesAnterior, SolicitacaoStatus.APROVADA);
        double taxaAprovacaoAtual    = percentual(solicitacoesAprovadasAtual, solicitacoes.size());
        double taxaAprovacaoAnterior = percentual(solicitacoesAprovadasAnterior, solicitacoesAnterior.size());

        long quantidadeReceitas = lancamentos.stream()
                .filter(item -> item.getTipo() == TipoLancamentoFinanceiro.RECEITA).count();
        BigDecimal ticketMedio = quantidadeReceitas == 0
                ? BigDecimal.ZERO
                : totalReceitas.divide(BigDecimal.valueOf(quantidadeReceitas), 2, RoundingMode.HALF_UP);

        ChangeDescriptor deltaNovosPacientes = descreverVariacao(novosPacientesAtual, novosPacientesAnterior, true);
        ChangeDescriptor deltaSessoes        = descreverVariacao(sessoes.size(), sessoesAnterior.size(), true);
        ChangeDescriptor deltaConclusao      = descreverVariacao(taxaConclusaoAtual, taxaConclusaoAnterior, true);
        ChangeDescriptor deltaReceitas       = descreverVariacao(totalReceitas, totalReceitasAnterior, true);
        ChangeDescriptor deltaSolicitacoes   = descreverVariacao(solicitacoes.size(), solicitacoesAnterior.size(), true);

        List<RelatorioAnalytics.Highlight> destaques = List.of(
                new RelatorioAnalytics.Highlight("Período", periodo.labelAtual(), TONE_NEUTRAL),
                new RelatorioAnalytics.Highlight("Média de sessões/dia",
                        RelatorioFormatUtil.formatNumero(sessoes.isEmpty() ? 0 : (double) sessoes.size() / periodo.totalDias(), 1),
                        TONE_INFO),
                new RelatorioAnalytics.Highlight("Ticket médio", RelatorioFormatUtil.formatCurrency(ticketMedio), TONE_SUCCESS),
                new RelatorioAnalytics.Highlight("Aprovação", RelatorioFormatUtil.formatPercent(taxaAprovacaoAtual),
                        taxaAprovacaoAtual >= 60 ? TONE_SUCCESS : TONE_WARNING)
        );

        List<RelatorioAnalytics.MetricCard> metricas = List.of(
                new RelatorioAnalytics.MetricCard("users-round", "Pacientes ativos",
                        RelatorioFormatUtil.formatInteiro(totalPacientesAtivos),
                        RelatorioFormatUtil.formatInteiro(novosPacientesAtual) + " novos no período",
                        "Base ativa da clínica", "teal", TONE_NEUTRAL),
                new RelatorioAnalytics.MetricCard("user-plus", "Novos pacientes",
                        RelatorioFormatUtil.formatInteiro(novosPacientesAtual),
                        "Cadastros no intervalo atual",
                        deltaNovosPacientes.label(), "blue", deltaNovosPacientes.tone()),
                new RelatorioAnalytics.MetricCard("calendar-range", "Sessões do período",
                        RelatorioFormatUtil.formatInteiro(sessoes.size()),
                        RelatorioFormatUtil.formatInteiro(sessoesRealizadasAtual) + " realizadas • " +
                                RelatorioFormatUtil.formatInteiro(sessoesCanceladasAtual) + " canceladas",
                        deltaSessoes.label(), "indigo", deltaSessoes.tone()),
                new RelatorioAnalytics.MetricCard("activity", "Taxa de conclusão",
                        RelatorioFormatUtil.formatPercent(taxaConclusaoAtual),
                        RelatorioFormatUtil.formatInteiro(sessoesRealizadasAtual) + " de " +
                                RelatorioFormatUtil.formatInteiro(sessoes.size()) + " sessões concluídas",
                        deltaConclusao.label(), "green", deltaConclusao.tone()),
                new RelatorioAnalytics.MetricCard("badge-dollar-sign", "Receita bruta",
                        RelatorioFormatUtil.formatCurrency(totalReceitas),
                        "Despesas " + RelatorioFormatUtil.formatCurrency(totalDespesas) +
                                " • saldo " + RelatorioFormatUtil.formatCurrency(saldoAtual),
                        deltaReceitas.label(), "amber", deltaReceitas.tone()),
                new RelatorioAnalytics.MetricCard("messages-square", "Solicitações",
                        RelatorioFormatUtil.formatInteiro(solicitacoes.size()),
                        RelatorioFormatUtil.formatInteiro(solicitacoesAprovadasAtual) + " aprovadas • " +
                                RelatorioFormatUtil.formatInteiro(solicitacoesPendentesAtual) + " pendentes",
                        deltaSolicitacoes.label(), "rose", deltaSolicitacoes.tone())
        );

        List<RelatorioAnalytics.ComparisonRow> comparativos = List.of(
                comparativo("Novos pacientes", novosPacientesAtual, novosPacientesAnterior, deltaNovosPacientes),
                comparativo("Sessões totais", sessoes.size(), sessoesAnterior.size(), deltaSessoes),
                comparativoLong("Sessões realizadas", sessoesRealizadasAtual, sessoesRealizadasAnterior,
                        descreverVariacao(sessoesRealizadasAtual, sessoesRealizadasAnterior, true)),
                new RelatorioAnalytics.ComparisonRow("Taxa de conclusão",
                        RelatorioFormatUtil.formatPercent(taxaConclusaoAtual),
                        RelatorioFormatUtil.formatPercent(taxaConclusaoAnterior),
                        deltaConclusao.label(), deltaConclusao.tone()),
                new RelatorioAnalytics.ComparisonRow("Receita bruta",
                        RelatorioFormatUtil.formatCurrency(totalReceitas),
                        RelatorioFormatUtil.formatCurrency(totalReceitasAnterior),
                        deltaReceitas.label(), deltaReceitas.tone()),
                new RelatorioAnalytics.ComparisonRow("Despesas",
                        RelatorioFormatUtil.formatCurrency(totalDespesas),
                        RelatorioFormatUtil.formatCurrency(totalDespesasAnterior),
                        descreverVariacao(totalDespesas, totalDespesasAnterior, false).label(),
                        descreverVariacao(totalDespesas, totalDespesasAnterior, false).tone()),
                new RelatorioAnalytics.ComparisonRow("Saldo do período",
                        RelatorioFormatUtil.formatCurrency(saldoAtual),
                        RelatorioFormatUtil.formatCurrency(saldoAnterior),
                        descreverVariacao(saldoAtual, saldoAnterior, true).label(),
                        descreverVariacao(saldoAtual, saldoAnterior, true).tone()),
                comparativo("Solicitações recebidas", solicitacoes.size(), solicitacoesAnterior.size(), deltaSolicitacoes),
                new RelatorioAnalytics.ComparisonRow("Taxa de aprovação",
                        RelatorioFormatUtil.formatPercent(taxaAprovacaoAtual),
                        RelatorioFormatUtil.formatPercent(taxaAprovacaoAnterior),
                        descreverVariacao(taxaAprovacaoAtual, taxaAprovacaoAnterior, true).label(),
                        descreverVariacao(taxaAprovacaoAtual, taxaAprovacaoAnterior, true).tone())
        );

        return new RelatorioAnalytics(
                periodo.labelAtual(),
                periodo.labelAnterior(),
                destaques,
                metricas,
                comparativos,
                construirAgendaDiaria(periodo, sessoes),
                construirFinanceiroDiario(periodo, lancamentos),
                construirGraficoSessoes(sessoes),
                construirGraficoSolicitacoes(solicitacoes),
                construirGraficoFinanceiro(totalReceitas, totalDespesas),
                construirRankingPacientes(sessoes, lancamentos),
                construirCategoriasFinanceiras(lancamentos),
                construirPacientesRecentes(pacientesRecentes)
        );
    }

    public RelatorioAnalytics vazia(PeriodoComparacao periodo) {
        RelatorioAnalytics.DonutChart semDados = new RelatorioAnalytics.DonutChart(
                "Sem dados", "0", "sem dados", "conic-gradient(#dce6e3 0 100%)", List.of(), true);
        return new RelatorioAnalytics(
                periodo.labelAtual(), periodo.labelAnterior(),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                semDados, semDados, semDados,
                List.of(), List.of(), List.of()
        );
    }

    // ── Agenda diária ──────────────────────────────────────────────────────────

    private List<RelatorioAnalytics.AgendaDailyPoint> construirAgendaDiaria(
            PeriodoComparacao periodo, List<SessaoEntity> sessoes) {
        var sessoesPorDia = sessoes.stream()
                .collect(Collectors.groupingBy(item -> RelatorioFormatUtil.paraLocalDate(item.getDataHoraInicio())));

        long maximo = periodo.datas().stream()
                .mapToLong(data -> sessoesPorDia.getOrDefault(data, List.of()).size())
                .max().orElse(0);
        if (maximo == 0) return List.of();

        List<RelatorioAnalytics.AgendaDailyPoint> itens = new ArrayList<>();
        for (LocalDate data : periodo.datas()) {
            List<SessaoEntity> doDia = sessoesPorDia.getOrDefault(data, List.of());
            long agendadas  = contarSessoesPorStatus(doDia, SessaoStatus.AGENDADA);
            long realizadas = contarSessoesPorStatus(doDia, SessaoStatus.REALIZADA);
            long canceladas = contarSessoesPorStatus(doDia, SessaoStatus.CANCELADA);
            itens.add(new RelatorioAnalytics.AgendaDailyPoint(
                    RelatorioFormatUtil.formatDateShort(data),
                    RelatorioFormatUtil.formatDateFull(data),
                    RelatorioFormatUtil.formatInteiro(doDia.size()),
                    RelatorioFormatUtil.formatInteiro(realizadas) + " realizadas • " +
                            RelatorioFormatUtil.formatInteiro(canceladas) + " canceladas",
                    percentualDoMaximo(agendadas, maximo),
                    percentualDoMaximo(realizadas, maximo),
                    percentualDoMaximo(canceladas, maximo)
            ));
        }
        return itens;
    }

    // ── Financeiro diário ──────────────────────────────────────────────────────

    private List<RelatorioAnalytics.FinanceDailyPoint> construirFinanceiroDiario(
            PeriodoComparacao periodo, List<LancamentoFinanceiroEntity> lancamentos) {
        var lancamentosPorDia = lancamentos.stream()
                .collect(Collectors.groupingBy(item -> RelatorioFormatUtil.paraLocalDate(item.getDataCompetencia())));

        BigDecimal maximo = periodo.datas().stream()
                .map(data -> somarAbsoluto(lancamentosPorDia.getOrDefault(data, List.of())))
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        if (maximo.compareTo(BigDecimal.ZERO) == 0) return List.of();

        List<RelatorioAnalytics.FinanceDailyPoint> itens = new ArrayList<>();
        for (LocalDate data : periodo.datas()) {
            List<LancamentoFinanceiroEntity> doDia = lancamentosPorDia.getOrDefault(data, List.of());
            BigDecimal receitas = somarPorTipo(doDia, TipoLancamentoFinanceiro.RECEITA);
            BigDecimal despesas = somarPorTipo(doDia, TipoLancamentoFinanceiro.DESPESA);
            BigDecimal saldo    = receitas.subtract(despesas);
            itens.add(new RelatorioAnalytics.FinanceDailyPoint(
                    RelatorioFormatUtil.formatDateShort(data),
                    RelatorioFormatUtil.formatDateFull(data),
                    RelatorioFormatUtil.formatCurrency(receitas),
                    RelatorioFormatUtil.formatCurrency(despesas),
                    RelatorioFormatUtil.formatCurrency(saldo),
                    percentualBigDecimal(receitas, maximo),
                    percentualBigDecimal(despesas, maximo),
                    RelatorioFormatUtil.toneBySign(saldo)
            ));
        }
        return itens;
    }

    // ── Gráficos ───────────────────────────────────────────────────────────────

    private RelatorioAnalytics.DonutChart construirGraficoSessoes(List<SessaoEntity> sessoes) {
        return construirGraficoRosca("Sessões por status", RelatorioFormatUtil.formatInteiro(sessoes.size()), "sessões",
                List.of(
                        new SliceInput("Agendadas",  BigDecimal.valueOf(contarSessoesPorStatus(sessoes, SessaoStatus.AGENDADA)),  "#56b8ff", false),
                        new SliceInput("Realizadas", BigDecimal.valueOf(contarSessoesPorStatus(sessoes, SessaoStatus.REALIZADA)), "#0f8f7a", false),
                        new SliceInput("Canceladas", BigDecimal.valueOf(contarSessoesPorStatus(sessoes, SessaoStatus.CANCELADA)), "#e36d6d", false)
                ));
    }

    private RelatorioAnalytics.DonutChart construirGraficoSolicitacoes(List<SolicitacaoAgendamentoEntity> solicitacoes) {
        return construirGraficoRosca("Solicitações por status", RelatorioFormatUtil.formatInteiro(solicitacoes.size()), "solicitações",
                List.of(
                        new SliceInput("Pendentes",  BigDecimal.valueOf(contarSolicitacoesPorStatus(solicitacoes, SolicitacaoStatus.PENDENTE)),  "#d19a10", false),
                        new SliceInput("Aprovadas",  BigDecimal.valueOf(contarSolicitacoesPorStatus(solicitacoes, SolicitacaoStatus.APROVADA)),  "#0f8f7a", false),
                        new SliceInput("Rejeitadas", BigDecimal.valueOf(contarSolicitacoesPorStatus(solicitacoes, SolicitacaoStatus.REJEITADA)), "#cf5b5b", false),
                        new SliceInput("Canceladas", BigDecimal.valueOf(contarSolicitacoesPorStatus(solicitacoes, SolicitacaoStatus.CANCELADA)), "#7b8794", false)
                ));
    }

    private RelatorioAnalytics.DonutChart construirGraficoFinanceiro(BigDecimal receitas, BigDecimal despesas) {
        return construirGraficoRosca("Movimento financeiro",
                RelatorioFormatUtil.formatCurrency(receitas.add(despesas)), "movimentado",
                List.of(
                        new SliceInput("Receitas", receitas, "#0f8f7a", true),
                        new SliceInput("Despesas", despesas, "#d55353", true)
                ));
    }

    private RelatorioAnalytics.DonutChart construirGraficoRosca(
            String titulo, String centroValor, String centroRotulo, List<SliceInput> entradas) {
        List<SliceInput> filtradas = entradas.stream()
                .filter(item -> item.valor().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        BigDecimal total = filtradas.stream().map(SliceInput::valor).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (filtradas.isEmpty() || total.compareTo(BigDecimal.ZERO) <= 0) {
            return new RelatorioAnalytics.DonutChart(titulo, "0", "sem dados",
                    "conic-gradient(#dce6e3 0 100%)",
                    List.of(new RelatorioAnalytics.LegendItem("Sem dados no período", "-", "-", "#dce6e3")),
                    true);
        }

        List<String> segmentos = new ArrayList<>();
        List<RelatorioAnalytics.LegendItem> itens = new ArrayList<>();
        BigDecimal acumulado = BigDecimal.ZERO;
        for (SliceInput entrada : filtradas) {
            double inicio = percentualBigDecimal(acumulado, total);
            acumulado = acumulado.add(entrada.valor());
            double fim = percentualBigDecimal(acumulado, total);
            double pct = percentualBigDecimal(entrada.valor(), total);
            segmentos.add(entrada.color() + " " + RelatorioFormatUtil.formatNumeroCru(inicio) + "% " +
                    RelatorioFormatUtil.formatNumeroCru(fim) + "%");
            itens.add(new RelatorioAnalytics.LegendItem(
                    entrada.label(),
                    entrada.monetario() ? RelatorioFormatUtil.formatCurrency(entrada.valor())
                            : RelatorioFormatUtil.formatInteiro(entrada.valor().longValue()),
                    RelatorioFormatUtil.formatPercent(pct),
                    entrada.color()
            ));
        }
        return new RelatorioAnalytics.DonutChart(titulo, centroValor, centroRotulo,
                "conic-gradient(" + String.join(", ", segmentos) + ")", itens, false);
    }

    // ── Rankings e listas ──────────────────────────────────────────────────────

    private List<RelatorioAnalytics.PatientRankingRow> construirRankingPacientes(
            List<SessaoEntity> sessoes, List<LancamentoFinanceiroEntity> lancamentos) {
        var acumulado = new LinkedHashMap<String, PacienteRankingAccumulator>();
        for (SessaoEntity sessao : sessoes) {
            String nome = sessao.getCliente().getNomeCompleto();
            PacienteRankingAccumulator item = acumulado.computeIfAbsent(nome, ignored -> new PacienteRankingAccumulator());
            item.totalSessoes++;
            if (sessao.getStatus() == SessaoStatus.REALIZADA) item.sessoesRealizadas++;
        }
        for (LancamentoFinanceiroEntity lancamento : lancamentos) {
            if (lancamento.getTipo() != TipoLancamentoFinanceiro.RECEITA || lancamento.getCliente() == null) continue;
            String nome = lancamento.getCliente().getNomeCompleto();
            acumulado.computeIfAbsent(nome, ignored -> new PacienteRankingAccumulator())
                    .faturamento = acumulado.get(nome).faturamento.add(lancamento.getValor());
        }
        return acumulado.entrySet().stream()
                .map(e -> new PacienteRankingSource(e.getKey(), e.getValue().totalSessoes, e.getValue().sessoesRealizadas, e.getValue().faturamento))
                .sorted(Comparator.comparingLong(PacienteRankingSource::totalSessoes).reversed()
                        .thenComparing(PacienteRankingSource::faturamento, Comparator.reverseOrder())
                        .thenComparing(PacienteRankingSource::nome))
                .limit(6)
                .map(item -> new RelatorioAnalytics.PatientRankingRow(
                        item.nome(),
                        RelatorioFormatUtil.formatInteiro(item.totalSessoes()) + " sessões",
                        RelatorioFormatUtil.formatInteiro(item.sessoesRealizadas()) + " realizadas",
                        RelatorioFormatUtil.formatCurrency(item.faturamento())
                ))
                .toList();
    }

    private List<RelatorioAnalytics.FinanceCategoryRow> construirCategoriasFinanceiras(List<LancamentoFinanceiroEntity> lancamentos) {
        var categorias = new LinkedHashMap<String, CategoriaAccumulator>();
        for (LancamentoFinanceiroEntity lancamento : lancamentos) {
            String cat = RelatorioFormatUtil.texto(lancamento.getCategoria(), "Sem categoria");
            CategoriaAccumulator item = categorias.computeIfAbsent(cat, ignored -> new CategoriaAccumulator());
            if (lancamento.getTipo() == TipoLancamentoFinanceiro.RECEITA) {
                item.receitas = item.receitas.add(lancamento.getValor());
            } else {
                item.despesas = item.despesas.add(lancamento.getValor());
            }
        }
        return categorias.entrySet().stream()
                .map(e -> new CategoriaSource(e.getKey(), e.getValue().receitas, e.getValue().despesas,
                        e.getValue().receitas.add(e.getValue().despesas)))
                .sorted(Comparator.comparing(CategoriaSource::movimento, Comparator.reverseOrder()))
                .limit(6)
                .map(item -> {
                    BigDecimal saldo = item.receitas().subtract(item.despesas());
                    return new RelatorioAnalytics.FinanceCategoryRow(
                            item.categoria(),
                            RelatorioFormatUtil.formatCurrency(item.receitas()),
                            RelatorioFormatUtil.formatCurrency(item.despesas()),
                            RelatorioFormatUtil.formatCurrency(saldo),
                            RelatorioFormatUtil.toneBySign(saldo)
                    );
                })
                .toList();
    }

    private List<RelatorioAnalytics.RecentPatientRow> construirPacientesRecentes(List<ClienteEntity> pacientes) {
        return pacientes.stream()
                .sorted(Comparator.comparing(ClienteEntity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .map(p -> new RelatorioAnalytics.RecentPatientRow(
                        p.getNomeCompleto(),
                        p.getCreatedAt() != null ? RelatorioFormatUtil.formatDateTime(p.getCreatedAt()) : "-",
                        RelatorioFormatUtil.texto(p.getTelefone(), "Sem telefone"),
                        RelatorioFormatUtil.texto(p.getEmail(), "Sem e-mail")
                ))
                .toList();
    }

    // ── Helpers de contagem e soma ─────────────────────────────────────────────

    private long contarSessoesPorStatus(List<SessaoEntity> sessoes, SessaoStatus status) {
        return sessoes.stream().filter(item -> item.getStatus() == status).count();
    }

    private long contarSolicitacoesPorStatus(List<SolicitacaoAgendamentoEntity> solicitacoes, SolicitacaoStatus status) {
        return solicitacoes.stream().filter(item -> item.getStatus() == status).count();
    }

    private BigDecimal somarPorTipo(List<LancamentoFinanceiroEntity> lancamentos, TipoLancamentoFinanceiro tipo) {
        return lancamentos.stream()
                .filter(item -> item.getTipo() == tipo)
                .map(LancamentoFinanceiroEntity::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarAbsoluto(List<LancamentoFinanceiroEntity> itens) {
        return itens.stream().map(LancamentoFinanceiroEntity::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ── Percentuais ────────────────────────────────────────────────────────────

    private double percentual(long parte, long total) {
        return total <= 0L ? 0.0 : (parte * 100.0) / total;
    }

    private double percentualDoMaximo(long valor, long maximo) {
        return maximo <= 0L ? 0.0 : (valor * 100.0) / maximo;
    }

    private double percentualBigDecimal(BigDecimal valor, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) <= 0) return 0.0;
        return valor.multiply(BigDecimal.valueOf(100))
                .divide(total, 4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    // ── Variação comparativa ───────────────────────────────────────────────────

    private ChangeDescriptor descreverVariacao(long atual, long anterior, boolean maiorEhMelhor) {
        return descreverVariacao(BigDecimal.valueOf(atual), BigDecimal.valueOf(anterior), maiorEhMelhor);
    }

    private ChangeDescriptor descreverVariacao(double atual, double anterior, boolean maiorEhMelhor) {
        return descreverVariacao(BigDecimal.valueOf(atual), BigDecimal.valueOf(anterior), maiorEhMelhor);
    }

    private ChangeDescriptor descreverVariacao(BigDecimal atual, BigDecimal anterior, boolean maiorEhMelhor) {
        if (anterior.compareTo(BigDecimal.ZERO) == 0) {
            if (atual.compareTo(BigDecimal.ZERO) == 0) return new ChangeDescriptor("Sem variação", TONE_NEUTRAL);
            return new ChangeDescriptor("Novo no comparativo", maiorEhMelhor ? TONE_SUCCESS : TONE_WARNING);
        }
        BigDecimal variacao = atual.subtract(anterior)
                .multiply(BigDecimal.valueOf(100))
                .divide(anterior.abs(), 2, RoundingMode.HALF_UP);
        int comparacao = variacao.compareTo(BigDecimal.ZERO);
        String label = (comparacao > 0 ? "+" : "") +
                RelatorioFormatUtil.formatNumero(variacao.doubleValue(), 2) + "% vs período anterior";
        if (comparacao == 0) return new ChangeDescriptor(label, TONE_NEUTRAL);
        boolean positivo = comparacao > 0 ? maiorEhMelhor : !maiorEhMelhor;
        return new ChangeDescriptor(label, positivo ? TONE_SUCCESS : TONE_DANGER);
    }

    // ── Helpers de linha comparativa ───────────────────────────────────────────

    private RelatorioAnalytics.ComparisonRow comparativo(String indicador, long atual, long anterior, ChangeDescriptor delta) {
        return new RelatorioAnalytics.ComparisonRow(indicador,
                RelatorioFormatUtil.formatInteiro(atual),
                RelatorioFormatUtil.formatInteiro(anterior),
                delta.label(), delta.tone());
    }

    private RelatorioAnalytics.ComparisonRow comparativoLong(String indicador, long atual, long anterior, ChangeDescriptor delta) {
        return new RelatorioAnalytics.ComparisonRow(indicador,
                RelatorioFormatUtil.formatInteiro(atual),
                RelatorioFormatUtil.formatInteiro(anterior),
                delta.label(), delta.tone());
    }

    // ── Inner types ────────────────────────────────────────────────────────────

    private record SliceInput(String label, BigDecimal valor, String color, boolean monetario) {}

    private record ChangeDescriptor(String label, String tone) {}

    private static final class PacienteRankingAccumulator {
        long totalSessoes;
        long sessoesRealizadas;
        BigDecimal faturamento = BigDecimal.ZERO;
    }

    private record PacienteRankingSource(String nome, long totalSessoes, long sessoesRealizadas, BigDecimal faturamento) {}

    private static final class CategoriaAccumulator {
        BigDecimal receitas = BigDecimal.ZERO;
        BigDecimal despesas = BigDecimal.ZERO;
    }

    private record CategoriaSource(String categoria, BigDecimal receitas, BigDecimal despesas, BigDecimal movimento) {}
}
