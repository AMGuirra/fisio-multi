package com.fisiogestao.financeiro.service;

import com.fisiogestao.config.TimezoneConfig;
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

    public List<LancamentoFinanceiroEntity> listarTodos() {
        return lancamentoFinanceiroRepository.findAllByOrderByDataCompetenciaDescCreatedAtDesc();
    }

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

    public BigDecimal totalReceitasPendentes() {
        return somarPorFiltro(TipoLancamentoFinanceiro.RECEITA, StatusLancamentoFinanceiro.PENDENTE);
    }

    public BigDecimal totalDespesasPendentes() {
        return somarPorFiltro(TipoLancamentoFinanceiro.DESPESA, StatusLancamentoFinanceiro.PENDENTE);
    }

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
        lancamento.setDataPagamento(OffsetDateTime.now(ZoneId.of(TimezoneConfig.CLINIC_TIMEZONE)));
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
        return listarTodos().stream()
                .filter(lancamento -> lancamento.getTipo() == tipo && lancamento.getStatus() == status)
                .map(LancamentoFinanceiroEntity::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OffsetDateTime paraInicioDoDia(LocalDate data) {
        return data.atStartOfDay(ZoneId.of(TimezoneConfig.CLINIC_TIMEZONE)).toOffsetDateTime();
    }
}
