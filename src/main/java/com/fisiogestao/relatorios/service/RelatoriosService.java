package com.fisiogestao.relatorios.service;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.auth.MenuAccessSecurity;
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
import com.fisiogestao.solicitacoes.service.SolicitacaoAgendamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RelatoriosService {

    private final PacienteService pacienteService;
    private final AgendaService agendaService;
    private final FinanceiroService financeiroService;
    private final SolicitacaoAgendamentoService solicitacaoAgendamentoService;
    private final SystemEmailService systemEmailService;
    private final RelatorioAnalyticsBuilder analyticsBuilder;
    private final RelatorioExportService exportService;
    private final MenuAccessSecurity menuAccessSecurity;

    /**
     * Faturamento detalhado (antigo {@code /financeiro/relatorio-faturamento}), agora incorporado
     * à aba "Faturamento detalhado" da Central de Relatórios. Continua protegido pela permissão
     * de menu FATURAMENTO (independente de RELATORIOS), pois papéis como Fisioterapeuta e
     * Assessoria acessam /relatorios sem ter acesso a dados financeiros detalhados.
     */
    public record FaturamentoDetalhado(
            FinanceiroService.RelatorioFaturamento relatorio,
            FinanceiroService.InadimplenciaResumo inadimplencia
    ) {}

    public boolean isFaturamentoDetalhadoDisponivel() {
        return menuAccessSecurity.canAccess("FATURAMENTO");
    }

    public FaturamentoDetalhado gerarFaturamentoDetalhado(FiltroRelatorioForm filtro) {
        if (!filtro.isIncluirFinanceiro() || !isFaturamentoDetalhadoDisponivel()) {
            return null;
        }
        validarFiltro(filtro);
        return new FaturamentoDetalhado(
                financeiroService.gerarRelatorioFaturamento(filtro.getDataInicial(), filtro.getDataFinal()),
                financeiroService.calcularInadimplencia()
        );
    }

    // ── API pública (contrato com RelatoriosController, inalterado) ────────────

    public RelatorioOperacional gerarRelatorio(FiltroRelatorioForm filtro) {
        validarFiltro(filtro);

        PeriodoComparacao periodo = calcularPeriodo(filtro.getDataInicial(), filtro.getDataFinal());

        long totalPacientesAtivos  = filtro.isIncluirPacientes() ? pacienteService.contarAtivos() : 0L;
        long novosPacientesAtual   = filtro.isIncluirPacientes() ? contarPacientesNoPeriodo(periodo.atualInicio(), periodo.atualFimExclusivo()) : 0L;
        long novosPacientesAnterior = filtro.isIncluirPacientes() ? contarPacientesNoPeriodo(periodo.anteriorInicio(), periodo.anteriorFimExclusivo()) : 0L;
        List<ClienteEntity> pacientesRecentes = filtro.isIncluirPacientes() ? pacienteService.listarAtivosRecentes() : List.of();

        List<SessaoEntity> sessoes         = filtro.isIncluirAgenda() ? agendaService.listarSessoesNoPeriodo(periodo.atualInicio(), periodo.atualFimExclusivo()) : List.of();
        List<SessaoEntity> sessoesAnterior = filtro.isIncluirAgenda() ? agendaService.listarSessoesNoPeriodo(periodo.anteriorInicio(), periodo.anteriorFimExclusivo()) : List.of();

        List<LancamentoFinanceiroEntity> todosLancamentos = filtro.isIncluirFinanceiro()
                ? financeiroService.listarNoPeriodo(periodo.anteriorInicio(), periodo.atualFimInclusive())
                : List.of();
        List<LancamentoFinanceiroEntity> lancamentos         = filtrarLancamentos(todosLancamentos, periodo.atualInicio(), periodo.atualFimInclusive());
        List<LancamentoFinanceiroEntity> lancamentosAnterior = filtrarLancamentos(todosLancamentos, periodo.anteriorInicio(), periodo.anteriorFimInclusive());

        List<SolicitacaoAgendamentoEntity> solicitacoes         = filtro.isIncluirSolicitacoes() ? solicitacaoAgendamentoService.listarNoPeriodo(periodo.atualInicio(), periodo.atualFimExclusivo()) : List.of();
        List<SolicitacaoAgendamentoEntity> solicitacoesAnterior = filtro.isIncluirSolicitacoes() ? solicitacaoAgendamentoService.listarNoPeriodo(periodo.anteriorInicio(), periodo.anteriorFimExclusivo()) : List.of();

        BigDecimal totalReceitas = somarPorTipo(lancamentos, TipoLancamentoFinanceiro.RECEITA);
        BigDecimal totalDespesas = somarPorTipo(lancamentos, TipoLancamentoFinanceiro.DESPESA);

        RelatorioAnalytics analytics = analyticsBuilder.construir(
                periodo, totalPacientesAtivos, novosPacientesAtual, novosPacientesAnterior,
                pacientesRecentes, sessoes, sessoesAnterior,
                lancamentos, lancamentosAnterior,
                solicitacoes, solicitacoesAnterior,
                totalReceitas, totalDespesas
        );

        return new RelatorioOperacional(filtro, pacientesRecentes, totalPacientesAtivos,
                sessoes, lancamentos, solicitacoes, totalReceitas, totalDespesas, analytics);
    }

    public RelatorioDocumento exportar(RelatorioOperacional relatorio, FormatoRelatorio formato, FaturamentoDetalhado faturamento) {
        return exportService.exportar(relatorio, formato, faturamento != null ? faturamento.relatorio() : null);
    }

    public EmailDispatchResult enviarRelatorioPorEmail(RelatorioOperacional relatorio, FaturamentoDetalhado faturamento,
                                                        EnvioRelatorioForm form, String usuario) {
        RelatorioDocumento documento = exportService.exportar(
                relatorio, form.getFormato(), faturamento != null ? faturamento.relatorio() : null);
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
                        "mensagem", RelatorioFormatUtil.texto(form.getMensagem(), "Segue em anexo o relatório solicitado no sistema."),
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
                        "mensagem", RelatorioFormatUtil.texto(form.getMensagem(), "Este e-mail foi enviado como lembrete pelo sistema."),
                        "nomeDestinatario", RelatorioFormatUtil.texto(form.getNomeDestinatario(), "Paciente"),
                        "tipoLembrete", form.getTipo().name(),
                        "dataReferencia", form.getDataReferencia(),
                        "horaReferencia", form.getHoraReferencia()
                ))
                .build());
    }

    public EmailDispatchResult enviarCobranca(CobrancaEmailForm form, String usuario) {
        LancamentoFinanceiroEntity lancamento = financeiroService.buscarPorId(form.getLancamentoId());

        BigDecimal receitas = lancamento.getTipo() == TipoLancamentoFinanceiro.RECEITA ? lancamento.getValor() : BigDecimal.ZERO;
        BigDecimal despesas = lancamento.getTipo() == TipoLancamentoFinanceiro.DESPESA ? lancamento.getValor() : BigDecimal.ZERO;

        FiltroRelatorioForm filtroCobranca = new FiltroRelatorioForm();
        LocalDate dataRecebimentoLocal = lancamento.getDataRecebimento()
                .atZoneSameInstant(ClinicTimezoneHolder.zoneId())
                .toLocalDate();
        filtroCobranca.setDataInicial(dataRecebimentoLocal);
        filtroCobranca.setDataFinal(dataRecebimentoLocal);
        filtroCobranca.setIncluirFinanceiro(true);

        PeriodoComparacao periodo = calcularPeriodo(filtroCobranca.getDataInicial(), filtroCobranca.getDataFinal());
        RelatorioOperacional relatorioCobranca = new RelatorioOperacional(
                filtroCobranca, List.of(), 0L, List.of(),
                List.of(lancamento), List.of(),
                receitas, despesas,
                analyticsBuilder.vazia(periodo)
        );

        RelatorioDocumento anexo = exportService.gerarPdf(relatorioCobranca);
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
                        "mensagem", RelatorioFormatUtil.texto(form.getMensagem(), "Segue o demonstrativo financeiro referente ao lançamento informado."),
                        "descricao", lancamento.getDescricao(),
                        "valor", lancamento.getValor(),
                        "dataRecebimento", lancamento.getDataRecebimento()
                ))
                .attachment(new EmailAttachment(anexo.fileName(), anexo.contentType(), anexo.content()))
                .build());
    }

    // ── Helpers privados ───────────────────────────────────────────────────────

    private void validarFiltro(FiltroRelatorioForm filtro) {
        if (filtro.getDataInicial() == null || filtro.getDataFinal() == null) {
            throw new BusinessException("Informe o período inicial e final do relatório.");
        }
        if (filtro.getDataFinal().isBefore(filtro.getDataInicial())) {
            throw new BusinessException("A data final não pode ser anterior à data inicial.");
        }
    }

    private PeriodoComparacao calcularPeriodo(LocalDate atualInicio, LocalDate atualFimInclusive) {
        long totalDias = ChronoUnit.DAYS.between(atualInicio, atualFimInclusive) + 1;
        LocalDate anteriorFim   = atualInicio.minusDays(1);
        LocalDate anteriorInicio = anteriorFim.minusDays(totalDias - 1);
        return new PeriodoComparacao(
                atualInicio, atualFimInclusive, atualFimInclusive.plusDays(1),
                anteriorInicio, anteriorFim, anteriorFim.plusDays(1),
                RelatorioFormatUtil.construirLabelPeriodo(atualInicio, atualFimInclusive),
                RelatorioFormatUtil.construirLabelPeriodo(anteriorInicio, anteriorFim),
                totalDias,
                RelatorioFormatUtil.construirListaDatas(atualInicio, atualFimInclusive)
        );
    }

    private List<LancamentoFinanceiroEntity> filtrarLancamentos(
            List<LancamentoFinanceiroEntity> itens, LocalDate inicioInclusive, LocalDate fimInclusive) {
        return itens.stream()
                .filter(item -> {
                    LocalDate data = RelatorioFormatUtil.paraLocalDate(item.getDataRecebimento());
                    return !data.isBefore(inicioInclusive) && !data.isAfter(fimInclusive);
                })
                .toList();
    }

    private long contarPacientesNoPeriodo(LocalDate inicioInclusive, LocalDate fimExclusivo) {
        ZoneId zone = ClinicTimezoneHolder.zoneId();
        OffsetDateTime inicio = inicioInclusive.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime fim    = fimExclusivo.atStartOfDay(zone).toOffsetDateTime();
        return pacienteService.contarAtivosCriadosNoPeriodo(inicio, fim);
    }

    private BigDecimal somarPorTipo(List<LancamentoFinanceiroEntity> lancamentos, TipoLancamentoFinanceiro tipo) {
        return lancamentos.stream()
                .filter(item -> item.getTipo() == tipo)
                .map(LancamentoFinanceiroEntity::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
