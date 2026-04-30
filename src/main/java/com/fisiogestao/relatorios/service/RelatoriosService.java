package com.fisiogestao.relatorios.service;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.config.ClinicTimezoneHolder;
import com.fisiogestao.email.EmailLogType;
import com.fisiogestao.email.service.EmailAttachment;
import com.fisiogestao.email.service.EmailDispatchResult;
import com.fisiogestao.email.service.EmailRequest;
import com.fisiogestao.email.service.SystemEmailService;
import com.fisiogestao.financeiro.LancamentoFinanceiroEntity;
import com.fisiogestao.financeiro.TipoLancamentoFinanceiro;
import com.fisiogestao.financeiro.service.FinanceiroService;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.relatorios.form.CobrancaEmailForm;
import com.fisiogestao.relatorios.form.EnvioRelatorioForm;
import com.fisiogestao.relatorios.form.FiltroRelatorioForm;
import com.fisiogestao.relatorios.form.FormatoRelatorio;
import com.fisiogestao.relatorios.form.LembreteEmailForm;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import com.fisiogestao.solicitacoes.SolicitacaoStatus;
import com.fisiogestao.solicitacoes.service.SolicitacaoAgendamentoService;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RelatoriosService {

    private static final Locale LOCALE_PT_BR = Locale.forLanguageTag("pt-BR");

    private static final String TONE_SUCCESS = "success";
    private static final String TONE_INFO = "info";
    private static final String TONE_WARNING = "warning";
    private static final String TONE_DANGER = "danger";
    private static final String TONE_NEUTRAL = "neutral";

    private final PacienteService pacienteService;
    private final AgendaService agendaService;
    private final FinanceiroService financeiroService;
    private final SolicitacaoAgendamentoService solicitacaoAgendamentoService;
    private final SystemEmailService systemEmailService;

    public RelatorioOperacional gerarRelatorio(FiltroRelatorioForm filtro) {
        validarFiltro(filtro);

        PeriodoComparacao periodo = calcularPeriodoComparacao(filtro.getDataInicial(), filtro.getDataFinal());

        List<ClienteEntity> pacientes = filtro.isIncluirPacientes() ? pacienteService.listarAtivos() : List.of();
        List<SessaoEntity> sessoes = filtro.isIncluirAgenda()
                ? agendaService.listarSessoesNoPeriodo(periodo.atualInicio(), periodo.atualFimExclusivo())
                : List.of();
        List<SessaoEntity> sessoesAnterior = filtro.isIncluirAgenda()
                ? agendaService.listarSessoesNoPeriodo(periodo.anteriorInicio(), periodo.anteriorFimExclusivo())
                : List.of();

        List<LancamentoFinanceiroEntity> todosLancamentos = filtro.isIncluirFinanceiro()
                ? financeiroService.listarTodos()
                : List.of();
        List<LancamentoFinanceiroEntity> lancamentos = filtrarLancamentos(todosLancamentos, periodo.atualInicio(), periodo.atualFimInclusive());
        List<LancamentoFinanceiroEntity> lancamentosAnterior = filtrarLancamentos(todosLancamentos, periodo.anteriorInicio(), periodo.anteriorFimInclusive());

        List<SolicitacaoAgendamentoEntity> solicitacoes = filtro.isIncluirSolicitacoes()
                ? solicitacaoAgendamentoService.listarNoPeriodo(periodo.atualInicio(), periodo.atualFimExclusivo())
                : List.of();
        List<SolicitacaoAgendamentoEntity> solicitacoesAnterior = filtro.isIncluirSolicitacoes()
                ? solicitacaoAgendamentoService.listarNoPeriodo(periodo.anteriorInicio(), periodo.anteriorFimExclusivo())
                : List.of();

        BigDecimal totalReceitas = somarPorTipo(lancamentos, TipoLancamentoFinanceiro.RECEITA);
        BigDecimal totalDespesas = somarPorTipo(lancamentos, TipoLancamentoFinanceiro.DESPESA);

        RelatorioAnalytics analytics = construirAnalytics(
                periodo,
                pacientes,
                sessoes,
                sessoesAnterior,
                lancamentos,
                lancamentosAnterior,
                solicitacoes,
                solicitacoesAnterior,
                totalReceitas,
                totalDespesas
        );

        return new RelatorioOperacional(
                filtro,
                pacientes,
                sessoes,
                lancamentos,
                solicitacoes,
                totalReceitas,
                totalDespesas,
                analytics
        );
    }

    public RelatorioDocumento exportar(RelatorioOperacional relatorio, FormatoRelatorio formato) {
        return switch (formato) {
            case PDF -> gerarPdf(relatorio);
            case EXCEL -> gerarExcel(relatorio);
        };
    }

    public EmailDispatchResult enviarRelatorioPorEmail(RelatorioOperacional relatorio, EnvioRelatorioForm form, String usuario) {
        RelatorioDocumento documento = exportar(relatorio, form.getFormato());
        return systemEmailService.send(EmailRequest.builder()
                .type(EmailLogType.RELATORIO)
                .subject(form.getAssunto())
                .to(form.getDestinatario())
                .templateName("email/relatorio")
                .templateId("relatorio")
                .triggeredBy(usuario)
                .entityType("relatorio_operacional")
                .templateVariables(Map.of(
                        "titulo", form.getAssunto(),
                        "mensagem", texto(form.getMensagem(), "Segue em anexo o relatório solicitado no sistema."),
                        "dataInicial", relatorio.filtro().getDataInicial(),
                        "dataFinal", relatorio.filtro().getDataFinal()
                ))
                .attachment(new EmailAttachment(documento.fileName(), documento.contentType(), documento.content()))
                .build());
    }

    public EmailDispatchResult enviarLembrete(LembreteEmailForm form, String usuario) {
        return systemEmailService.send(EmailRequest.builder()
                .type(EmailLogType.LEMBRETE)
                .subject(form.getAssunto())
                .to(form.getEmailDestinatario())
                .templateName("email/lembrete")
                .templateId("lembrete")
                .triggeredBy(usuario)
                .entityType("lembrete")
                .templateVariables(Map.of(
                        "titulo", form.getAssunto(),
                        "mensagem", texto(form.getMensagem(), "Este e-mail foi enviado como lembrete pelo sistema."),
                        "nomeDestinatario", texto(form.getNomeDestinatario(), "Paciente"),
                        "tipoLembrete", form.getTipo().name(),
                        "dataReferencia", form.getDataReferencia(),
                        "horaReferencia", form.getHoraReferencia()
                ))
                .build());
    }

    public EmailDispatchResult enviarCobranca(CobrancaEmailForm form, String usuario) {
        LancamentoFinanceiroEntity lancamento = financeiroService.buscarPorId(form.getLancamentoId());
        RelatorioDocumento anexo = gerarPdfCobranca(lancamento);
        return systemEmailService.send(EmailRequest.builder()
                .type(EmailLogType.COBRANCA)
                .subject(form.getAssunto())
                .to(form.getEmailDestinatario())
                .templateName("email/cobranca")
                .templateId("cobranca")
                .triggeredBy(usuario)
                .entityType("lancamento_financeiro")
                .entityId(lancamento.getId())
                .templateVariables(Map.of(
                        "titulo", form.getAssunto(),
                        "mensagem", texto(form.getMensagem(), "Segue o demonstrativo financeiro referente ao lançamento informado."),
                        "descricao", lancamento.getDescricao(),
                        "valor", lancamento.getValor(),
                        "dataCompetencia", lancamento.getDataCompetencia()
                ))
                .attachment(new EmailAttachment(anexo.fileName(), anexo.contentType(), anexo.content()))
                .build());
    }

    private RelatorioAnalytics construirAnalytics(
            PeriodoComparacao periodo,
            List<ClienteEntity> pacientes,
            List<SessaoEntity> sessoes,
            List<SessaoEntity> sessoesAnterior,
            List<LancamentoFinanceiroEntity> lancamentos,
            List<LancamentoFinanceiroEntity> lancamentosAnterior,
            List<SolicitacaoAgendamentoEntity> solicitacoes,
            List<SolicitacaoAgendamentoEntity> solicitacoesAnterior,
            BigDecimal totalReceitas,
            BigDecimal totalDespesas
    ) {
        long novosPacientesAtual = contarPacientesNoPeriodo(pacientes, periodo.atualInicio(), periodo.atualFimInclusive());
        long novosPacientesAnterior = contarPacientesNoPeriodo(pacientes, periodo.anteriorInicio(), periodo.anteriorFimInclusive());

        long sessoesRealizadasAtual = contarSessoesPorStatus(sessoes, SessaoStatus.REALIZADA);
        long sessoesCanceladasAtual = contarSessoesPorStatus(sessoes, SessaoStatus.CANCELADA);
        long sessoesRealizadasAnterior = contarSessoesPorStatus(sessoesAnterior, SessaoStatus.REALIZADA);
        double taxaConclusaoAtual = percentual(sessoesRealizadasAtual, sessoes.size());
        double taxaConclusaoAnterior = percentual(sessoesRealizadasAnterior, sessoesAnterior.size());

        BigDecimal totalReceitasAnterior = somarPorTipo(lancamentosAnterior, TipoLancamentoFinanceiro.RECEITA);
        BigDecimal totalDespesasAnterior = somarPorTipo(lancamentosAnterior, TipoLancamentoFinanceiro.DESPESA);
        BigDecimal saldoAtual = totalReceitas.subtract(totalDespesas);
        BigDecimal saldoAnterior = totalReceitasAnterior.subtract(totalDespesasAnterior);

        long solicitacoesAprovadasAtual = contarSolicitacoesPorStatus(solicitacoes, SolicitacaoStatus.APROVADA);
        long solicitacoesPendentesAtual = contarSolicitacoesPorStatus(solicitacoes, SolicitacaoStatus.PENDENTE);
        long solicitacoesAprovadasAnterior = contarSolicitacoesPorStatus(solicitacoesAnterior, SolicitacaoStatus.APROVADA);
        double taxaAprovacaoAtual = percentual(solicitacoesAprovadasAtual, solicitacoes.size());
        double taxaAprovacaoAnterior = percentual(solicitacoesAprovadasAnterior, solicitacoesAnterior.size());

        long quantidadeReceitas = lancamentos.stream().filter(item -> item.getTipo() == TipoLancamentoFinanceiro.RECEITA).count();
        BigDecimal ticketMedio = quantidadeReceitas == 0
                ? BigDecimal.ZERO
                : totalReceitas.divide(BigDecimal.valueOf(quantidadeReceitas), 2, RoundingMode.HALF_UP);

        ChangeDescriptor deltaNovosPacientes = descreverVariacao(novosPacientesAtual, novosPacientesAnterior, true);
        ChangeDescriptor deltaSessoes = descreverVariacao(sessoes.size(), sessoesAnterior.size(), true);
        ChangeDescriptor deltaConclusao = descreverVariacao(taxaConclusaoAtual, taxaConclusaoAnterior, true);
        ChangeDescriptor deltaReceitas = descreverVariacao(totalReceitas, totalReceitasAnterior, true);
        ChangeDescriptor deltaSolicitacoes = descreverVariacao(solicitacoes.size(), solicitacoesAnterior.size(), true);

        List<RelatorioAnalytics.Highlight> destaques = List.of(
                new RelatorioAnalytics.Highlight("Período", periodo.labelAtual(), TONE_NEUTRAL),
                new RelatorioAnalytics.Highlight("Média de sessões/dia", formatNumero(sessoes.isEmpty() ? 0 : (double) sessoes.size() / periodo.totalDias(), 1), TONE_INFO),
                new RelatorioAnalytics.Highlight("Ticket médio", formatCurrency(ticketMedio), TONE_SUCCESS),
                new RelatorioAnalytics.Highlight("Aprovação", formatPercent(taxaAprovacaoAtual), taxaAprovacaoAtual >= 60 ? TONE_SUCCESS : TONE_WARNING)
        );

        List<RelatorioAnalytics.MetricCard> metricas = List.of(
                new RelatorioAnalytics.MetricCard("users-round", "Pacientes ativos", formatInteiro(pacientes.size()), formatInteiro(novosPacientesAtual) + " novos no período", "Base ativa da clínica", "teal", TONE_NEUTRAL),
                new RelatorioAnalytics.MetricCard("user-plus", "Novos pacientes", formatInteiro(novosPacientesAtual), "Cadastros no intervalo atual", deltaNovosPacientes.label(), "blue", deltaNovosPacientes.tone()),
                new RelatorioAnalytics.MetricCard("calendar-range", "Sessões do período", formatInteiro(sessoes.size()), formatInteiro(sessoesRealizadasAtual) + " realizadas • " + formatInteiro(sessoesCanceladasAtual) + " canceladas", deltaSessoes.label(), "indigo", deltaSessoes.tone()),
                new RelatorioAnalytics.MetricCard("activity", "Taxa de conclusão", formatPercent(taxaConclusaoAtual), formatInteiro(sessoesRealizadasAtual) + " de " + formatInteiro(sessoes.size()) + " sessões concluídas", deltaConclusao.label(), "green", deltaConclusao.tone()),
                new RelatorioAnalytics.MetricCard("badge-dollar-sign", "Receita bruta", formatCurrency(totalReceitas), "Despesas " + formatCurrency(totalDespesas) + " • saldo " + formatCurrency(saldoAtual), deltaReceitas.label(), "amber", deltaReceitas.tone()),
                new RelatorioAnalytics.MetricCard("messages-square", "Solicitações", formatInteiro(solicitacoes.size()), formatInteiro(solicitacoesAprovadasAtual) + " aprovadas • " + formatInteiro(solicitacoesPendentesAtual) + " pendentes", deltaSolicitacoes.label(), "rose", deltaSolicitacoes.tone())
        );

        List<RelatorioAnalytics.ComparisonRow> comparativos = List.of(
                new RelatorioAnalytics.ComparisonRow("Novos pacientes", formatInteiro(novosPacientesAtual), formatInteiro(novosPacientesAnterior), deltaNovosPacientes.label(), deltaNovosPacientes.tone()),
                new RelatorioAnalytics.ComparisonRow("Sessões totais", formatInteiro(sessoes.size()), formatInteiro(sessoesAnterior.size()), deltaSessoes.label(), deltaSessoes.tone()),
                new RelatorioAnalytics.ComparisonRow("Sessões realizadas", formatInteiro(sessoesRealizadasAtual), formatInteiro(sessoesRealizadasAnterior), descreverVariacao(sessoesRealizadasAtual, sessoesRealizadasAnterior, true).label(), descreverVariacao(sessoesRealizadasAtual, sessoesRealizadasAnterior, true).tone()),
                new RelatorioAnalytics.ComparisonRow("Taxa de conclusão", formatPercent(taxaConclusaoAtual), formatPercent(taxaConclusaoAnterior), deltaConclusao.label(), deltaConclusao.tone()),
                new RelatorioAnalytics.ComparisonRow("Receita bruta", formatCurrency(totalReceitas), formatCurrency(totalReceitasAnterior), deltaReceitas.label(), deltaReceitas.tone()),
                new RelatorioAnalytics.ComparisonRow("Despesas", formatCurrency(totalDespesas), formatCurrency(totalDespesasAnterior), descreverVariacao(totalDespesas, totalDespesasAnterior, false).label(), descreverVariacao(totalDespesas, totalDespesasAnterior, false).tone()),
                new RelatorioAnalytics.ComparisonRow("Saldo do período", formatCurrency(saldoAtual), formatCurrency(saldoAnterior), descreverVariacao(saldoAtual, saldoAnterior, true).label(), descreverVariacao(saldoAtual, saldoAnterior, true).tone()),
                new RelatorioAnalytics.ComparisonRow("Solicitações recebidas", formatInteiro(solicitacoes.size()), formatInteiro(solicitacoesAnterior.size()), deltaSolicitacoes.label(), deltaSolicitacoes.tone()),
                new RelatorioAnalytics.ComparisonRow("Taxa de aprovação", formatPercent(taxaAprovacaoAtual), formatPercent(taxaAprovacaoAnterior), descreverVariacao(taxaAprovacaoAtual, taxaAprovacaoAnterior, true).label(), descreverVariacao(taxaAprovacaoAtual, taxaAprovacaoAnterior, true).tone())
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
                construirPacientesRecentes(pacientes)
        );
    }

    private List<RelatorioAnalytics.AgendaDailyPoint> construirAgendaDiaria(PeriodoComparacao periodo, List<SessaoEntity> sessoes) {
        Map<LocalDate, List<SessaoEntity>> sessoesPorDia = sessoes.stream()
                .collect(Collectors.groupingBy(item -> paraLocalDate(item.getDataHoraInicio())));

        long maximo = periodo.datas().stream()
                .mapToLong(data -> sessoesPorDia.getOrDefault(data, List.of()).size())
                .max()
                .orElse(0);

        if (maximo == 0) {
            return List.of();
        }

        List<RelatorioAnalytics.AgendaDailyPoint> itens = new ArrayList<>();
        for (LocalDate data : periodo.datas()) {
            List<SessaoEntity> doDia = sessoesPorDia.getOrDefault(data, List.of());
            long agendadas = doDia.stream().filter(item -> item.getStatus() == SessaoStatus.AGENDADA).count();
            long realizadas = doDia.stream().filter(item -> item.getStatus() == SessaoStatus.REALIZADA).count();
            long canceladas = doDia.stream().filter(item -> item.getStatus() == SessaoStatus.CANCELADA).count();
            long total = doDia.size();
            itens.add(new RelatorioAnalytics.AgendaDailyPoint(
                    formatDateShort(data),
                    formatDateFull(data),
                    formatInteiro(total),
                    formatInteiro(realizadas) + " realizadas • " + formatInteiro(canceladas) + " canceladas",
                    percentualDoMaximo(agendadas, maximo),
                    percentualDoMaximo(realizadas, maximo),
                    percentualDoMaximo(canceladas, maximo)
            ));
        }
        return itens;
    }

    private List<RelatorioAnalytics.FinanceDailyPoint> construirFinanceiroDiario(PeriodoComparacao periodo, List<LancamentoFinanceiroEntity> lancamentos) {
        Map<LocalDate, List<LancamentoFinanceiroEntity>> lancamentosPorDia = lancamentos.stream()
                .collect(Collectors.groupingBy(item -> paraLocalDate(item.getDataCompetencia())));

        BigDecimal maximo = periodo.datas().stream()
                .map(data -> somarAbsolutoDoDia(lancamentosPorDia.getOrDefault(data, List.of())))
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        if (maximo.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }

        List<RelatorioAnalytics.FinanceDailyPoint> itens = new ArrayList<>();
        for (LocalDate data : periodo.datas()) {
            List<LancamentoFinanceiroEntity> doDia = lancamentosPorDia.getOrDefault(data, List.of());
            BigDecimal receitas = somarPorTipo(doDia, TipoLancamentoFinanceiro.RECEITA);
            BigDecimal despesas = somarPorTipo(doDia, TipoLancamentoFinanceiro.DESPESA);
            BigDecimal saldo = receitas.subtract(despesas);
            itens.add(new RelatorioAnalytics.FinanceDailyPoint(
                    formatDateShort(data),
                    formatDateFull(data),
                    formatCurrency(receitas),
                    formatCurrency(despesas),
                    formatCurrency(saldo),
                    percentualBigDecimal(receitas, maximo),
                    percentualBigDecimal(despesas, maximo),
                    toneBySign(saldo)
            ));
        }
        return itens;
    }

    private RelatorioAnalytics.DonutChart construirGraficoSessoes(List<SessaoEntity> sessoes) {
        return construirGraficoRosca(
                "Sessões por status",
                formatInteiro(sessoes.size()),
                "sessões",
                List.of(
                        new SliceInput("Agendadas", BigDecimal.valueOf(contarSessoesPorStatus(sessoes, SessaoStatus.AGENDADA)), "#56b8ff", false),
                        new SliceInput("Realizadas", BigDecimal.valueOf(contarSessoesPorStatus(sessoes, SessaoStatus.REALIZADA)), "#0f8f7a", false),
                        new SliceInput("Canceladas", BigDecimal.valueOf(contarSessoesPorStatus(sessoes, SessaoStatus.CANCELADA)), "#e36d6d", false)
                )
        );
    }

    private RelatorioAnalytics.DonutChart construirGraficoSolicitacoes(List<SolicitacaoAgendamentoEntity> solicitacoes) {
        return construirGraficoRosca(
                "Solicitações por status",
                formatInteiro(solicitacoes.size()),
                "solicitações",
                List.of(
                        new SliceInput("Pendentes", BigDecimal.valueOf(contarSolicitacoesPorStatus(solicitacoes, SolicitacaoStatus.PENDENTE)), "#d19a10", false),
                        new SliceInput("Aprovadas", BigDecimal.valueOf(contarSolicitacoesPorStatus(solicitacoes, SolicitacaoStatus.APROVADA)), "#0f8f7a", false),
                        new SliceInput("Rejeitadas", BigDecimal.valueOf(contarSolicitacoesPorStatus(solicitacoes, SolicitacaoStatus.REJEITADA)), "#cf5b5b", false),
                        new SliceInput("Canceladas", BigDecimal.valueOf(contarSolicitacoesPorStatus(solicitacoes, SolicitacaoStatus.CANCELADA)), "#7b8794", false)
                )
        );
    }

    private RelatorioAnalytics.DonutChart construirGraficoFinanceiro(BigDecimal receitas, BigDecimal despesas) {
        return construirGraficoRosca(
                "Movimento financeiro",
                formatCurrency(receitas.add(despesas)),
                "movimentado",
                List.of(
                        new SliceInput("Receitas", receitas, "#0f8f7a", true),
                        new SliceInput("Despesas", despesas, "#d55353", true)
                )
        );
    }

    private RelatorioAnalytics.DonutChart construirGraficoRosca(
            String titulo,
            String centroValor,
            String centroRotulo,
            List<SliceInput> entradas
    ) {
        List<SliceInput> filtradas = entradas.stream()
                .filter(item -> item.valor().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        BigDecimal total = filtradas.stream()
                .map(SliceInput::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (filtradas.isEmpty() || total.compareTo(BigDecimal.ZERO) <= 0) {
            return new RelatorioAnalytics.DonutChart(
                    titulo,
                    "0",
                    "sem dados",
                    "conic-gradient(#dce6e3 0 100%)",
                    List.of(new RelatorioAnalytics.LegendItem("Sem dados no período", "-", "-", "#dce6e3")),
                    true
            );
        }

        List<String> segmentos = new ArrayList<>();
        List<RelatorioAnalytics.LegendItem> itens = new ArrayList<>();
        BigDecimal acumulado = BigDecimal.ZERO;

        for (SliceInput entrada : filtradas) {
            double inicio = percentualBigDecimal(acumulado, total);
            acumulado = acumulado.add(entrada.valor());
            double fim = percentualBigDecimal(acumulado, total);
            double percentual = percentualBigDecimal(entrada.valor(), total);
            segmentos.add(entrada.color() + " " + formatNumeroCru(inicio) + "% " + formatNumeroCru(fim) + "%");
            itens.add(new RelatorioAnalytics.LegendItem(
                    entrada.label(),
                    entrada.monetario() ? formatCurrency(entrada.valor()) : formatInteiro(entrada.valor().longValue()),
                    formatPercent(percentual),
                    entrada.color()
            ));
        }

        return new RelatorioAnalytics.DonutChart(
                titulo,
                centroValor,
                centroRotulo,
                "conic-gradient(" + String.join(", ", segmentos) + ")",
                itens,
                false
        );
    }

    private List<RelatorioAnalytics.PatientRankingRow> construirRankingPacientes(
            List<SessaoEntity> sessoes,
            List<LancamentoFinanceiroEntity> lancamentos
    ) {
        Map<String, PacienteRankingAccumulator> acumulado = new LinkedHashMap<>();

        for (SessaoEntity sessao : sessoes) {
            String nome = sessao.getCliente().getNomeCompleto();
            PacienteRankingAccumulator item = acumulado.computeIfAbsent(nome, ignored -> new PacienteRankingAccumulator());
            item.totalSessoes++;
            if (sessao.getStatus() == SessaoStatus.REALIZADA) {
                item.sessoesRealizadas++;
            }
        }

        for (LancamentoFinanceiroEntity lancamento : lancamentos) {
            if (lancamento.getTipo() != TipoLancamentoFinanceiro.RECEITA || lancamento.getCliente() == null) {
                continue;
            }
            String nome = lancamento.getCliente().getNomeCompleto();
            PacienteRankingAccumulator item = acumulado.computeIfAbsent(nome, ignored -> new PacienteRankingAccumulator());
            item.faturamento = item.faturamento.add(lancamento.getValor());
        }

        return acumulado.entrySet().stream()
                .map(entry -> new PacienteRankingSource(entry.getKey(), entry.getValue().totalSessoes, entry.getValue().sessoesRealizadas, entry.getValue().faturamento))
                .sorted(Comparator
                        .comparingLong(PacienteRankingSource::totalSessoes).reversed()
                        .thenComparing(PacienteRankingSource::faturamento, Comparator.reverseOrder())
                        .thenComparing(PacienteRankingSource::nome))
                .limit(6)
                .map(item -> new RelatorioAnalytics.PatientRankingRow(
                        item.nome(),
                        formatInteiro(item.totalSessoes()) + " sessões",
                        formatInteiro(item.sessoesRealizadas()) + " realizadas",
                        formatCurrency(item.faturamento())
                ))
                .toList();
    }

    private List<RelatorioAnalytics.FinanceCategoryRow> construirCategoriasFinanceiras(List<LancamentoFinanceiroEntity> lancamentos) {
        Map<String, CategoriaFinanceiraAccumulator> categorias = new LinkedHashMap<>();

        for (LancamentoFinanceiroEntity lancamento : lancamentos) {
            String categoria = texto(lancamento.getCategoria(), "Sem categoria");
            CategoriaFinanceiraAccumulator item = categorias.computeIfAbsent(categoria, ignored -> new CategoriaFinanceiraAccumulator());
            if (lancamento.getTipo() == TipoLancamentoFinanceiro.RECEITA) {
                item.receitas = item.receitas.add(lancamento.getValor());
            } else {
                item.despesas = item.despesas.add(lancamento.getValor());
            }
        }

        return categorias.entrySet().stream()
                .map(entry -> new CategoriaFinanceiraSource(
                        entry.getKey(),
                        entry.getValue().receitas,
                        entry.getValue().despesas,
                        entry.getValue().receitas.add(entry.getValue().despesas)
                ))
                .sorted(Comparator.comparing(CategoriaFinanceiraSource::movimento, Comparator.reverseOrder()))
                .limit(6)
                .map(item -> {
                    BigDecimal saldo = item.receitas().subtract(item.despesas());
                    return new RelatorioAnalytics.FinanceCategoryRow(
                            item.categoria(),
                            formatCurrency(item.receitas()),
                            formatCurrency(item.despesas()),
                            formatCurrency(saldo),
                            toneBySign(saldo)
                    );
                })
                .toList();
    }

    private List<RelatorioAnalytics.RecentPatientRow> construirPacientesRecentes(List<ClienteEntity> pacientes) {
        return pacientes.stream()
                .sorted(Comparator.comparing(ClienteEntity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .map(paciente -> new RelatorioAnalytics.RecentPatientRow(
                        paciente.getNomeCompleto(),
                        paciente.getCreatedAt() != null ? formatDateTime(paciente.getCreatedAt()) : "-",
                        texto(paciente.getTelefone(), "Sem telefone"),
                        texto(paciente.getEmail(), "Sem e-mail")
                ))
                .toList();
    }

    private RelatorioDocumento gerarPdf(RelatorioOperacional relatorio) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(new Paragraph("Relatório Operacional - FisioGestao", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("Período: " + relatorio.filtro().getDataInicial() + " até " + relatorio.filtro().getDataFinal()));
            document.add(new Paragraph(" "));

            PdfPTable resumo = new PdfPTable(5);
            resumo.setWidthPercentage(100);
            resumo.addCell(celulaCabecalho("Pacientes"));
            resumo.addCell(celulaCabecalho("Sessões"));
            resumo.addCell(celulaCabecalho("Receitas"));
            resumo.addCell(celulaCabecalho("Despesas"));
            resumo.addCell(celulaCabecalho("Saldo"));
            resumo.addCell(celulaValor(String.valueOf(relatorio.pacientes().size())));
            resumo.addCell(celulaValor(String.valueOf(relatorio.sessoes().size())));
            resumo.addCell(celulaValor(formatCurrency(relatorio.totalReceitas())));
            resumo.addCell(celulaValor(formatCurrency(relatorio.totalDespesas())));
            resumo.addCell(celulaValor(formatCurrency(relatorio.saldoFinanceiro())));
            document.add(resumo);
            document.add(new Paragraph(" "));

            if (!relatorio.sessoes().isEmpty()) {
                document.add(new Paragraph("Sessões", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
                document.add(tabelaSessoes(relatorio.sessoes()));
            }
            if (!relatorio.lancamentos().isEmpty()) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Financeiro", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
                document.add(tabelaFinanceiro(relatorio.lancamentos()));
            }
            if (!relatorio.solicitacoes().isEmpty()) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Solicitações", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
                document.add(tabelaSolicitacoes(relatorio.solicitacoes()));
            }
            document.close();
            return new RelatorioDocumento("relatorio-operacional.pdf", "application/pdf", output.toByteArray());
        } catch (Exception ex) {
            throw new BusinessException("Não foi possível gerar o PDF do relatório.");
        }
    }

    private RelatorioDocumento gerarExcel(RelatorioOperacional relatorio) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet resumo = workbook.createSheet("Resumo");
            Row row = resumo.createRow(0);
            row.createCell(0).setCellValue("Período");
            row.createCell(1).setCellValue(relatorio.filtro().getDataInicial() + " até " + relatorio.filtro().getDataFinal());
            row = resumo.createRow(1);
            row.createCell(0).setCellValue("Pacientes");
            row.createCell(1).setCellValue(relatorio.pacientes().size());
            row = resumo.createRow(2);
            row.createCell(0).setCellValue("Sessões");
            row.createCell(1).setCellValue(relatorio.sessoes().size());
            row = resumo.createRow(3);
            row.createCell(0).setCellValue("Receitas");
            row.createCell(1).setCellValue(relatorio.totalReceitas().doubleValue());
            row = resumo.createRow(4);
            row.createCell(0).setCellValue("Despesas");
            row.createCell(1).setCellValue(relatorio.totalDespesas().doubleValue());
            row = resumo.createRow(5);
            row.createCell(0).setCellValue("Saldo");
            row.createCell(1).setCellValue(relatorio.saldoFinanceiro().doubleValue());

            if (!relatorio.sessoes().isEmpty()) {
                popularAgendaSheet(workbook.createSheet("Agenda"), relatorio.sessoes());
            }
            if (!relatorio.lancamentos().isEmpty()) {
                popularFinanceiroSheet(workbook.createSheet("Financeiro"), relatorio.lancamentos());
            }
            if (!relatorio.solicitacoes().isEmpty()) {
                popularSolicitacaoSheet(workbook.createSheet("Solicitacoes"), relatorio.solicitacoes());
            }

            workbook.write(output);
            return new RelatorioDocumento(
                    "relatorio-operacional.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray()
            );
        } catch (Exception ex) {
            throw new BusinessException("Não foi possível gerar a planilha do relatório.");
        }
    }

    private RelatorioDocumento gerarPdfCobranca(LancamentoFinanceiroEntity lancamento) {
        FiltroRelatorioForm filtro = new FiltroRelatorioForm();
        filtro.setDataInicial(lancamento.getDataCompetencia().toLocalDate());
        filtro.setDataFinal(lancamento.getDataCompetencia().toLocalDate());
        filtro.setIncluirPacientes(false);
        filtro.setIncluirAgenda(false);
        filtro.setIncluirFinanceiro(true);
        filtro.setIncluirSolicitacoes(false);

        BigDecimal receitas = lancamento.getTipo() == TipoLancamentoFinanceiro.RECEITA ? lancamento.getValor() : BigDecimal.ZERO;
        BigDecimal despesas = lancamento.getTipo() == TipoLancamentoFinanceiro.DESPESA ? lancamento.getValor() : BigDecimal.ZERO;

        return gerarPdf(new RelatorioOperacional(
                filtro,
                List.of(),
                List.of(),
                List.of(lancamento),
                List.of(),
                receitas,
                despesas,
                analyticsVazia(calcularPeriodoComparacao(filtro.getDataInicial(), filtro.getDataFinal()))
        ));
    }

    private RelatorioAnalytics analyticsVazia(PeriodoComparacao periodo) {
        return new RelatorioAnalytics(
                periodo.labelAtual(),
                periodo.labelAnterior(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new RelatorioAnalytics.DonutChart("Sessões", "0", "sem dados", "conic-gradient(#dce6e3 0 100%)", List.of(), true),
                new RelatorioAnalytics.DonutChart("Solicitações", "0", "sem dados", "conic-gradient(#dce6e3 0 100%)", List.of(), true),
                new RelatorioAnalytics.DonutChart("Financeiro", "0", "sem dados", "conic-gradient(#dce6e3 0 100%)", List.of(), true),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private BigDecimal somarPorTipo(List<LancamentoFinanceiroEntity> lancamentos, TipoLancamentoFinanceiro tipo) {
        return lancamentos.stream()
                .filter(item -> item.getTipo() == tipo)
                .map(LancamentoFinanceiroEntity::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PdfPTable tabelaSessoes(List<SessaoEntity> sessoes) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.addCell(celulaCabecalho("Paciente"));
        table.addCell(celulaCabecalho("Início"));
        table.addCell(celulaCabecalho("Fim"));
        table.addCell(celulaCabecalho("Status"));
        for (SessaoEntity item : sessoes) {
            table.addCell(celulaValor(item.getCliente().getNomeCompleto()));
            table.addCell(celulaValor(formatDateTime(item.getDataHoraInicio())));
            table.addCell(celulaValor(formatDateTime(item.getDataHoraFim())));
            table.addCell(celulaValor(item.getStatus().name()));
        }
        return table;
    }

    private PdfPTable tabelaFinanceiro(List<LancamentoFinanceiroEntity> lancamentos) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.addCell(celulaCabecalho("Descrição"));
        table.addCell(celulaCabecalho("Tipo"));
        table.addCell(celulaCabecalho("Status"));
        table.addCell(celulaCabecalho("Valor"));
        for (LancamentoFinanceiroEntity item : lancamentos) {
            table.addCell(celulaValor(item.getDescricao()));
            table.addCell(celulaValor(item.getTipo().name()));
            table.addCell(celulaValor(item.getStatus().name()));
            table.addCell(celulaValor(formatCurrency(item.getValor())));
        }
        return table;
    }

    private PdfPTable tabelaSolicitacoes(List<SolicitacaoAgendamentoEntity> solicitacoes) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.addCell(celulaCabecalho("Paciente"));
        table.addCell(celulaCabecalho("Data"));
        table.addCell(celulaCabecalho("Telefone"));
        table.addCell(celulaCabecalho("Status"));
        for (SolicitacaoAgendamentoEntity item : solicitacoes) {
            table.addCell(celulaValor(item.getNomePaciente()));
            table.addCell(celulaValor(formatDateTime(item.getDataHoraInicio())));
            table.addCell(celulaValor(item.getTelefone()));
            table.addCell(celulaValor(item.getStatus().name()));
        }
        return table;
    }

    private void popularAgendaSheet(Sheet sheet, List<SessaoEntity> sessoes) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Paciente");
        header.createCell(1).setCellValue("Início");
        header.createCell(2).setCellValue("Fim");
        header.createCell(3).setCellValue("Status");
        int rowIndex = 1;
        for (SessaoEntity item : sessoes) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.getCliente().getNomeCompleto());
            row.createCell(1).setCellValue(formatDateTime(item.getDataHoraInicio()));
            row.createCell(2).setCellValue(formatDateTime(item.getDataHoraFim()));
            row.createCell(3).setCellValue(item.getStatus().name());
        }
    }

    private void popularFinanceiroSheet(Sheet sheet, List<LancamentoFinanceiroEntity> lancamentos) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Descrição");
        header.createCell(1).setCellValue("Tipo");
        header.createCell(2).setCellValue("Status");
        header.createCell(3).setCellValue("Competência");
        header.createCell(4).setCellValue("Valor");
        int rowIndex = 1;
        for (LancamentoFinanceiroEntity item : lancamentos) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.getDescricao());
            row.createCell(1).setCellValue(item.getTipo().name());
            row.createCell(2).setCellValue(item.getStatus().name());
            row.createCell(3).setCellValue(formatDateTime(item.getDataCompetencia()));
            row.createCell(4).setCellValue(item.getValor().doubleValue());
        }
    }

    private void popularSolicitacaoSheet(Sheet sheet, List<SolicitacaoAgendamentoEntity> solicitacoes) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Paciente");
        header.createCell(1).setCellValue("Data");
        header.createCell(2).setCellValue("Telefone");
        header.createCell(3).setCellValue("Status");
        int rowIndex = 1;
        for (SolicitacaoAgendamentoEntity item : solicitacoes) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.getNomePaciente());
            row.createCell(1).setCellValue(formatDateTime(item.getDataHoraInicio()));
            row.createCell(2).setCellValue(item.getTelefone());
            row.createCell(3).setCellValue(item.getStatus().name());
        }
    }

    private PdfPCell celulaCabecalho(String valor) {
        PdfPCell cell = new PdfPCell(new Phrase(valor, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
        cell.setPadding(6f);
        return cell;
    }

    private PdfPCell celulaValor(String valor) {
        PdfPCell cell = new PdfPCell(new Phrase(valor == null ? "-" : valor, FontFactory.getFont(FontFactory.HELVETICA, 9)));
        cell.setPadding(6f);
        return cell;
    }

    private void validarFiltro(FiltroRelatorioForm filtro) {
        if (filtro.getDataInicial() == null || filtro.getDataFinal() == null) {
            throw new BusinessException("Informe o período inicial e final do relatório.");
        }
        if (filtro.getDataFinal().isBefore(filtro.getDataInicial())) {
            throw new BusinessException("A data final não pode ser anterior à data inicial.");
        }
    }

    private PeriodoComparacao calcularPeriodoComparacao(LocalDate atualInicio, LocalDate atualFimInclusive) {
        long totalDias = ChronoUnit.DAYS.between(atualInicio, atualFimInclusive) + 1;
        LocalDate anteriorFim = atualInicio.minusDays(1);
        LocalDate anteriorInicio = anteriorFim.minusDays(totalDias - 1);
        return new PeriodoComparacao(
                atualInicio,
                atualFimInclusive,
                atualFimInclusive.plusDays(1),
                anteriorInicio,
                anteriorFim,
                anteriorFim.plusDays(1),
                construirLabelPeriodo(atualInicio, atualFimInclusive),
                construirLabelPeriodo(anteriorInicio, anteriorFim),
                totalDias,
                construirListaDatas(atualInicio, atualFimInclusive)
        );
    }

    private List<LancamentoFinanceiroEntity> filtrarLancamentos(List<LancamentoFinanceiroEntity> itens, LocalDate inicioInclusive, LocalDate fimInclusive) {
        return itens.stream()
                .filter(item -> {
                    LocalDate data = paraLocalDate(item.getDataCompetencia());
                    return !data.isBefore(inicioInclusive) && !data.isAfter(fimInclusive);
                })
                .toList();
    }

    private long contarPacientesNoPeriodo(List<ClienteEntity> pacientes, LocalDate inicioInclusive, LocalDate fimInclusive) {
        return pacientes.stream()
                .filter(item -> item.getCreatedAt() != null)
                .map(ClienteEntity::getCreatedAt)
                .map(this::paraLocalDate)
                .filter(data -> !data.isBefore(inicioInclusive) && !data.isAfter(fimInclusive))
                .count();
    }

    private long contarSessoesPorStatus(List<SessaoEntity> sessoes, SessaoStatus status) {
        return sessoes.stream().filter(item -> item.getStatus() == status).count();
    }

    private long contarSolicitacoesPorStatus(List<SolicitacaoAgendamentoEntity> solicitacoes, SolicitacaoStatus status) {
        return solicitacoes.stream().filter(item -> item.getStatus() == status).count();
    }

    private BigDecimal somarAbsolutoDoDia(List<LancamentoFinanceiroEntity> itens) {
        return itens.stream()
                .map(LancamentoFinanceiroEntity::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double percentual(long parte, long total) {
        if (total <= 0L) {
            return 0.0;
        }
        return (parte * 100.0) / total;
    }

    private double percentualDoMaximo(long valor, long maximo) {
        if (maximo <= 0L) {
            return 0.0;
        }
        return (valor * 100.0) / maximo;
    }

    private double percentualBigDecimal(BigDecimal valor, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return valor.multiply(BigDecimal.valueOf(100))
                .divide(total, 4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private ChangeDescriptor descreverVariacao(long atual, long anterior, boolean maiorEhMelhor) {
        return descreverVariacao(BigDecimal.valueOf(atual), BigDecimal.valueOf(anterior), maiorEhMelhor);
    }

    private ChangeDescriptor descreverVariacao(double atual, double anterior, boolean maiorEhMelhor) {
        return descreverVariacao(BigDecimal.valueOf(atual), BigDecimal.valueOf(anterior), maiorEhMelhor);
    }

    private ChangeDescriptor descreverVariacao(BigDecimal atual, BigDecimal anterior, boolean maiorEhMelhor) {
        if (anterior.compareTo(BigDecimal.ZERO) == 0) {
            if (atual.compareTo(BigDecimal.ZERO) == 0) {
                return new ChangeDescriptor("Sem variação", TONE_NEUTRAL);
            }
            return new ChangeDescriptor("Novo no comparativo", maiorEhMelhor ? TONE_SUCCESS : TONE_WARNING);
        }

        BigDecimal variacao = atual.subtract(anterior)
                .multiply(BigDecimal.valueOf(100))
                .divide(anterior.abs(), 2, RoundingMode.HALF_UP);
        int comparacao = variacao.compareTo(BigDecimal.ZERO);
        String prefixo = comparacao > 0 ? "+" : "";
        String label = prefixo + formatNumero(variacao.doubleValue(), 2) + "% vs período anterior";
        if (comparacao == 0) {
            return new ChangeDescriptor(label, TONE_NEUTRAL);
        }
        boolean positivo = comparacao > 0 ? maiorEhMelhor : !maiorEhMelhor;
        return new ChangeDescriptor(label, positivo ? TONE_SUCCESS : TONE_DANGER);
    }

    private String toneBySign(BigDecimal valor) {
        int comparacao = valor.compareTo(BigDecimal.ZERO);
        if (comparacao > 0) {
            return TONE_SUCCESS;
        }
        if (comparacao < 0) {
            return TONE_DANGER;
        }
        return TONE_NEUTRAL;
    }

    private LocalDate paraLocalDate(OffsetDateTime valor) {
        return valor.atZoneSameInstant(clinicZone()).toLocalDate();
    }

    private String formatDateTime(OffsetDateTime dataHora) {
        return dataHora.atZoneSameInstant(clinicZone())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private ZoneId clinicZone() {
        return ClinicTimezoneHolder.zoneId();
    }

    private String formatDateShort(LocalDate data) {
        return data.format(DateTimeFormatter.ofPattern("dd/MM"));
    }

    private String formatDateFull(LocalDate data) {
        return data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String construirLabelPeriodo(LocalDate inicio, LocalDate fim) {
        return formatDateFull(inicio) + " a " + formatDateFull(fim);
    }

    private List<LocalDate> construirListaDatas(LocalDate inicio, LocalDate fim) {
        List<LocalDate> datas = new ArrayList<>();
        LocalDate cursor = inicio;
        while (!cursor.isAfter(fim)) {
            datas.add(cursor);
            cursor = cursor.plusDays(1);
        }
        return datas;
    }

    private String formatCurrency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(LOCALE_PT_BR).format(value);
    }

    private String formatInteiro(long value) {
        return NumberFormat.getIntegerInstance(LOCALE_PT_BR).format(value);
    }

    private String formatNumero(double value, int escala) {
        NumberFormat format = NumberFormat.getNumberInstance(LOCALE_PT_BR);
        format.setMinimumFractionDigits(escala);
        format.setMaximumFractionDigits(escala);
        return format.format(value);
    }

    private String formatNumeroCru(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private String formatPercent(double value) {
        return formatNumero(value, 1) + "%";
    }

    private String texto(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor.trim();
    }

    private record PeriodoComparacao(
            LocalDate atualInicio,
            LocalDate atualFimInclusive,
            LocalDate atualFimExclusivo,
            LocalDate anteriorInicio,
            LocalDate anteriorFimInclusive,
            LocalDate anteriorFimExclusivo,
            String labelAtual,
            String labelAnterior,
            long totalDias,
            List<LocalDate> datas
    ) {
    }

    private record SliceInput(
            String label,
            BigDecimal valor,
            String color,
            boolean monetario
    ) {
    }

    private record ChangeDescriptor(
            String label,
            String tone
    ) {
    }

    private static final class PacienteRankingAccumulator {
        private long totalSessoes;
        private long sessoesRealizadas;
        private BigDecimal faturamento = BigDecimal.ZERO;
    }

    private record PacienteRankingSource(
            String nome,
            long totalSessoes,
            long sessoesRealizadas,
            BigDecimal faturamento
    ) {
    }

    private static final class CategoriaFinanceiraAccumulator {
        private BigDecimal receitas = BigDecimal.ZERO;
        private BigDecimal despesas = BigDecimal.ZERO;
    }

    private record CategoriaFinanceiraSource(
            String categoria,
            BigDecimal receitas,
            BigDecimal despesas,
            BigDecimal movimento
    ) {
    }
}
