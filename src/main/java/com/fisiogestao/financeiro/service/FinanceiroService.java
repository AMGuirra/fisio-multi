package com.fisiogestao.financeiro.service;

import com.fisiogestao.config.TimezoneConfig;
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

    public List<LancamentoFinanceiroEntity> listarTodos() {
        return lancamentoFinanceiroRepository.findAllByOrderByDataCompetenciaDescCreatedAtDesc();
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
        return lancamentoFinanceiroRepository.save(lancamento);
    }

    @Transactional
    public void registrarReceitaPacote(PacoteSessaoEntity pacote) {
        LancamentoFinanceiroEntity receita = LancamentoFinanceiroEntity.builder()
                .tipo(TipoLancamentoFinanceiro.RECEITA)
                .status(StatusLancamentoFinanceiro.PENDENTE)
                .descricao("Pacote de sessões: " + pacote.getNomePacote())
                .categoria("Pacotes")
                .valor(pacote.getValorTotal())
                .dataCompetencia(paraInicioDoDia(pacote.getDataInicio()))
                .cliente(pacote.getCliente())
                .pacoteSessao(pacote)
                .build();
        lancamentoFinanceiroRepository.save(receita);
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
    public void marcarComoPago(UUID lancamentoId) {
        LancamentoFinanceiroEntity lancamento = lancamentoFinanceiroRepository.findById(lancamentoId)
                .orElseThrow(() -> new BusinessException("Lançamento financeiro não encontrado."));

        if (lancamento.getStatus() != StatusLancamentoFinanceiro.PENDENTE) {
            throw new BusinessException("Apenas lançamentos pendentes podem ser marcados como pagos.");
        }

        lancamento.setStatus(StatusLancamentoFinanceiro.PAGO);
        lancamento.setDataPagamento(OffsetDateTime.now(ZoneId.of(TimezoneConfig.CLINIC_TIMEZONE)));
        lancamentoFinanceiroRepository.save(lancamento);
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
