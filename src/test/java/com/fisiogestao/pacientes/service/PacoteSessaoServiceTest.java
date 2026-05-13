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
import com.fisiogestao.pacientes.PacoteSessaoEntity;
import com.fisiogestao.pacientes.PacoteSessaoStatus;
import com.fisiogestao.pacientes.form.PacoteSessaoForm;
import com.fisiogestao.pacientes.repository.PacoteSessaoRepository;
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
@SuppressWarnings("null")
class PacoteSessaoServiceTest {

    @Mock
    private PacoteSessaoRepository pacoteSessaoRepository;

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
    private PacoteSessaoService pacoteSessaoService;

    // -----------------------------------------------------------------------
    // criar — deve disparar registrarReceitaPacote
    // -----------------------------------------------------------------------

    @Test
    void deveRegistrarReceitaFinanceiraAoCriarPacote() {
        UUID clienteId = UUID.randomUUID();
        PacoteSessaoForm form = formPacote("Plano Mensal", 10, "450.00",
                LocalDate.now(), LocalDate.now().plusMonths(1));

        when(pacienteService.buscarPorId(clienteId)).thenReturn(cliente("Maria"));
        when(pacoteSessaoRepository.save(any())).thenAnswer(inv -> {
            PacoteSessaoEntity p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        pacoteSessaoService.criar(clienteId, form);

        verify(financeiroService).registrarReceitaPacote(any(PacoteSessaoEntity.class));
    }

    @Test
    void deveImpedirCriacaoComValidadeAnteriorAoInicio() {
        UUID clienteId = UUID.randomUUID();
        PacoteSessaoForm form = formPacote("Plano", 5, "200.00",
                LocalDate.now(), LocalDate.now().minusDays(1));

        when(pacienteService.buscarPorId(clienteId)).thenReturn(cliente("João"));

        assertThrows(BusinessException.class, () -> pacoteSessaoService.criar(clienteId, form));
        verify(pacoteSessaoRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // reservarSessao
    // -----------------------------------------------------------------------

    @Test
    void deveIncrementarSessoesConsumidasAoReservar() {
        PacoteSessaoEntity pacote = pacoteAtivo(5, 3);

        when(pacoteSessaoRepository.findById(pacote.getId())).thenReturn(Optional.of(pacote));
        when(pacoteSessaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        pacoteSessaoService.reservarSessao(pacote.getId());

        assertEquals(4, pacote.getSessoesConsumidas());
        assertEquals(PacoteSessaoStatus.ATIVO, pacote.getStatus());
    }

    @Test
    void deveConcluirPacoteQuandoUltimaSessionForReservada() {
        PacoteSessaoEntity pacote = pacoteAtivo(5, 4); // falta só 1

        when(pacoteSessaoRepository.findById(pacote.getId())).thenReturn(Optional.of(pacote));
        when(pacoteSessaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        pacoteSessaoService.reservarSessao(pacote.getId());

        assertEquals(5, pacote.getSessoesConsumidas());
        assertEquals(PacoteSessaoStatus.CONCLUIDO, pacote.getStatus());
    }

    @Test
    void deveLancarExcecaoAoReservarPacoteEsgotado() {
        PacoteSessaoEntity pacote = pacoteComStatus(PacoteSessaoStatus.CONCLUIDO, 5, 5);

        when(pacoteSessaoRepository.findById(pacote.getId())).thenReturn(Optional.of(pacote));

        assertThrows(BusinessException.class,
                () -> pacoteSessaoService.reservarSessao(pacote.getId()));
    }

    @Test
    void deveLancarExcecaoAoReservarPacoteCancelado() {
        PacoteSessaoEntity pacote = pacoteComStatus(PacoteSessaoStatus.CANCELADO, 10, 2);

        when(pacoteSessaoRepository.findById(pacote.getId())).thenReturn(Optional.of(pacote));

        assertThrows(BusinessException.class,
                () -> pacoteSessaoService.reservarSessao(pacote.getId()));
    }

    @Test
    void deveLancarExcecaoAoReservarPacoteComValidadeVencida() {
        PacoteSessaoEntity pacote = PacoteSessaoEntity.builder()
                .cliente(cliente("Teste"))
                .nomePacote("Plano Expirado")
                .sessoesContratadas(10)
                .sessoesConsumidas(2)
                .valorTotal(BigDecimal.TEN)
                .dataInicio(LocalDate.now().minusMonths(2))
                .dataValidade(LocalDate.now().minusDays(1)) // vencido ontem
                .status(PacoteSessaoStatus.ATIVO)
                .build();
        pacote.setId(UUID.randomUUID());

        when(pacoteSessaoRepository.findById(pacote.getId())).thenReturn(Optional.of(pacote));

        assertThrows(BusinessException.class,
                () -> pacoteSessaoService.reservarSessao(pacote.getId()));
    }

    // -----------------------------------------------------------------------
    // estornarSessao
    // -----------------------------------------------------------------------

    @Test
    void deveDecrementarSessoesConsumidasAoEstornar() {
        PacoteSessaoEntity pacote = pacoteComStatus(PacoteSessaoStatus.CONCLUIDO, 5, 5);

        when(pacoteSessaoRepository.findById(pacote.getId())).thenReturn(Optional.of(pacote));
        when(pacoteSessaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        pacoteSessaoService.estornarSessao(pacote.getId());

        assertEquals(4, pacote.getSessoesConsumidas());
        assertEquals(PacoteSessaoStatus.ATIVO, pacote.getStatus());
    }

    @Test
    void deveIgnorarEstornoQuandoSessoesConsumidasForZero() {
        PacoteSessaoEntity pacote = pacoteAtivo(5, 0);

        when(pacoteSessaoRepository.findById(pacote.getId())).thenReturn(Optional.of(pacote));

        pacoteSessaoService.estornarSessao(pacote.getId());

        verify(pacoteSessaoRepository, never()).save(any());
        assertEquals(0, pacote.getSessoesConsumidas());
    }

    // -----------------------------------------------------------------------
    // cancelar
    // -----------------------------------------------------------------------

    @Test
    void deveCancelarPacoteSemSessoesAtivas() {
        PacoteSessaoEntity pacote = pacoteAtivo(10, 0);

        when(pacoteSessaoRepository.findById(pacote.getId())).thenReturn(Optional.of(pacote));
        when(sessaoRepository.existsByPacoteSessaoIdAndStatusIn(any(), any())).thenReturn(false);
        when(pacoteSessaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        assertDoesNotThrow(() -> pacoteSessaoService.cancelar(pacote.getId()));

        assertEquals(PacoteSessaoStatus.CANCELADO, pacote.getStatus());
    }

    @Test
    void deveBloquearCancelamentoComSessoesAtivas() {
        PacoteSessaoEntity pacote = pacoteAtivo(10, 3);

        when(pacoteSessaoRepository.findById(pacote.getId())).thenReturn(Optional.of(pacote));
        when(sessaoRepository.existsByPacoteSessaoIdAndStatusIn(any(), any())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pacoteSessaoService.cancelar(pacote.getId()));

        assertTrue(ex.getMessage().contains("sessões confirmadas ou realizadas"));
    }

    // -----------------------------------------------------------------------
    // calcularSessoesDisponiveis
    // -----------------------------------------------------------------------

    @Test
    void deveCalcularSessoesDisponiveisPorDiferenca() {
        assertEquals(7, pacoteSessaoService.calcularSessoesDisponiveis(pacoteAtivo(10, 3)));
    }

    @Test
    void sessoesDisponiveisNuncaDeveSerNegativa() {
        // Cenário defensivo: consumidas > contratadas por inconsistência de dados
        assertEquals(0, pacoteSessaoService.calcularSessoesDisponiveis(pacoteAtivo(5, 7)));
    }

    // -----------------------------------------------------------------------
    // buscarPrimeiroDisponivel
    // -----------------------------------------------------------------------

    @Test
    void deveRetornarVazioQuandoNenhumPacoteDisponivel() {
        UUID clienteId = UUID.randomUUID();
        PacoteSessaoEntity concluido = pacoteComStatus(PacoteSessaoStatus.CONCLUIDO, 5, 5);
        PacoteSessaoEntity cancelado = pacoteComStatus(PacoteSessaoStatus.CANCELADO, 5, 0);

        when(pacoteSessaoRepository.findByClienteIdOrderByCreatedAtDesc(clienteId))
                .thenReturn(List.of(concluido, cancelado));

        assertTrue(pacoteSessaoService.buscarPrimeiroDisponivel(clienteId).isEmpty());
    }

    @Test
    void deveRetornarPrimeiroPacoteDisponivel() {
        UUID clienteId = UUID.randomUUID();
        PacoteSessaoEntity disponivel = pacoteAtivo(10, 2);

        when(pacoteSessaoRepository.findByClienteIdOrderByCreatedAtDesc(clienteId))
                .thenReturn(List.of(disponivel));

        assertTrue(pacoteSessaoService.buscarPrimeiroDisponivel(clienteId).isPresent());
    }

    // -----------------------------------------------------------------------
    // excluir — cenários de regressao
    // -----------------------------------------------------------------------

    @Test
    void deveExcluirPacoteSemHistoricoRemovendoLancamentosPendentes() {
        UUID pacoteId = UUID.randomUUID();
        PacoteSessaoEntity pacote = pacoteComStatus(PacoteSessaoStatus.CANCELADO, 10, 0);
        pacote.setId(pacoteId);
        LancamentoFinanceiroEntity lancamento = lancamentoPendente(pacote);

        when(pacoteSessaoRepository.findById(pacoteId)).thenReturn(Optional.of(pacote));
        when(sessaoRepository.findByPacoteSessaoIdOrderByDataHoraInicioDesc(pacoteId)).thenReturn(List.of());
        when(lancamentoFinanceiroRepository.findByPacoteSessaoId(pacoteId)).thenReturn(List.of(lancamento));

        assertDoesNotThrow(() -> pacoteSessaoService.excluir(pacoteId));

        verify(lancamentoFinanceiroRepository).deleteAll(List.of(lancamento));
        verify(pacoteSessaoRepository).delete(pacote);
    }

    @Test
    void deveExcluirPacoteComHistoricoDeSessoesDesvinculandoAgenda() {
        UUID pacoteId = UUID.randomUUID();
        ClienteEntity clienteObj = cliente("Maria");
        PacoteSessaoEntity pacote = pacoteAtivo(10, 0);
        pacote.setId(pacoteId);
        SessaoEntity sessao = SessaoEntity.builder()
                .cliente(clienteObj)
                .pacoteSessao(pacote)
                .dataHoraInicio(OffsetDateTime.now().plusDays(1))
                .dataHoraFim(OffsetDateTime.now().plusDays(1).plusMinutes(50))
                .status(SessaoStatus.CANCELADA)
                .tipoAgendamento(TipoAgendamento.PACOTE)
                .pacoteConsumoRegistrado(Boolean.FALSE)
                .build();
        sessao.setId(UUID.randomUUID());

        when(pacoteSessaoRepository.findById(pacoteId)).thenReturn(Optional.of(pacote));
        when(sessaoRepository.findByPacoteSessaoIdOrderByDataHoraInicioDesc(pacoteId)).thenReturn(List.of(sessao));
        when(sessaoRepository.save(sessao)).thenReturn(sessao);
        when(lancamentoFinanceiroRepository.findByPacoteSessaoId(pacoteId)).thenReturn(List.of());

        assertDoesNotThrow(() -> pacoteSessaoService.excluir(pacoteId));

        assertNull(sessao.getPacoteSessao());
        assertEquals(TipoAgendamento.AVULSO, sessao.getTipoAgendamento());
        assertFalse(sessao.getPacoteConsumoRegistrado());
        verify(pacoteSessaoRepository).delete(pacote);
    }

    @Test
    void devePreservarLancamentoPagoAoExcluirPacote() {
        UUID pacoteId = UUID.randomUUID();
        PacoteSessaoEntity pacote = pacoteAtivo(10, 3);
        pacote.setId(pacoteId);
        LancamentoFinanceiroEntity lancamentoPago = LancamentoFinanceiroEntity.builder()
                .status(StatusLancamentoFinanceiro.PAGO)
                .descricao("Plano de tratamento")
                .categoria("Planos de tratamento")
                .tipo(com.fisiogestao.financeiro.TipoLancamentoFinanceiro.RECEITA)
                .valor(BigDecimal.TEN)
                .dataCompetencia(LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime())
                .pacoteSessao(pacote)
                .build();
        lancamentoPago.setId(UUID.randomUUID());

        when(pacoteSessaoRepository.findById(pacoteId)).thenReturn(Optional.of(pacote));
        when(sessaoRepository.findByPacoteSessaoIdOrderByDataHoraInicioDesc(pacoteId)).thenReturn(List.of());
        when(lancamentoFinanceiroRepository.findByPacoteSessaoId(pacoteId)).thenReturn(List.of(lancamentoPago));
        when(lancamentoFinanceiroRepository.save(lancamentoPago)).thenReturn(lancamentoPago);

        assertDoesNotThrow(() -> pacoteSessaoService.excluir(pacoteId));

        assertNull(lancamentoPago.getPacoteSessao());
        verify(lancamentoFinanceiroRepository, never()).deleteAll(List.of(lancamentoPago));
        verify(lancamentoFinanceiroRepository).save(lancamentoPago);
        verify(pacoteSessaoRepository).delete(pacote);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ClienteEntity cliente(String nome) {
        ClienteEntity c = ClienteEntity.builder().nomeCompleto(nome).ativo(true).build();
        c.setId(UUID.randomUUID());
        return c;
    }

    private PacoteSessaoEntity pacoteAtivo(int contratadas, int consumidas) {
        return pacoteComStatus(PacoteSessaoStatus.ATIVO, contratadas, consumidas);
    }

    private PacoteSessaoEntity pacoteComStatus(PacoteSessaoStatus status, int contratadas, int consumidas) {
        PacoteSessaoEntity p = PacoteSessaoEntity.builder()
                .cliente(cliente("Paciente"))
                .nomePacote("Plano Teste")
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

    private PacoteSessaoForm formPacote(String nome, int sessoes, String valor,
                                        LocalDate inicio, LocalDate validade) {
        PacoteSessaoForm form = new PacoteSessaoForm();
        form.setNomePacote(nome);
        form.setSessoesContratadas(sessoes);
        form.setValorTotal(new BigDecimal(valor));
        form.setDataInicio(inicio);
        form.setDataValidade(validade);
        return form;
    }

    private LancamentoFinanceiroEntity lancamentoPendente(PacoteSessaoEntity pacote) {
        LancamentoFinanceiroEntity l = LancamentoFinanceiroEntity.builder()
                .status(StatusLancamentoFinanceiro.PENDENTE)
                .descricao("Plano de tratamento")
                .categoria("Planos de tratamento")
                .tipo(com.fisiogestao.financeiro.TipoLancamentoFinanceiro.RECEITA)
                .valor(BigDecimal.TEN)
                .dataCompetencia(LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime())
                .pacoteSessao(pacote)
                .build();
        l.setId(UUID.randomUUID());
        return l;
    }
}
