package com.fisiogestao.financeiro.service;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.config.ClinicTimezoneHolder;
import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.financeiro.LancamentoFinanceiroEntity;
import com.fisiogestao.financeiro.MetodoPagamento;
import com.fisiogestao.financeiro.StatusLancamentoFinanceiro;
import com.fisiogestao.financeiro.TipoLancamentoFinanceiro;
import com.fisiogestao.financeiro.form.LancamentoFinanceiroForm;
import com.fisiogestao.financeiro.repository.LancamentoFinanceiroRepository;
import com.fisiogestao.pacientes.PlanoSessaoEntity;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class FinanceiroService {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;
    private final AuditService auditService;

    public record ResumoFinanceiro(
            BigDecimal receitasRecebidas,
            BigDecimal receitasPendentes,
            BigDecimal despesasPagas,
            BigDecimal despesasPendentes,
            long receitasPagasCount,
            long lancamentosAtivosCount
    ) {
        public BigDecimal saldoRealizado() {
            return receitasRecebidas.subtract(despesasPagas);
        }
        public BigDecimal saldoProjetado() {
            return receitasRecebidas.add(receitasPendentes).subtract(despesasPagas).subtract(despesasPendentes);
        }
        public BigDecimal ticketMedio() {
            if (receitasPagasCount == 0) return BigDecimal.ZERO;
            return receitasRecebidas.divide(
                    BigDecimal.valueOf(receitasPagasCount), 2, java.math.RoundingMode.HALF_UP);
        }
        public String taxaRecebimento() {
            BigDecimal total = receitasRecebidas.add(receitasPendentes);
            if (total.compareTo(BigDecimal.ZERO) == 0) return "0,0%";
            return receitasRecebidas.multiply(BigDecimal.valueOf(100))
                    .divide(total, 1, java.math.RoundingMode.HALF_UP)
                    .toPlainString().replace('.', ',') + "%";
        }
        public String taxaTone() {
            BigDecimal total = receitasRecebidas.add(receitasPendentes);
            if (total.compareTo(BigDecimal.ZERO) == 0) return "neutral";
            BigDecimal taxa = receitasRecebidas.multiply(BigDecimal.valueOf(100))
                    .divide(total, 1, java.math.RoundingMode.HALF_UP);
            if (taxa.compareTo(BigDecimal.valueOf(80)) >= 0) return "success";
            if (taxa.compareTo(BigDecimal.valueOf(50)) >= 0) return "warning";
            return "danger";
        }
    }

    public record InadimplenciaResumo(
            BigDecimal totalVencido,
            long quantidade,
            long maiorAtrasoDias
    ) {
        public boolean temInadimplencia() {
            return quantidade > 0 && totalVencido.compareTo(BigDecimal.ZERO) > 0;
        }

        public String tone() {
            if (!temInadimplencia()) {
                return "success";
            }
            if (maiorAtrasoDias <= 7) {
                return "warning";
            }
            return "danger";
        }
    }

    public record ContasAReceberResumo(
            BigDecimal total,
            BigDecimal totalVencido,
            BigDecimal totalAVencer,
            long quantidade,
            long gruposCount
    ) {}

    public record ContaReceberGrupo(
            String paciente,
            String plano,
            UUID clienteId,
            BigDecimal total,
            BigDecimal aVencer,
            BigDecimal vencidoAte7,
            BigDecimal vencido8A15,
            BigDecimal vencido16A30,
            BigDecimal vencidoAcima30,
            long quantidade,
            UUID primeiroLancamentoId,
            LocalDate vencimentoMaisAntigo,
            long maiorAtrasoDias
    ) {
        public BigDecimal totalVencido() {
            return vencidoAte7.add(vencido8A15).add(vencido16A30).add(vencidoAcima30);
        }

        public String agingTone() {
            if (maiorAtrasoDias > 30) {
                return "danger";
            }
            if (maiorAtrasoDias > 0) {
                return "warning";
            }
            return "success";
        }
    }

    public record ContasAReceberView(
            ContasAReceberResumo resumo,
            List<ContaReceberGrupo> grupos
    ) {}

    public record ContasAPagarResumo(
            BigDecimal total,
            BigDecimal totalVencido,
            BigDecimal totalAVencer,
            long quantidade,
            long gruposCount
    ) {}

    public record ContaPagarGrupo(
            String categoria,
            String descricao,
            BigDecimal total,
            BigDecimal aVencer,
            BigDecimal vencidoAte7,
            BigDecimal vencido8A15,
            BigDecimal vencido16A30,
            BigDecimal vencidoAcima30,
            long quantidade,
            UUID primeiroLancamentoId,
            LocalDate vencimentoMaisAntigo,
            long maiorAtrasoDias
    ) {
        public BigDecimal totalVencido() {
            return vencidoAte7.add(vencido8A15).add(vencido16A30).add(vencidoAcima30);
        }

        public String agingTone() {
            if (maiorAtrasoDias > 30) {
                return "danger";
            }
            if (maiorAtrasoDias > 0) {
                return "warning";
            }
            return "success";
        }
    }

    public record ContasAPagarView(
            ContasAPagarResumo resumo,
            List<ContaPagarGrupo> grupos
    ) {}

    public record FaturamentoResumo(
            BigDecimal total,
            long quantidade,
            BigDecimal ticketMedio
    ) {}

    public record FaturamentoGrupo(
            String nome,
            long quantidade,
            BigDecimal total,
            String participacao
    ) {}

    public record RelatorioFaturamento(
            LocalDate inicio,
            LocalDate fim,
            FaturamentoResumo resumo,
            List<FaturamentoGrupo> porCategoria,
            List<FaturamentoGrupo> porPlano,
            List<FaturamentoGrupo> porProfissional,
            List<FaturamentoGrupo> porMetodoPagamento,
            List<FaturamentoGrupo> porTipoAtendimento
    ) {}

    @Transactional(readOnly = true)
    public List<LancamentoFinanceiroEntity> listarNoPeriodo(LocalDate inicio, LocalDate fim) {
        PeriodoFinanceiro periodo = periodoFinanceiro(inicio, fim);
        return lancamentoFinanceiroRepository
                .findByDataRecebimentoBetweenOrderByDataRecebimentoDescCreatedAtDesc(periodo.inicio(), periodo.fim());
    }

    private PeriodoFinanceiro periodoFinanceiro(LocalDate inicio, LocalDate fim) {
        ZoneId zone = ClinicTimezoneHolder.zoneId();
        OffsetDateTime dtInicio = inicio.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime dtFim = fim.plusDays(1).atStartOfDay(zone).toOffsetDateTime().minusNanos(1);
        return new PeriodoFinanceiro(dtInicio, dtFim);
    }

    @Transactional(readOnly = true)
    public ResumoFinanceiro calcularResumo(LocalDate inicio, LocalDate fim) {
        PeriodoFinanceiro periodo = periodoFinanceiro(inicio, fim);
        long receitasPagasCount = lancamentoFinanceiroRepository.countByDataRecebimentoBetweenAndTipoAndStatus(
                periodo.inicio(), periodo.fim(), TipoLancamentoFinanceiro.RECEITA, StatusLancamentoFinanceiro.PAGO);
        long lancamentosAtivosCount = lancamentoFinanceiroRepository.countByDataRecebimentoBetweenAndStatusNot(
                periodo.inicio(), periodo.fim(), StatusLancamentoFinanceiro.CANCELADO);
        return new ResumoFinanceiro(
                somarNoPeriodo(periodo, TipoLancamentoFinanceiro.RECEITA, StatusLancamentoFinanceiro.PAGO),
                somarNoPeriodo(periodo, TipoLancamentoFinanceiro.RECEITA, StatusLancamentoFinanceiro.PENDENTE),
                somarNoPeriodo(periodo, TipoLancamentoFinanceiro.DESPESA, StatusLancamentoFinanceiro.PAGO),
                somarNoPeriodo(periodo, TipoLancamentoFinanceiro.DESPESA, StatusLancamentoFinanceiro.PENDENTE),
                receitasPagasCount,
                lancamentosAtivosCount
        );
    }

    @Transactional(readOnly = true)
    public InadimplenciaResumo calcularInadimplencia() {
        LocalDate hoje = ClinicTimezoneHolder.today();
        OffsetDateTime limite = hoje.atStartOfDay(ClinicTimezoneHolder.zoneId()).toOffsetDateTime();
        BigDecimal total = valorSeguro(lancamentoFinanceiroRepository.sumValorByTipoAndStatusAndDataRecebimentoBefore(
                TipoLancamentoFinanceiro.RECEITA,
                StatusLancamentoFinanceiro.PENDENTE,
                limite
        ));
        long quantidade = lancamentoFinanceiroRepository.countByTipoAndStatusAndDataRecebimentoBefore(
                TipoLancamentoFinanceiro.RECEITA,
                StatusLancamentoFinanceiro.PENDENTE,
                limite
        );
        long maiorAtraso = lancamentoFinanceiroRepository
                .findFirstByTipoAndStatusAndDataRecebimentoBeforeOrderByDataRecebimentoAscCreatedAtAsc(
                        TipoLancamentoFinanceiro.RECEITA,
                        StatusLancamentoFinanceiro.PENDENTE,
                        limite
                )
                .map(lancamento -> atrasoEmDias(lancamento, hoje))
                .orElse(0L);
        return new InadimplenciaResumo(total, quantidade, maiorAtraso);
    }

    @Transactional(readOnly = true)
    public ContasAReceberView listarContasAReceber() {
        List<LancamentoFinanceiroEntity> pendentes = lancamentoFinanceiroRepository
                .findByTipoAndStatusOrderByDataRecebimentoAscCreatedAtAsc(
                        TipoLancamentoFinanceiro.RECEITA,
                        StatusLancamentoFinanceiro.PENDENTE
                );
        LocalDate hoje = ClinicTimezoneHolder.today();
        Map<String, ContaReceberAcumulador> acumulados = new LinkedHashMap<>();

        for (LancamentoFinanceiroEntity lancamento : pendentes) {
            String paciente = lancamento.getCliente() != null
                    ? textoOuPadrao(lancamento.getCliente().getNomeCompleto(), "Sem paciente")
                    : "Sem paciente";
            String plano = lancamento.getPlanoSessao() != null
                    ? textoOuPadrao(lancamento.getPlanoSessao().getNomePlano(), "Sem plano")
                    : "Sem plano";
            String chave = paciente + "|" + plano;
            acumulados.computeIfAbsent(chave, ignored -> new ContaReceberAcumulador(paciente, plano))
                    .adicionar(lancamento, hoje);
        }

        List<ContaReceberGrupo> grupos = acumulados.values().stream()
                .map(ContaReceberAcumulador::toGrupo)
                .sorted(Comparator
                        .comparing(ContaReceberGrupo::maiorAtrasoDias, Comparator.reverseOrder())
                        .thenComparing(ContaReceberGrupo::vencimentoMaisAntigo, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ContaReceberGrupo::paciente))
                .toList();

        BigDecimal total = somarGrupos(grupos, ContaReceberGrupo::total);
        BigDecimal totalVencido = somarGrupos(grupos, ContaReceberGrupo::totalVencido);
        BigDecimal totalAVencer = somarGrupos(grupos, ContaReceberGrupo::aVencer);
        long quantidade = grupos.stream().mapToLong(ContaReceberGrupo::quantidade).sum();

        return new ContasAReceberView(
                new ContasAReceberResumo(total, totalVencido, totalAVencer, quantidade, grupos.size()),
                grupos
        );
    }

    @Transactional(readOnly = true)
    public ContasAPagarView listarContasAPagar() {
        List<LancamentoFinanceiroEntity> pendentes = lancamentoFinanceiroRepository
                .findByTipoAndStatusOrderByDataRecebimentoAscCreatedAtAsc(
                        TipoLancamentoFinanceiro.DESPESA,
                        StatusLancamentoFinanceiro.PENDENTE
                );
        LocalDate hoje = ClinicTimezoneHolder.today();
        Map<String, ContaPagarAcumulador> acumulados = new LinkedHashMap<>();

        for (LancamentoFinanceiroEntity lancamento : pendentes) {
            String categoria = textoOuPadrao(lancamento.getCategoria(), "Sem categoria");
            String descricao = textoOuPadrao(lancamento.getDescricao(), "Sem descricao");
            String chave = categoria + "|" + descricao;
            acumulados.computeIfAbsent(chave, ignored -> new ContaPagarAcumulador(categoria, descricao))
                    .adicionar(lancamento, hoje);
        }

        List<ContaPagarGrupo> grupos = acumulados.values().stream()
                .map(ContaPagarAcumulador::toGrupo)
                .sorted(Comparator
                        .comparing(ContaPagarGrupo::maiorAtrasoDias, Comparator.reverseOrder())
                        .thenComparing(ContaPagarGrupo::vencimentoMaisAntigo, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ContaPagarGrupo::categoria)
                        .thenComparing(ContaPagarGrupo::descricao))
                .toList();

        BigDecimal total = somarGruposAPagar(grupos, ContaPagarGrupo::total);
        BigDecimal totalVencido = somarGruposAPagar(grupos, ContaPagarGrupo::totalVencido);
        BigDecimal totalAVencer = somarGruposAPagar(grupos, ContaPagarGrupo::aVencer);
        long quantidade = grupos.stream().mapToLong(ContaPagarGrupo::quantidade).sum();

        return new ContasAPagarView(
                new ContasAPagarResumo(total, totalVencido, totalAVencer, quantidade, grupos.size()),
                grupos
        );
    }

    @Transactional(readOnly = true)
    public long contarContasAPagarPendentes() {
        return lancamentoFinanceiroRepository.countByTipoAndStatus(
                TipoLancamentoFinanceiro.DESPESA,
                StatusLancamentoFinanceiro.PENDENTE
        );
    }

    @Transactional(readOnly = true)
    public RelatorioFaturamento gerarRelatorioFaturamento(LocalDate inicio, LocalDate fim) {
        PeriodoFinanceiro periodo = periodoFinanceiro(inicio, fim);
        List<LancamentoFinanceiroEntity> receitasPagas = lancamentoFinanceiroRepository
                .findByDataRecebimentoBetweenAndTipoAndStatusOrderByDataRecebimentoDescCreatedAtDesc(
                        periodo.inicio(),
                        periodo.fim(),
                        TipoLancamentoFinanceiro.RECEITA,
                        StatusLancamentoFinanceiro.PAGO
                );

        BigDecimal totalGeral = receitasPagas.stream()
                .map(LancamentoFinanceiroEntity::getValor)
                .map(FinanceiroService::valorSeguro)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long quantidade = receitasPagas.size();
        BigDecimal ticketMedio = quantidade == 0
                ? BigDecimal.ZERO
                : totalGeral.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);

        return new RelatorioFaturamento(
                inicio,
                fim,
                new FaturamentoResumo(totalGeral, quantidade, ticketMedio),
                mapearTexto(lancamentoFinanceiroRepository.agruparFaturamentoPorCategoria(
                        periodo.inicio(), periodo.fim(), TipoLancamentoFinanceiro.RECEITA, StatusLancamentoFinanceiro.PAGO), totalGeral),
                mapearTexto(lancamentoFinanceiroRepository.agruparFaturamentoPorPlano(
                        periodo.inicio(), periodo.fim(), TipoLancamentoFinanceiro.RECEITA, StatusLancamentoFinanceiro.PAGO), totalGeral),
                mapearTexto(lancamentoFinanceiroRepository.agruparFaturamentoPorProfissional(
                        periodo.inicio(), periodo.fim(), TipoLancamentoFinanceiro.RECEITA, StatusLancamentoFinanceiro.PAGO), totalGeral),
                mapearMetodo(lancamentoFinanceiroRepository.agruparFaturamentoPorMetodoPagamento(
                        periodo.inicio(), periodo.fim(), TipoLancamentoFinanceiro.RECEITA, StatusLancamentoFinanceiro.PAGO), totalGeral),
                agruparPorTipoAtendimento(receitasPagas, totalGeral)
        );
    }

    private BigDecimal somarNoPeriodo(PeriodoFinanceiro periodo,
                                      TipoLancamentoFinanceiro tipo,
                                      StatusLancamentoFinanceiro status) {
        BigDecimal total = lancamentoFinanceiroRepository.sumValorByPeriodoAndTipoAndStatus(
                periodo.inicio(), periodo.fim(), tipo, status);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public LancamentoFinanceiroEntity buscarPorId(UUID id) {
        return lancamentoFinanceiroRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Lançamento financeiro não encontrado."));
    }

    @Transactional
    public LancamentoFinanceiroEntity atualizarManual(UUID lancamentoId, LancamentoFinanceiroForm form) {
        LancamentoFinanceiroEntity lancamento = buscarPorId(lancamentoId);
        var snapshotAnterior = auditService.snapshotOf(lancamento);

        if (lancamento.getStatus() == StatusLancamentoFinanceiro.CANCELADO) {
            throw new BusinessException("Lançamentos cancelados não podem ser editados.");
        }
        if (lancamento.getPlanoSessao() != null && lancamento.getTipo() != form.getTipo()) {
            throw new BusinessException("Não é possível alterar o tipo de um lançamento vinculado a um plano de tratamento.");
        }

        lancamento.setTipo(form.getTipo());
        lancamento.setDescricao(form.getDescricao().trim());
        lancamento.setCategoria(form.getCategoria().trim());
        lancamento.setProfissional(texto(form.getProfissional()));
        lancamento.setValor(form.getValor());
        lancamento.setDataRecebimento(paraInicioDoDia(form.getDataRecebimento()));

        LancamentoFinanceiroEntity salvo = lancamentoFinanceiroRepository.save(lancamento);
        auditService.recordManual(
                AuditModule.FINANCEIRO,
                "lancamento_financeiro",
                salvo.getId().toString(),
                salvo.getDescricao(),
                AuditActionType.UPDATE,
                snapshotAnterior,
                auditService.snapshotOf(salvo),
                null,
                "Edição manual de lançamento financeiro.",
                AuditEventOrigin.MANUAL
        );
        return salvo;
    }

    @Transactional
    public LancamentoFinanceiroEntity criarManual(LancamentoFinanceiroForm form) {
        LancamentoFinanceiroEntity lancamento = LancamentoFinanceiroEntity.builder()
                .tipo(form.getTipo())
                .status(StatusLancamentoFinanceiro.PENDENTE)
                .descricao(form.getDescricao().trim())
                .categoria(form.getCategoria().trim())
                .profissional(texto(form.getProfissional()))
                .valor(form.getValor())
                .dataRecebimento(paraInicioDoDia(form.getDataRecebimento()))
                .build();
        LancamentoFinanceiroEntity salvo = lancamentoFinanceiroRepository.save(lancamento);
        auditService.recordCreate(
                AuditModule.FINANCEIRO,
                "lancamento_financeiro",
                salvo,
                null,
                "Cadastro manual de lançamento financeiro.",
                AuditEventOrigin.MANUAL
        );
        return salvo;
    }

    @Transactional
    public void registrarReceitaAvulso(SessaoEntity sessao) {
        registrarReceitaAvulso(sessao, null);
    }

    @Transactional
    public void registrarReceitaAvulso(SessaoEntity sessao, String profissionalResponsavel) {
        if (sessao.getValorAvulso() == null || sessao.getValorAvulso().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        String descricao = "Sessão avulsa: " +
                (sessao.getTipoAtendimento() != null && !sessao.getTipoAtendimento().isBlank()
                        ? sessao.getTipoAtendimento()
                        : "Fisioterapia");
        LocalDate dataLocal = sessao.getDataHoraInicio()
                .atZoneSameInstant(ClinicTimezoneHolder.zoneId())
                .toLocalDate();
        LancamentoFinanceiroEntity receita = LancamentoFinanceiroEntity.builder()
                .tipo(TipoLancamentoFinanceiro.RECEITA)
                .status(StatusLancamentoFinanceiro.PENDENTE)
                .descricao(descricao)
                .categoria("Sessões avulsas")
                .valor(sessao.getValorAvulso())
                .dataRecebimento(paraInicioDoDia(dataLocal))
                .profissional(texto(profissionalResponsavel))
                .cliente(sessao.getCliente())
                .build();
        LancamentoFinanceiroEntity salvo = lancamentoFinanceiroRepository.save(receita);
        auditService.recordCreate(
                AuditModule.FINANCEIRO,
                "lancamento_financeiro",
                salvo,
                null,
                "Receita gerada automaticamente a partir de sessão avulsa.",
                AuditEventOrigin.SISTEMA
        );
    }

    @Transactional
    public void registrarReceitaPlano(PlanoSessaoEntity plano) {
        registrarReceitaPlano(plano, null);
    }

    @Transactional
    public void registrarReceitaPlano(PlanoSessaoEntity plano, String profissionalResponsavel) {
        LancamentoFinanceiroEntity receita = LancamentoFinanceiroEntity.builder()
                .tipo(TipoLancamentoFinanceiro.RECEITA)
                .status(StatusLancamentoFinanceiro.PENDENTE)
                .descricao("Plano de tratamento: " + plano.getNomePlano())
                .categoria("Planos de tratamento")
                .valor(plano.getValorTotal())
                .dataRecebimento(paraInicioDoDia(plano.getDataInicio()))
                .cliente(plano.getCliente())
                .planoSessao(plano)
                .profissional(texto(profissionalResponsavel))
                .build();
        LancamentoFinanceiroEntity salvo = lancamentoFinanceiroRepository.save(receita);
        auditService.recordCreate(
                AuditModule.FINANCEIRO,
                "lancamento_financeiro",
                salvo,
                null,
                "Receita gerada automaticamente a partir da venda de plano de tratamento.",
                AuditEventOrigin.SISTEMA
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal totalReceitasPendentes() {
        return somarPorFiltro(TipoLancamentoFinanceiro.RECEITA, StatusLancamentoFinanceiro.PENDENTE);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalDespesasPendentes() {
        return somarPorFiltro(TipoLancamentoFinanceiro.DESPESA, StatusLancamentoFinanceiro.PENDENTE);
    }

    @Transactional(readOnly = true)
    public BigDecimal saldoProjetado() {
        return totalReceitasPendentes().subtract(totalDespesasPendentes());
    }

    @Transactional
    public void cancelar(UUID lancamentoId) {
        LancamentoFinanceiroEntity lancamento = buscarPorId(lancamentoId);
        var snapshotAnterior = auditService.snapshotOf(lancamento);

        if (lancamento.getStatus() == StatusLancamentoFinanceiro.CANCELADO) {
            return;
        }
        if (lancamento.getStatus() != StatusLancamentoFinanceiro.PENDENTE) {
            throw new BusinessException("Somente lançamentos pendentes podem ser cancelados.");
        }

        lancamento.setStatus(StatusLancamentoFinanceiro.CANCELADO);
        lancamento.setDataPagamento(null);
        LancamentoFinanceiroEntity salvo = lancamentoFinanceiroRepository.save(lancamento);
        auditService.recordManual(
                AuditModule.FINANCEIRO,
                "lancamento_financeiro",
                salvo.getId().toString(),
                salvo.getDescricao(),
                AuditActionType.CANCEL,
                snapshotAnterior,
                auditService.snapshotOf(salvo),
                null,
                "Cancelamento manual de lançamento financeiro.",
                AuditEventOrigin.MANUAL
        );
    }

    @Transactional
    public void marcarComoPago(UUID lancamentoId, MetodoPagamento metodoPagamento) {
        LancamentoFinanceiroEntity lancamento = lancamentoFinanceiroRepository.findById(lancamentoId)
                .orElseThrow(() -> new BusinessException("Lançamento financeiro não encontrado."));
        var snapshotAnterior = auditService.snapshotOf(lancamento);

        if (lancamento.getStatus() != StatusLancamentoFinanceiro.PENDENTE) {
            throw new BusinessException("Apenas lançamentos pendentes podem ser marcados como pagos.");
        }
        if (metodoPagamento == null) {
            throw new BusinessException("Selecione a forma de pagamento para registrar a baixa.");
        }

        lancamento.setStatus(StatusLancamentoFinanceiro.PAGO);
        lancamento.setDataPagamento(ClinicTimezoneHolder.now());
        lancamento.setMetodoPagamento(metodoPagamento);
        LancamentoFinanceiroEntity salvo = lancamentoFinanceiroRepository.save(lancamento);
        auditService.recordManual(
                AuditModule.FINANCEIRO,
                "lancamento_financeiro",
                salvo.getId().toString(),
                salvo.getDescricao(),
                AuditActionType.MARK_AS_PAID,
                snapshotAnterior,
                auditService.snapshotOf(salvo),
                null,
                "Baixa manual de lançamento financeiro.",
                AuditEventOrigin.MANUAL
        );
    }

    @Transactional
    public void cancelarBaixa(UUID lancamentoId) {
        LancamentoFinanceiroEntity lancamento = buscarPorId(lancamentoId);
        var snapshotAnterior = auditService.snapshotOf(lancamento);

        if (lancamento.getStatus() != StatusLancamentoFinanceiro.PAGO) {
            throw new BusinessException("Somente lançamentos pagos podem ter a baixa cancelada.");
        }

        lancamento.setStatus(StatusLancamentoFinanceiro.PENDENTE);
        lancamento.setDataPagamento(null);
        lancamento.setMetodoPagamento(null);
        LancamentoFinanceiroEntity salvo = lancamentoFinanceiroRepository.save(lancamento);
        auditService.recordManual(
                AuditModule.FINANCEIRO,
                "lancamento_financeiro",
                salvo.getId().toString(),
                salvo.getDescricao(),
                AuditActionType.UNDO_PAYMENT,
                snapshotAnterior,
                auditService.snapshotOf(salvo),
                null,
                "Baixa financeira cancelada manualmente para permitir refazer o lançamento.",
                AuditEventOrigin.MANUAL
        );
    }

    @Transactional
    public void reabrir(UUID lancamentoId) {
        LancamentoFinanceiroEntity lancamento = buscarPorId(lancamentoId);
        var snapshotAnterior = auditService.snapshotOf(lancamento);

        if (lancamento.getStatus() != StatusLancamentoFinanceiro.CANCELADO) {
            throw new BusinessException("Somente lançamentos cancelados podem ter o cancelamento revertido.");
        }

        lancamento.setStatus(StatusLancamentoFinanceiro.PENDENTE);
        LancamentoFinanceiroEntity salvo = lancamentoFinanceiroRepository.save(lancamento);
        auditService.recordManual(
                AuditModule.FINANCEIRO,
                "lancamento_financeiro",
                salvo.getId().toString(),
                salvo.getDescricao(),
                AuditActionType.RESTORE,
                snapshotAnterior,
                auditService.snapshotOf(salvo),
                null,
                "Reversão manual de cancelamento de lançamento financeiro.",
                AuditEventOrigin.MANUAL
        );
    }

    private BigDecimal somarPorFiltro(TipoLancamentoFinanceiro tipo, StatusLancamentoFinanceiro status) {
        return lancamentoFinanceiroRepository.sumValorByTipoAndStatus(tipo, status);
    }

    private static BigDecimal somarGrupos(
            List<ContaReceberGrupo> grupos,
            Function<ContaReceberGrupo, BigDecimal> extractor
    ) {
        return grupos.stream()
                .map(extractor)
                .map(FinanceiroService::valorSeguro)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal somarGruposAPagar(
            List<ContaPagarGrupo> grupos,
            Function<ContaPagarGrupo, BigDecimal> extractor
    ) {
        return grupos.stream()
                .map(extractor)
                .map(FinanceiroService::valorSeguro)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static List<FaturamentoGrupo> mapearTexto(
            List<LancamentoFinanceiroRepository.TotalPorTextoProjection> projections,
            BigDecimal totalGeral
    ) {
        return projections.stream()
                .map(item -> new FaturamentoGrupo(
                        textoOuPadrao(item.getNome(), "Sem informação"),
                        item.getQuantidade() == null ? 0L : item.getQuantidade(),
                        valorSeguro(item.getTotal()),
                        percentual(valorSeguro(item.getTotal()), totalGeral)
                ))
                .toList();
    }

    private static List<FaturamentoGrupo> mapearMetodo(
            List<LancamentoFinanceiroRepository.TotalPorMetodoProjection> projections,
            BigDecimal totalGeral
    ) {
        return projections.stream()
                .map(item -> new FaturamentoGrupo(
                        item.getMetodoPagamento() != null ? item.getMetodoPagamento().getLabel() : "Sem método",
                        item.getQuantidade() == null ? 0L : item.getQuantidade(),
                        valorSeguro(item.getTotal()),
                        percentual(valorSeguro(item.getTotal()), totalGeral)
                ))
                .toList();
    }

    private List<FaturamentoGrupo> agruparPorTipoAtendimento(
            List<LancamentoFinanceiroEntity> receitasPagas,
            BigDecimal totalGeral
    ) {
        Map<String, FaturamentoAcumulador> acumulados = new LinkedHashMap<>();
        for (LancamentoFinanceiroEntity lancamento : receitasPagas) {
            String nome = tipoAtendimentoFinanceiro(lancamento);
            acumulados.computeIfAbsent(nome, FaturamentoAcumulador::new)
                    .adicionar(lancamento.getValor());
        }
        return acumulados.values().stream()
                .sorted(Comparator.comparing(FaturamentoAcumulador::total, Comparator.reverseOrder())
                        .thenComparing(FaturamentoAcumulador::nome))
                .map(acumulador -> acumulador.toGrupo(totalGeral))
                .toList();
    }

    private String tipoAtendimentoFinanceiro(LancamentoFinanceiroEntity lancamento) {
        String descricao = texto(lancamento.getDescricao());
        if (descricao != null) {
            int separador = descricao.indexOf(':');
            String prefixo = separador > 0 ? descricao.substring(0, separador).toLowerCase(Locale.ROOT) : "";
            if (separador > 0 && prefixo.contains("avulsa")) {
                return textoOuPadrao(descricao.substring(separador + 1), "Sessão avulsa");
            }
        }
        if (lancamento.getPlanoSessao() != null) {
            return "Plano: " + textoOuPadrao(lancamento.getPlanoSessao().getNomePlano(), "Sem plano");
        }
        return textoOuPadrao(lancamento.getCategoria(), "Sem categoria");
    }

    private static String percentual(BigDecimal valor, BigDecimal totalGeral) {
        if (totalGeral == null || totalGeral.compareTo(BigDecimal.ZERO) == 0) {
            return "0,0%";
        }
        return valorSeguro(valor)
                .multiply(CEM)
                .divide(totalGeral, 1, RoundingMode.HALF_UP)
                .toPlainString()
                .replace('.', ',') + "%";
    }

    private static long atrasoEmDias(LancamentoFinanceiroEntity lancamento, LocalDate hoje) {
        LocalDate vencimento = dataRecebimentoLocal(lancamento);
        return Math.max(0L, ChronoUnit.DAYS.between(vencimento, hoje));
    }

    private static LocalDate dataRecebimentoLocal(LancamentoFinanceiroEntity lancamento) {
        return lancamento.getDataRecebimento()
                .atZoneSameInstant(ClinicTimezoneHolder.zoneId())
                .toLocalDate();
    }

    private static BigDecimal valorSeguro(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static String textoOuPadrao(String valor, String padrao) {
        String normalizado = texto(valor);
        return normalizado == null ? padrao : normalizado;
    }

    private OffsetDateTime paraInicioDoDia(LocalDate data) {
        return data.atStartOfDay(ClinicTimezoneHolder.zoneId()).toOffsetDateTime();
    }

    private static final class ContaReceberAcumulador {
        private final String paciente;
        private final String plano;
        private UUID clienteId;
        private BigDecimal total = BigDecimal.ZERO;
        private BigDecimal aVencer = BigDecimal.ZERO;
        private BigDecimal vencidoAte7 = BigDecimal.ZERO;
        private BigDecimal vencido8A15 = BigDecimal.ZERO;
        private BigDecimal vencido16A30 = BigDecimal.ZERO;
        private BigDecimal vencidoAcima30 = BigDecimal.ZERO;
        private long quantidade;
        private UUID primeiroLancamentoId;
        private LocalDate vencimentoMaisAntigo;
        private long maiorAtrasoDias;

        private ContaReceberAcumulador(String paciente, String plano) {
            this.paciente = paciente;
            this.plano = plano;
        }

        private void adicionar(LancamentoFinanceiroEntity lancamento, LocalDate hoje) {
            BigDecimal valor = valorSeguro(lancamento.getValor());
            LocalDate vencimento = dataRecebimentoLocal(lancamento);
            long atraso = atrasoEmDias(lancamento, hoje);

            total = total.add(valor);
            quantidade++;
            if (clienteId == null && lancamento.getCliente() != null) {
                clienteId = lancamento.getCliente().getId();
            }
            if (vencimentoMaisAntigo == null || vencimento.isBefore(vencimentoMaisAntigo)) {
                vencimentoMaisAntigo = vencimento;
                primeiroLancamentoId = lancamento.getId();
            }
            maiorAtrasoDias = Math.max(maiorAtrasoDias, atraso);

            if (atraso <= 0) {
                aVencer = aVencer.add(valor);
            } else if (atraso <= 7) {
                vencidoAte7 = vencidoAte7.add(valor);
            } else if (atraso <= 15) {
                vencido8A15 = vencido8A15.add(valor);
            } else if (atraso <= 30) {
                vencido16A30 = vencido16A30.add(valor);
            } else {
                vencidoAcima30 = vencidoAcima30.add(valor);
            }
        }

        private ContaReceberGrupo toGrupo() {
            return new ContaReceberGrupo(
                    paciente,
                    plano,
                    clienteId,
                    total,
                    aVencer,
                    vencidoAte7,
                    vencido8A15,
                    vencido16A30,
                    vencidoAcima30,
                    quantidade,
                    primeiroLancamentoId,
                    vencimentoMaisAntigo,
                    maiorAtrasoDias
            );
        }
    }

    private static final class ContaPagarAcumulador {
        private final String categoria;
        private final String descricao;
        private BigDecimal total = BigDecimal.ZERO;
        private BigDecimal aVencer = BigDecimal.ZERO;
        private BigDecimal vencidoAte7 = BigDecimal.ZERO;
        private BigDecimal vencido8A15 = BigDecimal.ZERO;
        private BigDecimal vencido16A30 = BigDecimal.ZERO;
        private BigDecimal vencidoAcima30 = BigDecimal.ZERO;
        private long quantidade;
        private UUID primeiroLancamentoId;
        private LocalDate vencimentoMaisAntigo;
        private long maiorAtrasoDias;

        private ContaPagarAcumulador(String categoria, String descricao) {
            this.categoria = categoria;
            this.descricao = descricao;
        }

        private void adicionar(LancamentoFinanceiroEntity lancamento, LocalDate hoje) {
            BigDecimal valor = valorSeguro(lancamento.getValor());
            LocalDate vencimento = dataRecebimentoLocal(lancamento);
            long atraso = atrasoEmDias(lancamento, hoje);

            total = total.add(valor);
            quantidade++;
            if (vencimentoMaisAntigo == null || vencimento.isBefore(vencimentoMaisAntigo)) {
                vencimentoMaisAntigo = vencimento;
                primeiroLancamentoId = lancamento.getId();
            }
            maiorAtrasoDias = Math.max(maiorAtrasoDias, atraso);

            if (atraso <= 0) {
                aVencer = aVencer.add(valor);
            } else if (atraso <= 7) {
                vencidoAte7 = vencidoAte7.add(valor);
            } else if (atraso <= 15) {
                vencido8A15 = vencido8A15.add(valor);
            } else if (atraso <= 30) {
                vencido16A30 = vencido16A30.add(valor);
            } else {
                vencidoAcima30 = vencidoAcima30.add(valor);
            }
        }

        private ContaPagarGrupo toGrupo() {
            return new ContaPagarGrupo(
                    categoria,
                    descricao,
                    total,
                    aVencer,
                    vencidoAte7,
                    vencido8A15,
                    vencido16A30,
                    vencidoAcima30,
                    quantidade,
                    primeiroLancamentoId,
                    vencimentoMaisAntigo,
                    maiorAtrasoDias
            );
        }
    }

    private static final class FaturamentoAcumulador {
        private final String nome;
        private long quantidade;
        private BigDecimal total = BigDecimal.ZERO;

        private FaturamentoAcumulador(String nome) {
            this.nome = nome;
        }

        private void adicionar(BigDecimal valor) {
            quantidade++;
            total = total.add(valorSeguro(valor));
        }

        private String nome() {
            return nome;
        }

        private BigDecimal total() {
            return total;
        }

        private FaturamentoGrupo toGrupo(BigDecimal totalGeral) {
            return new FaturamentoGrupo(nome, quantidade, total, percentual(total, totalGeral));
        }
    }

    private record PeriodoFinanceiro(OffsetDateTime inicio, OffsetDateTime fim) {
    }
}
