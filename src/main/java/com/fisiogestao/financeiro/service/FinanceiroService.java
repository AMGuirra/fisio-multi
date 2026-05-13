package com.fisiogestao.financeiro.service;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.config.ClinicTimezoneHolder;
import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.financeiro.LancamentoFinanceiroEntity;
import com.fisiogestao.financeiro.StatusLancamentoFinanceiro;
import com.fisiogestao.financeiro.TipoLancamentoFinanceiro;
import com.fisiogestao.financeiro.form.LancamentoFinanceiroForm;
import com.fisiogestao.financeiro.repository.LancamentoFinanceiroRepository;
import com.fisiogestao.pacientes.PacoteSessaoEntity;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinanceiroService {

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

    @Transactional(readOnly = true)
    public List<LancamentoFinanceiroEntity> listarNoPeriodo(LocalDate inicio, LocalDate fim) {
        ZoneId zone = ClinicTimezoneHolder.zoneId();
        OffsetDateTime dtInicio = inicio.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime dtFim = fim.plusDays(1).atStartOfDay(zone).toOffsetDateTime().minusNanos(1);
        return lancamentoFinanceiroRepository
                .findByDataCompetenciaBetweenOrderByDataCompetenciaDescCreatedAtDesc(dtInicio, dtFim);
    }

    @Transactional(readOnly = true)
    public ResumoFinanceiro calcularResumo(LocalDate inicio, LocalDate fim) {
        List<LancamentoFinanceiroEntity> lista = listarNoPeriodo(inicio, fim);
        long receitasPagasCount = lista.stream()
                .filter(l -> l.getTipo() == TipoLancamentoFinanceiro.RECEITA
                        && l.getStatus() == StatusLancamentoFinanceiro.PAGO)
                .count();
        long lancamentosAtivosCount = lista.stream()
                .filter(l -> l.getStatus() != StatusLancamentoFinanceiro.CANCELADO)
                .count();
        return new ResumoFinanceiro(
                somarFiltrado(lista, TipoLancamentoFinanceiro.RECEITA, StatusLancamentoFinanceiro.PAGO),
                somarFiltrado(lista, TipoLancamentoFinanceiro.RECEITA, StatusLancamentoFinanceiro.PENDENTE),
                somarFiltrado(lista, TipoLancamentoFinanceiro.DESPESA, StatusLancamentoFinanceiro.PAGO),
                somarFiltrado(lista, TipoLancamentoFinanceiro.DESPESA, StatusLancamentoFinanceiro.PENDENTE),
                receitasPagasCount,
                lancamentosAtivosCount
        );
    }

    private BigDecimal somarFiltrado(List<LancamentoFinanceiroEntity> lista,
                                     TipoLancamentoFinanceiro tipo,
                                     StatusLancamentoFinanceiro status) {
        return lista.stream()
                .filter(l -> l.getTipo() == tipo && l.getStatus() == status)
                .map(LancamentoFinanceiroEntity::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

        lancamento.setTipo(form.getTipo());
        lancamento.setDescricao(form.getDescricao().trim());
        lancamento.setCategoria(form.getCategoria().trim());
        lancamento.setValor(form.getValor());
        lancamento.setDataCompetencia(paraInicioDoDia(form.getDataCompetencia()));

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
                .valor(form.getValor())
                .dataCompetencia(paraInicioDoDia(form.getDataCompetencia()))
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
                .dataCompetencia(paraInicioDoDia(dataLocal))
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
    public void registrarReceitaPacote(PacoteSessaoEntity pacote) {
        LancamentoFinanceiroEntity receita = LancamentoFinanceiroEntity.builder()
                .tipo(TipoLancamentoFinanceiro.RECEITA)
                .status(StatusLancamentoFinanceiro.PENDENTE)
                .descricao("Plano de tratamento: " + pacote.getNomePacote())
                .categoria("Planos de tratamento")
                .valor(pacote.getValorTotal())
                .dataCompetencia(paraInicioDoDia(pacote.getDataInicio()))
                .cliente(pacote.getCliente())
                .pacoteSessao(pacote)
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
    public void marcarComoPago(UUID lancamentoId) {
        LancamentoFinanceiroEntity lancamento = lancamentoFinanceiroRepository.findById(lancamentoId)
                .orElseThrow(() -> new BusinessException("Lançamento financeiro não encontrado."));
        var snapshotAnterior = auditService.snapshotOf(lancamento);

        if (lancamento.getStatus() != StatusLancamentoFinanceiro.PENDENTE) {
            throw new BusinessException("Apenas lançamentos pendentes podem ser marcados como pagos.");
        }

        lancamento.setStatus(StatusLancamentoFinanceiro.PAGO);
        lancamento.setDataPagamento(ClinicTimezoneHolder.now());
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
        LancamentoFinanceiroEntity lancamento = lancamentoFinanceiroRepository.findById(lancamentoId)
                .orElseThrow(() -> new BusinessException("Lançamento financeiro não encontrado."));
        var snapshotAnterior = auditService.snapshotOf(lancamento);

        if (lancamento.getStatus() != StatusLancamentoFinanceiro.PAGO) {
            throw new BusinessException("Somente lançamentos pagos podem ter a baixa cancelada.");
        }

        lancamento.setStatus(StatusLancamentoFinanceiro.PENDENTE);
        lancamento.setDataPagamento(null);
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

    private BigDecimal somarPorFiltro(TipoLancamentoFinanceiro tipo, StatusLancamentoFinanceiro status) {
        return lancamentoFinanceiroRepository.sumValorByTipoAndStatus(tipo, status);
    }

    private OffsetDateTime paraInicioDoDia(LocalDate data) {
        return data.atStartOfDay(ClinicTimezoneHolder.zoneId()).toOffsetDateTime();
    }
}
