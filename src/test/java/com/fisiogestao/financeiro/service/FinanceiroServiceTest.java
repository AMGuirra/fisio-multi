package com.fisiogestao.financeiro.service;

import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.financeiro.LancamentoFinanceiroEntity;
import com.fisiogestao.financeiro.StatusLancamentoFinanceiro;
import com.fisiogestao.financeiro.TipoLancamentoFinanceiro;
import com.fisiogestao.financeiro.form.LancamentoFinanceiroForm;
import com.fisiogestao.financeiro.repository.LancamentoFinanceiroRepository;
import com.fisiogestao.shared.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class FinanceiroServiceTest {

    @Mock
    private LancamentoFinanceiroRepository lancamentoFinanceiroRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private FinanceiroService financeiroService;

    @Test
    void deveAtualizarLancamentoPendente() {
        LancamentoFinanceiroEntity lancamento = criarLancamento(StatusLancamentoFinanceiro.PENDENTE);
        LancamentoFinanceiroForm form = new LancamentoFinanceiroForm();
        form.setTipo(TipoLancamentoFinanceiro.DESPESA);
        form.setDescricao("Material terapêutico");
        form.setCategoria("Insumos");
        form.setValor(new BigDecimal("89.90"));
        form.setDataCompetencia(LocalDate.now().plusDays(2));

        when(lancamentoFinanceiroRepository.findById(lancamento.getId())).thenReturn(Optional.of(lancamento));
        when(lancamentoFinanceiroRepository.save(any(LancamentoFinanceiroEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        financeiroService.atualizarManual(lancamento.getId(), form);

        assertEquals(TipoLancamentoFinanceiro.DESPESA, lancamento.getTipo());
        assertEquals("Material terapêutico", lancamento.getDescricao());
        assertEquals("Insumos", lancamento.getCategoria());
        assertEquals(new BigDecimal("89.90"), lancamento.getValor());
    }

    @Test
    void deveCancelarLancamentoPendente() {
        LancamentoFinanceiroEntity lancamento = criarLancamento(StatusLancamentoFinanceiro.PENDENTE);

        when(lancamentoFinanceiroRepository.findById(lancamento.getId())).thenReturn(Optional.of(lancamento));
        when(lancamentoFinanceiroRepository.save(any(LancamentoFinanceiroEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        financeiroService.cancelar(lancamento.getId());

        assertEquals(StatusLancamentoFinanceiro.CANCELADO, lancamento.getStatus());
        assertEquals(null, lancamento.getDataPagamento());
    }

    @Test
    void deveImpedirCancelamentoDeLancamentoPago() {
        LancamentoFinanceiroEntity lancamento = criarLancamento(StatusLancamentoFinanceiro.PAGO);

        when(lancamentoFinanceiroRepository.findById(lancamento.getId())).thenReturn(Optional.of(lancamento));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        BusinessException ex = assertThrows(BusinessException.class, () -> financeiroService.cancelar(lancamento.getId()));

        assertEquals("Somente lançamentos pendentes podem ser cancelados.", ex.getMessage());
    }

    private LancamentoFinanceiroEntity criarLancamento(StatusLancamentoFinanceiro status) {
        LancamentoFinanceiroEntity lancamento = LancamentoFinanceiroEntity.builder()
                .tipo(TipoLancamentoFinanceiro.RECEITA)
                .status(status)
                .descricao("Plano de sessões")
                .categoria("Pacotes")
                .valor(new BigDecimal("100.00"))
                .dataCompetencia(LocalDate.now().atStartOfDay(ZoneId.of("America/Campo_Grande")).toOffsetDateTime())
                .dataPagamento(status == StatusLancamentoFinanceiro.PAGO ? OffsetDateTime.now(ZoneId.of("America/Campo_Grande")) : null)
                .build();
        lancamento.setId(UUID.randomUUID());
        return lancamento;
    }
}
