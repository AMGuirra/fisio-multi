package com.fisiogestao.financeiro.service;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.financeiro.LancamentoFinanceiroEntity;
import com.fisiogestao.financeiro.StatusLancamentoFinanceiro;
import com.fisiogestao.financeiro.TipoLancamentoFinanceiro;
import com.fisiogestao.financeiro.form.LancamentoFinanceiroForm;
import com.fisiogestao.financeiro.repository.LancamentoFinanceiroRepository;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.PlanoSessaoEntity;
import com.fisiogestao.pacientes.PlanoSessaoStatus;
import com.fisiogestao.shared.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceiroServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Campo_Grande");

    @Mock
    private LancamentoFinanceiroRepository lancamentoFinanceiroRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private FinanceiroService financeiroService;

    // -----------------------------------------------------------------------
    // criarManual
    // -----------------------------------------------------------------------

    @Test
    void deveCriarLancamentoManualComStatusPendente() {
        LancamentoFinanceiroForm form = formReceita("Consulta avulsa", "Sessões", "120.00");
        when(lancamentoFinanceiroRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        LancamentoFinanceiroEntity salvo = financeiroService.criarManual(form);

        assertEquals(TipoLancamentoFinanceiro.RECEITA, salvo.getTipo());
        assertEquals(StatusLancamentoFinanceiro.PENDENTE, salvo.getStatus());
        assertEquals("Consulta avulsa", salvo.getDescricao());
        assertEquals(new BigDecimal("120.00"), salvo.getValor());
    }

    // -----------------------------------------------------------------------
    // atualizarManual
    // -----------------------------------------------------------------------

    @Test
    void deveAtualizarLancamentoPendente() {
        LancamentoFinanceiroEntity lancamento = lancamento(TipoLancamentoFinanceiro.RECEITA, StatusLancamentoFinanceiro.PENDENTE, "100.00");
        LancamentoFinanceiroForm form = formDespesa("Material terapêutico", "Insumos", "89.90");

        when(lancamentoFinanceiroRepository.findById(lancamento.getId())).thenReturn(Optional.of(lancamento));
        when(lancamentoFinanceiroRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        financeiroService.atualizarManual(lancamento.getId(), form);

        assertEquals(TipoLancamentoFinanceiro.DESPESA, lancamento.getTipo());
        assertEquals("Material terapêutico", lancamento.getDescricao());
        assertEquals(new BigDecimal("89.90"), lancamento.getValor());
    }

    @Test
    void deveImpedirEdicaoDeLancamentoCancelado() {
        LancamentoFinanceiroEntity lancamento = lancamento(TipoLancamentoFinanceiro.RECEITA, StatusLancamentoFinanceiro.CANCELADO, "100.00");
        LancamentoFinanceiroForm form = formReceita("Qualquer", "Cat", "50.00");

        when(lancamentoFinanceiroRepository.findById(lancamento.getId())).thenReturn(Optional.of(lancamento));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        assertThrows(BusinessException.class,
                () -> financeiroService.atualizarManual(lancamento.getId(), form));
        verify(lancamentoFinanceiroRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // cancelar
    // -----------------------------------------------------------------------

    @Test
    void deveCancelarLancamentoPendente() {
        LancamentoFinanceiroEntity lancamento = lancamento(TipoLancamentoFinanceiro.RECEITA, StatusLancamentoFinanceiro.PENDENTE, "100.00");

        when(lancamentoFinanceiroRepository.findById(lancamento.getId())).thenReturn(Optional.of(lancamento));
        when(lancamentoFinanceiroRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        financeiroService.cancelar(lancamento.getId());

        assertEquals(StatusLancamentoFinanceiro.CANCELADO, lancamento.getStatus());
        assertNull(lancamento.getDataPagamento());
    }

    @Test
    void deveImpedirCancelamentoDeLancamentoPago() {
        LancamentoFinanceiroEntity lancamento = lancamentoPago("150.00");

        when(lancamentoFinanceiroRepository.findById(lancamento.getId())).thenReturn(Optional.of(lancamento));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> financeiroService.cancelar(lancamento.getId()));
        assertEquals("Somente lançamentos pendentes podem ser cancelados.", ex.getMessage());
    }

    @Test
    void deveIgnorarCancelamentoDuplo() {
        LancamentoFinanceiroEntity lancamento = lancamento(TipoLancamentoFinanceiro.DESPESA, StatusLancamentoFinanceiro.CANCELADO, "30.00");

        when(lancamentoFinanceiroRepository.findById(lancamento.getId())).thenReturn(Optional.of(lancamento));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        // Nao deve lancar excecao nem salvar novamente
        financeiroService.cancelar(lancamento.getId());
        verify(lancamentoFinanceiroRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // marcarComoPago
    // -----------------------------------------------------------------------

    @Test
    void deveMarcarComoPagoDefinindoDataPagamento() {
        LancamentoFinanceiroEntity lancamento = lancamento(TipoLancamentoFinanceiro.RECEITA, StatusLancamentoFinanceiro.PENDENTE, "200.00");

        when(lancamentoFinanceiroRepository.findById(lancamento.getId())).thenReturn(Optional.of(lancamento));
        when(lancamentoFinanceiroRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        financeiroService.marcarComoPago(lancamento.getId());

        assertEquals(StatusLancamentoFinanceiro.PAGO, lancamento.getStatus());
        assertNotNull(lancamento.getDataPagamento());
    }

    @Test
    void deveImpedirBaixaDeLancamentoJaPago() {
        LancamentoFinanceiroEntity lancamento = lancamentoPago("100.00");

        when(lancamentoFinanceiroRepository.findById(lancamento.getId())).thenReturn(Optional.of(lancamento));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> financeiroService.marcarComoPago(lancamento.getId()));
        assertEquals("Apenas lançamentos pendentes podem ser marcados como pagos.", ex.getMessage());
    }

    @Test
    void deveImpedirBaixaDeLancamentoCancelado() {
        LancamentoFinanceiroEntity lancamento = lancamento(TipoLancamentoFinanceiro.RECEITA, StatusLancamentoFinanceiro.CANCELADO, "50.00");

        when(lancamentoFinanceiroRepository.findById(lancamento.getId())).thenReturn(Optional.of(lancamento));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        assertThrows(BusinessException.class,
                () -> financeiroService.marcarComoPago(lancamento.getId()));
    }

    // -----------------------------------------------------------------------
    // cancelarBaixa
    // -----------------------------------------------------------------------

    @Test
    void deveCancelarBaixaVoltandoParaPendente() {
        LancamentoFinanceiroEntity lancamento = lancamentoPago("300.00");

        when(lancamentoFinanceiroRepository.findById(lancamento.getId())).thenReturn(Optional.of(lancamento));
        when(lancamentoFinanceiroRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        financeiroService.cancelarBaixa(lancamento.getId());

        assertEquals(StatusLancamentoFinanceiro.PENDENTE, lancamento.getStatus());
        assertNull(lancamento.getDataPagamento());
    }

    @Test
    void deveImpedirCancelamentoDeBaixaEmLancamentoPendente() {
        LancamentoFinanceiroEntity lancamento = lancamento(TipoLancamentoFinanceiro.DESPESA, StatusLancamentoFinanceiro.PENDENTE, "60.00");

        when(lancamentoFinanceiroRepository.findById(lancamento.getId())).thenReturn(Optional.of(lancamento));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> financeiroService.cancelarBaixa(lancamento.getId()));
        assertEquals("Somente lançamentos pagos podem ter a baixa cancelada.", ex.getMessage());
    }

    // -----------------------------------------------------------------------
    // registrarReceitaPlano
    // -----------------------------------------------------------------------

    @Test
    void deveRegistrarReceitaAoVenderPlano() {
        ClienteEntity cliente = cliente("João");
        PlanoSessaoEntity plano = plano(cliente, "Plano Mensal", "500.00", LocalDate.now());

        ArgumentCaptor<LancamentoFinanceiroEntity> captor = ArgumentCaptor.forClass(LancamentoFinanceiroEntity.class);
        when(lancamentoFinanceiroRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        financeiroService.registrarReceitaPlano(plano);

        LancamentoFinanceiroEntity salvo = captor.getValue();
        assertEquals(TipoLancamentoFinanceiro.RECEITA, salvo.getTipo());
        assertEquals(StatusLancamentoFinanceiro.PENDENTE, salvo.getStatus());
        assertEquals(new BigDecimal("500.00"), salvo.getValor());
        assertEquals("Planos de tratamento", salvo.getCategoria());
        assertEquals(cliente, salvo.getCliente());
        assertEquals(plano, salvo.getPlanoSessao());
    }

    @Test
    void descricaoDaReceitaDePlanoDeveConterNomeDoPlano() {
        PlanoSessaoEntity plano = plano(cliente("Ana"), "Fisio Intensivo", "800.00", LocalDate.now());

        ArgumentCaptor<LancamentoFinanceiroEntity> captor = ArgumentCaptor.forClass(LancamentoFinanceiroEntity.class);
        when(lancamentoFinanceiroRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        financeiroService.registrarReceitaPlano(plano);

        assertEquals("Plano de tratamento: Fisio Intensivo", captor.getValue().getDescricao());
    }

    // -----------------------------------------------------------------------
    // registrarReceitaAvulso
    // -----------------------------------------------------------------------

    @Test
    void deveRegistrarReceitaAvulsoComValor() {
        SessaoEntity sessao = sessaoAvulsa("Pilates", new BigDecimal("90.00"));

        ArgumentCaptor<LancamentoFinanceiroEntity> captor = ArgumentCaptor.forClass(LancamentoFinanceiroEntity.class);
        when(lancamentoFinanceiroRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        financeiroService.registrarReceitaAvulso(sessao);

        LancamentoFinanceiroEntity salvo = captor.getValue();
        assertEquals(TipoLancamentoFinanceiro.RECEITA, salvo.getTipo());
        assertEquals(StatusLancamentoFinanceiro.PENDENTE, salvo.getStatus());
        assertEquals(new BigDecimal("90.00"), salvo.getValor());
        assertEquals("Sessões avulsas", salvo.getCategoria());
    }

    @Test
    void naoDeveRegistrarReceitaAvulsoSemValor() {
        SessaoEntity sessao = sessaoAvulsa("RPG", null);

        financeiroService.registrarReceitaAvulso(sessao);

        verify(lancamentoFinanceiroRepository, never()).save(any());
    }

    @Test
    void naoDeveRegistrarReceitaAvulsoComValorZero() {
        SessaoEntity sessao = sessaoAvulsa("Acupuntura", BigDecimal.ZERO);

        financeiroService.registrarReceitaAvulso(sessao);

        verify(lancamentoFinanceiroRepository, never()).save(any());
    }

    @Test
    void descricaoAvulsoDeveUsarTipoDeAtendimento() {
        SessaoEntity sessao = sessaoAvulsa("Pilates Solo", new BigDecimal("75.00"));

        ArgumentCaptor<LancamentoFinanceiroEntity> captor = ArgumentCaptor.forClass(LancamentoFinanceiroEntity.class);
        when(lancamentoFinanceiroRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        financeiroService.registrarReceitaAvulso(sessao);

        assertEquals("Sessão avulsa: Pilates Solo", captor.getValue().getDescricao());
    }

    @Test
    void descricaoAvulsoUsaFisioterapiaComoFallback() {
        SessaoEntity sessao = sessaoAvulsa(null, new BigDecimal("60.00"));

        ArgumentCaptor<LancamentoFinanceiroEntity> captor = ArgumentCaptor.forClass(LancamentoFinanceiroEntity.class);
        when(lancamentoFinanceiroRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        financeiroService.registrarReceitaAvulso(sessao);

        assertEquals("Sessão avulsa: Fisioterapia", captor.getValue().getDescricao());
    }

    // -----------------------------------------------------------------------
    // calcularResumo
    // -----------------------------------------------------------------------

    @Test
    void deveCalcularSaldoRealizadoESaldoProjetado() {
        mockResumo("350.00", "100.00", "80.00", "50.00", 2, 5);

        FinanceiroService.ResumoFinanceiro resumo =
                financeiroService.calcularResumo(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        // receitasRecebidas = 200 + 150 = 350
        assertEquals(new BigDecimal("350.00"), resumo.receitasRecebidas());
        assertEquals(new BigDecimal("100.00"), resumo.receitasPendentes());
        assertEquals(new BigDecimal("80.00"),  resumo.despesasPagas());
        assertEquals(new BigDecimal("50.00"),  resumo.despesasPendentes());
        // saldoRealizado = 350 - 80 = 270
        assertEquals(new BigDecimal("270.00"), resumo.saldoRealizado());
        // saldoProjetado = 350 + 100 - 80 - 50 = 320
        assertEquals(new BigDecimal("320.00"), resumo.saldoProjetado());
    }

    @Test
    void canceladosNaoDevemEntrarNoCalculo() {
        mockResumo("300.00", "0.00", "0.00", "0.00", 1, 1);

        FinanceiroService.ResumoFinanceiro resumo =
                financeiroService.calcularResumo(LocalDate.now(), LocalDate.now());

        assertEquals(new BigDecimal("300.00"), resumo.receitasRecebidas());
        assertEquals(0, BigDecimal.ZERO.compareTo(resumo.despesasPagas()));
    }

    @Test
    void deveCalcularTicketMedio() {
        mockResumo("600.00", "0.00", "0.00", "0.00", 3, 3);

        FinanceiroService.ResumoFinanceiro resumo =
                financeiroService.calcularResumo(LocalDate.now(), LocalDate.now());

        // ticketMedio = 600 / 3 = 200
        assertEquals(new BigDecimal("200.00"), resumo.ticketMedio());
    }

    @Test
    void ticketMedioDeveSerZeroSemReceitas() {
        mockResumo("0.00", "0.00", "0.00", "0.00", 0, 0);

        FinanceiroService.ResumoFinanceiro resumo =
                financeiroService.calcularResumo(LocalDate.now(), LocalDate.now());

        assertEquals(BigDecimal.ZERO, resumo.ticketMedio());
    }

    @Test
    void deveCalcularTaxaRecebimento() {
        // 350 recebido de 450 total → 350*100/450 = 77.7... → arredonda para 77.8 → "77,8%"
        mockResumo("350.00", "100.00", "0.00", "0.00", 2, 3);

        assertEquals("77,8%", financeiroService.calcularResumo(LocalDate.now(), LocalDate.now()).taxaRecebimento());
    }

    @Test
    void taxaRecebimentoDeveSerZeroQuandoNaoHaReceitas() {
        mockResumo("0.00", "0.00", "0.00", "0.00", 0, 0);

        assertEquals("0,0%", financeiroService.calcularResumo(LocalDate.now(), LocalDate.now()).taxaRecebimento());
    }

    @Test
    void taxaToneDeveSerSuccessQuandoAcimaDe80Porcento() {
        // 800/1000 = 80% exato → success
        mockResumo("800.00", "200.00", "0.00", "0.00", 1, 2);

        assertEquals("success", financeiroService.calcularResumo(LocalDate.now(), LocalDate.now()).taxaTone());
    }

    @Test
    void taxaToneDeveSerWarningEntre50E80Porcento() {
        // 600/1000 = 60% → warning
        mockResumo("600.00", "400.00", "0.00", "0.00", 1, 2);

        assertEquals("warning", financeiroService.calcularResumo(LocalDate.now(), LocalDate.now()).taxaTone());
    }

    @Test
    void taxaToneDeveSerDangerAbaixoDe50Porcento() {
        // 300/1000 = 30% → danger
        mockResumo("300.00", "700.00", "0.00", "0.00", 1, 2);

        assertEquals("danger", financeiroService.calcularResumo(LocalDate.now(), LocalDate.now()).taxaTone());
    }

    @Test
    void taxaToneDeveSerNeutralSemReceitas() {
        mockResumo("0.00", "0.00", "0.00", "0.00", 0, 0);

        assertEquals("neutral", financeiroService.calcularResumo(LocalDate.now(), LocalDate.now()).taxaTone());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void mockResumo(String receitasRecebidas,
                            String receitasPendentes,
                            String despesasPagas,
                            String despesasPendentes,
                            long receitasPagasCount,
                            long lancamentosAtivosCount) {
        when(lancamentoFinanceiroRepository.sumValorByPeriodoAndTipoAndStatus(
                any(), any(), eq(TipoLancamentoFinanceiro.RECEITA), eq(StatusLancamentoFinanceiro.PAGO)))
                .thenReturn(new BigDecimal(receitasRecebidas));
        when(lancamentoFinanceiroRepository.sumValorByPeriodoAndTipoAndStatus(
                any(), any(), eq(TipoLancamentoFinanceiro.RECEITA), eq(StatusLancamentoFinanceiro.PENDENTE)))
                .thenReturn(new BigDecimal(receitasPendentes));
        when(lancamentoFinanceiroRepository.sumValorByPeriodoAndTipoAndStatus(
                any(), any(), eq(TipoLancamentoFinanceiro.DESPESA), eq(StatusLancamentoFinanceiro.PAGO)))
                .thenReturn(new BigDecimal(despesasPagas));
        when(lancamentoFinanceiroRepository.sumValorByPeriodoAndTipoAndStatus(
                any(), any(), eq(TipoLancamentoFinanceiro.DESPESA), eq(StatusLancamentoFinanceiro.PENDENTE)))
                .thenReturn(new BigDecimal(despesasPendentes));
        when(lancamentoFinanceiroRepository.countByDataCompetenciaBetweenAndTipoAndStatus(
                any(), any(), eq(TipoLancamentoFinanceiro.RECEITA), eq(StatusLancamentoFinanceiro.PAGO)))
                .thenReturn(receitasPagasCount);
        when(lancamentoFinanceiroRepository.countByDataCompetenciaBetweenAndStatusNot(
                any(), any(), eq(StatusLancamentoFinanceiro.CANCELADO)))
                .thenReturn(lancamentosAtivosCount);
    }

    private LancamentoFinanceiroEntity lancamento(TipoLancamentoFinanceiro tipo,
                                                   StatusLancamentoFinanceiro status,
                                                   String valor) {
        LancamentoFinanceiroEntity l = LancamentoFinanceiroEntity.builder()
                .tipo(tipo)
                .status(status)
                .descricao("Lançamento teste")
                .categoria("Teste")
                .valor(new BigDecimal(valor))
                .dataCompetencia(LocalDate.now().atStartOfDay(ZONA).toOffsetDateTime())
                .dataPagamento(status == StatusLancamentoFinanceiro.PAGO
                        ? OffsetDateTime.now(ZONA) : null)
                .build();
        l.setId(UUID.randomUUID());
        return l;
    }

    private LancamentoFinanceiroEntity lancamentoPago(String valor) {
        return lancamento(TipoLancamentoFinanceiro.RECEITA, StatusLancamentoFinanceiro.PAGO, valor);
    }

    private LancamentoFinanceiroForm formReceita(String descricao, String categoria, String valor) {
        LancamentoFinanceiroForm form = new LancamentoFinanceiroForm();
        form.setTipo(TipoLancamentoFinanceiro.RECEITA);
        form.setDescricao(descricao);
        form.setCategoria(categoria);
        form.setValor(new BigDecimal(valor));
        form.setDataCompetencia(LocalDate.now());
        return form;
    }

    private LancamentoFinanceiroForm formDespesa(String descricao, String categoria, String valor) {
        LancamentoFinanceiroForm form = new LancamentoFinanceiroForm();
        form.setTipo(TipoLancamentoFinanceiro.DESPESA);
        form.setDescricao(descricao);
        form.setCategoria(categoria);
        form.setValor(new BigDecimal(valor));
        form.setDataCompetencia(LocalDate.now());
        return form;
    }

    private ClienteEntity cliente(String nome) {
        ClienteEntity c = ClienteEntity.builder().nomeCompleto(nome).ativo(true).build();
        c.setId(UUID.randomUUID());
        return c;
    }

    private PlanoSessaoEntity plano(ClienteEntity cliente, String nome, String valor, LocalDate dataInicio) {
        PlanoSessaoEntity p = PlanoSessaoEntity.builder()
                .cliente(cliente)
                .nomePlano(nome)
                .sessoesContratadas(10)
                .sessoesConsumidas(0)
                .valorTotal(new BigDecimal(valor))
                .dataInicio(dataInicio)
                .status(PlanoSessaoStatus.ATIVO)
                .build();
        p.setId(UUID.randomUUID());
        return p;
    }

    private SessaoEntity sessaoAvulsa(String tipoAtendimento, BigDecimal valorAvulso) {
        ClienteEntity cliente = cliente("Paciente");
        SessaoEntity sessao = SessaoEntity.builder()
                .cliente(cliente)
                .dataHoraInicio(OffsetDateTime.now(ZONA))
                .dataHoraFim(OffsetDateTime.now(ZONA).plusMinutes(50))
                .tipoAtendimento(tipoAtendimento)
                .valorAvulso(valorAvulso)
                .build();
        sessao.setId(UUID.randomUUID());
        return sessao;
    }
}
