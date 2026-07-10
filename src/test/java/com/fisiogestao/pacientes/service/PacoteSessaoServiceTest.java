package com.fisiogestao.pacientes.service;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.TipoAgendamento;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.financeiro.LancamentoFinanceiroEntity;
import com.fisiogestao.financeiro.StatusLancamentoFinanceiro;
import com.fisiogestao.financeiro.repository.LancamentoFinanceiroRepository;
import com.fisiogestao.financeiro.service.FinanceiroService;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.PlanoSessaoEntity;
import com.fisiogestao.pacientes.PlanoSessaoStatus;
import com.fisiogestao.pacientes.form.PlanoSessaoForm;
import com.fisiogestao.pacientes.repository.PlanoSessaoRepository;
import com.fisiogestao.shared.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanoSessaoServiceTest {

    @Mock
    private PlanoSessaoRepository planoSessaoRepository;

    @Mock
    private PacienteService pacienteService;

    @Mock
    private FinanceiroService financeiroService;

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private LancamentoFinanceiroRepository lancamentoFinanceiroRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PlanoSessaoService planoSessaoService;

    @Test
    void ajusteDeSessoesDoPacienteDeveConsumirPendenciasComSaldoDisponivel() {
        ClienteEntity clienteObj = cliente("Maria");
        PlanoSessaoEntity plano = pacoteAtivo(10, 3);
        plano.setCliente(clienteObj);
        SessaoEntity s1 = sessaoPlano(clienteObj, null, SessaoStatus.REALIZADA);
        SessaoEntity s2 = sessaoPlano(clienteObj, plano, SessaoStatus.FALTA_FATURADA);

        when(planoSessaoRepository.findById(plano.getId())).thenReturn(Optional.of(plano));
        when(sessaoRepository.findPendentesDeConsumoDoPacienteNoPlano(
                any(), any(), any(), any())).thenReturn(List.of(s1, s2));
        when(sessaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planoSessaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        var resultado = planoSessaoService.executarAjusteSessoesPaciente(clienteObj.getId(), plano.getId());

        assertEquals(2, resultado.sessoesAjustaveis());
        assertEquals(5, plano.getSessoesConsumidas());
        assertEquals(plano, s1.getPlanoSessao());
        assertTrue(s1.getPlanoConsumoRegistrado());
        assertTrue(s2.getPlanoConsumoRegistrado());
    }

    @Test
    void ajusteDeSessoesDoPacienteDeveCorrigirContadorQuandoHistoricoJaTemSessoesRealizadas() {
        ClienteEntity clienteObj = cliente("Maria");
        PlanoSessaoEntity plano = pacoteAtivo(3, 0);
        plano.setCliente(clienteObj);

        when(planoSessaoRepository.findById(plano.getId())).thenReturn(Optional.of(plano));
        when(sessaoRepository.findPendentesDeConsumoDoPacienteNoPlano(
                any(), any(), any(), any())).thenReturn(List.of());
        when(sessaoRepository.countContabilizaveisPorPlano(plano.getId())).thenReturn(3L);
        when(planoSessaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        var resultado = planoSessaoService.executarAjusteSessoesPaciente(clienteObj.getId(), plano.getId());

        assertEquals(3, resultado.sessoesPendentes());
        assertEquals(3, resultado.sessoesAjustaveis());
        assertEquals(3, plano.getSessoesConsumidas());
        assertEquals(PlanoSessaoStatus.CONCLUIDO, plano.getStatus());
    }

    @Test
    void ajusteDeSessoesDoPacienteDeveConsumirParcialmenteQuandoSaldoForInsuficiente() {
        ClienteEntity clienteObj = cliente("Maria");
        PlanoSessaoEntity plano = pacoteAtivo(4, 3);
        plano.setCliente(clienteObj);
        SessaoEntity s1 = sessaoPlano(clienteObj, null, SessaoStatus.REALIZADA);
        SessaoEntity s2 = sessaoPlano(clienteObj, null, SessaoStatus.REALIZADA);
        SessaoEntity s3 = sessaoPlano(clienteObj, null, SessaoStatus.FALTA_FATURADA);

        when(planoSessaoRepository.findById(plano.getId())).thenReturn(Optional.of(plano));
        when(sessaoRepository.findPendentesDeConsumoDoPacienteNoPlano(
                any(), any(), any(), any())).thenReturn(List.of(s1, s2, s3));
        when(sessaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planoSessaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        var resultado = planoSessaoService.executarAjusteSessoesPaciente(clienteObj.getId(), plano.getId());

        assertEquals(1, resultado.sessoesAjustaveis());
        assertEquals(2, resultado.sessoesExcedentes());
        assertTrue(resultado.sugerirNovoPlano());
        assertEquals(4, plano.getSessoesConsumidas());
        assertEquals(PlanoSessaoStatus.CONCLUIDO, plano.getStatus());
        assertTrue(s1.getPlanoConsumoRegistrado());
        assertFalse(s2.getPlanoConsumoRegistrado());
        assertFalse(s3.getPlanoConsumoRegistrado());
    }

    @Test
    void ajusteDeSessoesDoPacienteSemSaldoDeveSugerirNovoPlanoSemAlterarConsumo() {
        ClienteEntity clienteObj = cliente("Maria");
        PlanoSessaoEntity plano = pacoteComStatus(PlanoSessaoStatus.CONCLUIDO, 4, 4);
        plano.setCliente(clienteObj);
        SessaoEntity pendente = sessaoPlano(clienteObj, null, SessaoStatus.REALIZADA);

        when(planoSessaoRepository.findById(plano.getId())).thenReturn(Optional.of(plano));
        when(sessaoRepository.findPendentesDeConsumoDoPacienteNoPlano(
                any(), any(), any(), any())).thenReturn(List.of(pendente));

        var resultado = planoSessaoService.executarAjusteSessoesPaciente(clienteObj.getId(), plano.getId());

        assertEquals(0, resultado.sessoesAjustaveis());
        assertEquals(1, resultado.sessoesExcedentes());
        assertTrue(resultado.sugerirNovoPlano());
        assertEquals(4, plano.getSessoesConsumidas());
        assertFalse(pendente.getPlanoConsumoRegistrado());
        verify(planoSessaoRepository, never()).save(plano);
    }

    // -----------------------------------------------------------------------
    // criar — deve disparar registrarReceitaPlano
    // -----------------------------------------------------------------------

    @Test
    void deveRegistrarReceitaFinanceiraAoCriarPacote() {
        UUID clienteId = UUID.randomUUID();
        PlanoSessaoForm form = formPacote("Plano Mensal", 10, "450.00",
                LocalDate.now(), LocalDate.now().plusMonths(1));

        when(pacienteService.buscarPorId(clienteId)).thenReturn(cliente("Maria"));
        when(planoSessaoRepository.save(any())).thenAnswer(inv -> {
            PlanoSessaoEntity p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        planoSessaoService.criar(clienteId, form);

        verify(financeiroService).registrarReceitaPlano(any(PlanoSessaoEntity.class));
    }

    @Test
    void deveImpedirCriacaoComValidadeAnteriorAoInicio() {
        UUID clienteId = UUID.randomUUID();
        PlanoSessaoForm form = formPacote("Plano", 5, "200.00",
                LocalDate.now(), LocalDate.now().minusDays(1));

        assertThrows(BusinessException.class, () -> planoSessaoService.criar(clienteId, form));
        verify(planoSessaoRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // reservarSessao
    // -----------------------------------------------------------------------

    @Test
    void deveIncrementarSessoesConsumidasAoReservar() {
        PlanoSessaoEntity pacote = pacoteAtivo(5, 3);

        when(planoSessaoRepository.findById(pacote.getId())).thenReturn(Optional.of(pacote));
        when(planoSessaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        planoSessaoService.reservarSessao(pacote.getId());

        assertEquals(4, pacote.getSessoesConsumidas());
        assertEquals(PlanoSessaoStatus.ATIVO, pacote.getStatus());
    }

    @Test
    void deveConcluirPacoteQuandoUltimaSessionForReservada() {
        PlanoSessaoEntity pacote = pacoteAtivo(5, 4); // falta só 1

        when(planoSessaoRepository.findById(pacote.getId())).thenReturn(Optional.of(pacote));
        when(planoSessaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        planoSessaoService.reservarSessao(pacote.getId());

        assertEquals(5, pacote.getSessoesConsumidas());
        assertEquals(PlanoSessaoStatus.CONCLUIDO, pacote.getStatus());
    }

    @Test
    void deveLancarExcecaoAoReservarPacoteEsgotado() {
        PlanoSessaoEntity pacote = pacoteComStatus(PlanoSessaoStatus.CONCLUIDO, 5, 5);

        when(planoSessaoRepository.findById(pacote.getId())).thenReturn(Optional.of(pacote));

        assertThrows(BusinessException.class,
                () -> planoSessaoService.reservarSessao(pacote.getId()));
    }

    @Test
    void deveLancarExcecaoAoReservarPacoteCancelado() {
        PlanoSessaoEntity pacote = pacoteComStatus(PlanoSessaoStatus.CANCELADO, 10, 2);

        when(planoSessaoRepository.findById(pacote.getId())).thenReturn(Optional.of(pacote));

        assertThrows(BusinessException.class,
                () -> planoSessaoService.reservarSessao(pacote.getId()));
    }

    @Test
    void deveLancarExcecaoAoReservarPacoteComValidadeVencida() {
        PlanoSessaoEntity pacote = PlanoSessaoEntity.builder()
                .cliente(cliente("Teste"))
                .nomePlano("Plano Expirado")
                .sessoesContratadas(10)
                .sessoesConsumidas(2)
                .valorTotal(BigDecimal.TEN)
                .dataInicio(LocalDate.now().minusMonths(2))
                .dataValidade(LocalDate.now().minusDays(1)) // vencido ontem
                .status(PlanoSessaoStatus.ATIVO)
                .build();
        pacote.setId(UUID.randomUUID());

        when(planoSessaoRepository.findById(pacote.getId())).thenReturn(Optional.of(pacote));

        assertThrows(BusinessException.class,
                () -> planoSessaoService.reservarSessao(pacote.getId()));
    }

    // -----------------------------------------------------------------------
    // estornarSessao
    // -----------------------------------------------------------------------

    @Test
    void deveDecrementarSessoesConsumidasAoEstornar() {
        PlanoSessaoEntity pacote = pacoteComStatus(PlanoSessaoStatus.CONCLUIDO, 5, 5);

        when(planoSessaoRepository.findById(pacote.getId())).thenReturn(Optional.of(pacote));
        when(planoSessaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        planoSessaoService.estornarSessao(pacote.getId());

        assertEquals(4, pacote.getSessoesConsumidas());
        assertEquals(PlanoSessaoStatus.ATIVO, pacote.getStatus());
    }

    @Test
    void deveIgnorarEstornoQuandoSessoesConsumidasForZero() {
        PlanoSessaoEntity pacote = pacoteAtivo(5, 0);

        when(planoSessaoRepository.findById(pacote.getId())).thenReturn(Optional.of(pacote));

        planoSessaoService.estornarSessao(pacote.getId());

        verify(planoSessaoRepository, never()).save(any());
        assertEquals(0, pacote.getSessoesConsumidas());
    }

    // -----------------------------------------------------------------------
    // cancelar
    // -----------------------------------------------------------------------

    @Test
    void deveCancelarPlanoSemSessoesFuturas() {
        PlanoSessaoEntity pacote = pacoteAtivo(10, 0);

        when(planoSessaoRepository.findById(pacote.getId())).thenReturn(Optional.of(pacote));
        when(sessaoRepository.deletarFuturasPorPlanoEStatus(any(), any(), any())).thenReturn(0);
        when(planoSessaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        assertDoesNotThrow(() -> planoSessaoService.cancelar(pacote.getId()));

        assertEquals(PlanoSessaoStatus.CANCELADO, pacote.getStatus());
    }

    @Test
    void deveCancelarPlanoRemovendoSessoesFuturas() {
        PlanoSessaoEntity pacote = pacoteAtivo(10, 3);

        when(planoSessaoRepository.findById(pacote.getId())).thenReturn(Optional.of(pacote));
        when(sessaoRepository.deletarFuturasPorPlanoEStatus(any(), any(), any())).thenReturn(2);
        when(planoSessaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        assertDoesNotThrow(() -> planoSessaoService.cancelar(pacote.getId()));

        assertEquals(PlanoSessaoStatus.CANCELADO, pacote.getStatus());
    }

    // -----------------------------------------------------------------------
    // calcularSessoesDisponiveis
    // -----------------------------------------------------------------------

    @Test
    void deveCalcularSessoesDisponiveisPorDiferenca() {
        assertEquals(7, planoSessaoService.calcularSessoesDisponiveis(pacoteAtivo(10, 3)));
    }

    @Test
    void sessoesDisponiveisNuncaDeveSerNegativa() {
        // Cenário defensivo: consumidas > contratadas por inconsistência de dados
        assertEquals(0, planoSessaoService.calcularSessoesDisponiveis(pacoteAtivo(5, 7)));
    }

    // -----------------------------------------------------------------------
    // buscarPrimeiroDisponivel
    // -----------------------------------------------------------------------

    @Test
    void deveRetornarVazioQuandoNenhumPacoteDisponivel() {
        UUID clienteId = UUID.randomUUID();
        PlanoSessaoEntity concluido = pacoteComStatus(PlanoSessaoStatus.CONCLUIDO, 5, 5);
        PlanoSessaoEntity cancelado = pacoteComStatus(PlanoSessaoStatus.CANCELADO, 5, 0);

        when(planoSessaoRepository.findByClienteIdOrderByCreatedAtDesc(clienteId))
                .thenReturn(List.of(concluido, cancelado));

        assertTrue(planoSessaoService.buscarPrimeiroDisponivel(clienteId).isEmpty());
    }

    @Test
    void deveRetornarPrimeiroPacoteDisponivel() {
        UUID clienteId = UUID.randomUUID();
        PlanoSessaoEntity disponivel = pacoteAtivo(10, 2);

        when(planoSessaoRepository.findByClienteIdOrderByCreatedAtDesc(clienteId))
                .thenReturn(List.of(disponivel));

        assertTrue(planoSessaoService.buscarPrimeiroDisponivel(clienteId).isPresent());
    }

    @Test
    void deveIgnorarPacoteComSaldoComprometidoAoBuscarLivreParaAgenda() {
        UUID clienteId = UUID.randomUUID();
        PlanoSessaoEntity comprometido = pacoteAtivo(10, 9);
        PlanoSessaoEntity livre = pacoteAtivo(10, 3);

        when(planoSessaoRepository.findByClienteIdOrderByCreatedAtDesc(clienteId))
                .thenReturn(List.of(comprometido, livre));
        when(sessaoRepository.countAgendadasPorPlano(comprometido.getId())).thenReturn(1L);
        when(sessaoRepository.countAgendadasPorPlano(livre.getId())).thenReturn(0L);

        Optional<PlanoSessaoEntity> resultado = planoSessaoService.buscarPrimeiroLivreParaAgenda(clienteId);

        assertTrue(resultado.isPresent());
        assertEquals(livre.getId(), resultado.get().getId());
    }

    // -----------------------------------------------------------------------
    // excluir — cenários de regressao
    // -----------------------------------------------------------------------

    @Test
    void deveExcluirPacoteSemHistoricoRemovendoLancamentosPendentes() {
        UUID pacoteId = UUID.randomUUID();
        PlanoSessaoEntity pacote = pacoteComStatus(PlanoSessaoStatus.CANCELADO, 10, 0);
        pacote.setId(pacoteId);
        LancamentoFinanceiroEntity lancamento = lancamentoPendente(pacote);

        when(planoSessaoRepository.findById(pacoteId)).thenReturn(Optional.of(pacote));
        when(sessaoRepository.findByPlanoSessaoIdOrderByDataHoraInicioDesc(pacoteId)).thenReturn(List.of());
        when(lancamentoFinanceiroRepository.findByPlanoSessaoId(pacoteId)).thenReturn(List.of(lancamento));

        assertDoesNotThrow(() -> planoSessaoService.excluir(pacoteId));

        verify(lancamentoFinanceiroRepository).deleteAll(List.of(lancamento));
        verify(planoSessaoRepository).delete(pacote);
    }

    @Test
    void deveExcluirPacoteComHistoricoDeSessoesDesvinculandoAgenda() {
        UUID pacoteId = UUID.randomUUID();
        ClienteEntity clienteObj = cliente("Maria");
        PlanoSessaoEntity pacote = pacoteAtivo(10, 0);
        pacote.setId(pacoteId);
        SessaoEntity sessao = SessaoEntity.builder()
                .cliente(clienteObj)
                .planoSessao(pacote)
                .dataHoraInicio(OffsetDateTime.now().plusDays(1))
                .dataHoraFim(OffsetDateTime.now().plusDays(1).plusMinutes(50))
                .status(SessaoStatus.CANCELADA)
                .tipoAgendamento(TipoAgendamento.PLANO)
                .planoConsumoRegistrado(Boolean.FALSE)
                .build();
        sessao.setId(UUID.randomUUID());

        when(planoSessaoRepository.findById(pacoteId)).thenReturn(Optional.of(pacote));
        when(sessaoRepository.findByPlanoSessaoIdOrderByDataHoraInicioDesc(pacoteId)).thenReturn(List.of(sessao));
        when(sessaoRepository.save(sessao)).thenReturn(sessao);
        when(lancamentoFinanceiroRepository.findByPlanoSessaoId(pacoteId)).thenReturn(List.of());

        assertDoesNotThrow(() -> planoSessaoService.excluir(pacoteId));

        assertNull(sessao.getPlanoSessao());
        assertEquals(TipoAgendamento.AVULSO, sessao.getTipoAgendamento());
        assertFalse(sessao.getPlanoConsumoRegistrado());
        verify(planoSessaoRepository).delete(pacote);
    }

    @Test
    void deveBloquearExclusaoDePacoteComLancamentoPago() {
        UUID pacoteId = UUID.randomUUID();
        PlanoSessaoEntity pacote = pacoteAtivo(10, 3);
        pacote.setId(pacoteId);

        when(planoSessaoRepository.findById(pacoteId)).thenReturn(Optional.of(pacote));
        when(lancamentoFinanceiroRepository.existsByPlanoSessaoIdAndStatus(pacoteId, StatusLancamentoFinanceiro.PAGO))
                .thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> planoSessaoService.excluir(pacoteId));

        assertTrue(ex.getMessage().contains("lançamento financeiro pago"));
        verify(planoSessaoRepository, never()).delete(pacote);
    }

    @Test
    void deveBloquearExclusaoDePacoteComSessaoRealizadaOuFaltaFaturada() {
        UUID pacoteId = UUID.randomUUID();
        PlanoSessaoEntity pacote = pacoteAtivo(10, 3);
        pacote.setId(pacoteId);

        when(planoSessaoRepository.findById(pacoteId)).thenReturn(Optional.of(pacote));
        when(sessaoRepository.existsByPlanoSessaoIdAndStatusIn(
                pacoteId,
                List.of(SessaoStatus.REALIZADA, SessaoStatus.FALTA_FATURADA)
        )).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> planoSessaoService.excluir(pacoteId));

        assertTrue(ex.getMessage().contains("histórico de atendimento"));
        verify(planoSessaoRepository, never()).delete(pacote);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ClienteEntity cliente(String nome) {
        ClienteEntity c = ClienteEntity.builder().nomeCompleto(nome).ativo(true).build();
        c.setId(UUID.randomUUID());
        return c;
    }

    private PlanoSessaoEntity pacoteAtivo(int contratadas, int consumidas) {
        return pacoteComStatus(PlanoSessaoStatus.ATIVO, contratadas, consumidas);
    }

    private PlanoSessaoEntity pacoteComStatus(PlanoSessaoStatus status, int contratadas, int consumidas) {
        PlanoSessaoEntity p = PlanoSessaoEntity.builder()
                .cliente(cliente("Paciente"))
                .nomePlano("Plano Teste")
                .sessoesContratadas(contratadas)
                .sessoesConsumidas(consumidas)
                .valorTotal(new BigDecimal("300.00"))
                .dataInicio(LocalDate.now().minusDays(10))
                .dataValidade(LocalDate.now().plusMonths(3))
                .status(status)
                .build();
        p.setId(UUID.randomUUID());
        return p;
    }

    private PlanoSessaoForm formPacote(String nome, int sessoes, String valor,
                                        LocalDate inicio, LocalDate validade) {
        PlanoSessaoForm form = new PlanoSessaoForm();
        form.setNomePlano(nome);
        form.setSessoesContratadas(sessoes);
        form.setValorTotal(new BigDecimal(valor));
        form.setDataInicio(inicio);
        form.setDataValidade(validade);
        return form;
    }

    private LancamentoFinanceiroEntity lancamentoPendente(PlanoSessaoEntity pacote) {
        LancamentoFinanceiroEntity l = LancamentoFinanceiroEntity.builder()
                .status(StatusLancamentoFinanceiro.PENDENTE)
                .descricao("Plano de tratamento")
                .categoria("Planos de tratamento")
                .tipo(com.fisiogestao.financeiro.TipoLancamentoFinanceiro.RECEITA)
                .valor(BigDecimal.TEN)
                .dataRecebimento(LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime())
                .planoSessao(pacote)
                .build();
        l.setId(UUID.randomUUID());
        return l;
    }

    private SessaoEntity sessaoPlano(ClienteEntity clienteObj, PlanoSessaoEntity plano, SessaoStatus status) {
        SessaoEntity sessao = SessaoEntity.builder()
                .cliente(clienteObj)
                .planoSessao(plano)
                .dataHoraInicio(OffsetDateTime.now())
                .dataHoraFim(OffsetDateTime.now().plusMinutes(50))
                .status(status)
                .tipoAgendamento(TipoAgendamento.PLANO)
                .planoConsumoRegistrado(Boolean.FALSE)
                .build();
        sessao.setId(UUID.randomUUID());
        return sessao;
    }
}
