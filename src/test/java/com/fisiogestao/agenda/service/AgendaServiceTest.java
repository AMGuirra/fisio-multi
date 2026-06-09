package com.fisiogestao.agenda.service;

import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.BloqueioAgendaEntity;
import com.fisiogestao.agenda.HorarioBloqueadoEntity;
import com.fisiogestao.agenda.TipoAgendamento;
import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.form.AgendaManualForm;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.agenda.repository.HorarioBloqueadoRepository;
import com.fisiogestao.agenda.repository.RecorrenciaRepository;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.email.service.AgendamentoEmailNotificationService;
import com.fisiogestao.financeiro.service.FinanceiroService;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.PlanoSessaoEntity;
import com.fisiogestao.pacientes.PlanoSessaoStatus;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.pacientes.service.PlanoSessaoService;
import com.fisiogestao.shared.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendaServiceTest {

    @Mock
    private ConfiguracaoClinicaRepository configuracaoClinicaRepository;

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private HorarioBloqueadoRepository horarioBloqueadoRepository;

    @Mock
    private RecorrenciaRepository recorrenciaRepository;

    @Mock
    private PacienteService pacienteService;

    @Mock
    private PlanoSessaoService planoSessaoService;

    @Mock
    private AgendamentoEmailNotificationService agendamentoEmailNotificationService;

    @Mock
    private BloqueioAgendaService bloqueioAgendaService;

    @Mock
    private RecorrenciaService recorrenciaService;

    @Mock
    private FinanceiroService financeiroService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AgendaService agendaService;

    @Test
    void devePermitirCancelarSessaoRealizadaEstornandoPlano() {
        PlanoSessaoEntity plano = criarPlano();
        SessaoEntity sessao = criarSessao(SessaoStatus.REALIZADA, plano, true, OffsetDateTime.now().minusDays(1));

        when(sessaoRepository.findById(sessao.getId())).thenReturn(Optional.of(sessao));
        when(sessaoRepository.save(any(SessaoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        agendaService.cancelarSessao(sessao.getId(), "Cancelamento administrativo");

        assertEquals(SessaoStatus.CANCELADA, sessao.getStatus());
        assertEquals(Boolean.FALSE, sessao.getPlanoConsumoRegistrado());
        verify(planoSessaoService).estornarSessao(plano.getId());
    }

    @Test
    void devePermitirMarcarSessaoCanceladaComoRealizadaReservandoPlanoSemNotificarCancelamento() {
        PlanoSessaoEntity plano = criarPlano();
        SessaoEntity sessao = criarSessao(SessaoStatus.CANCELADA, plano, false, OffsetDateTime.now().minusDays(1));

        when(sessaoRepository.findById(sessao.getId())).thenReturn(Optional.of(sessao));
        when(sessaoRepository.save(any(SessaoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        agendaService.marcarComoRealizada(sessao.getId(), "Atendimento confirmado");

        assertEquals(SessaoStatus.REALIZADA, sessao.getStatus());
        assertEquals(Boolean.TRUE, sessao.getPlanoConsumoRegistrado());
        verify(planoSessaoService).reservarSessao(plano.getId());
        verify(agendamentoEmailNotificationService, never()).notificarCanceladoPorSessao(any(), any(), any());
    }

    @Test
    void faltaNaoFaturadaNaoConsomePlano() {
        PlanoSessaoEntity plano = criarPlano();
        SessaoEntity sessao = criarSessao(SessaoStatus.AGENDADA, plano, false, OffsetDateTime.now().minusDays(1));

        when(sessaoRepository.findById(sessao.getId())).thenReturn(Optional.of(sessao));
        when(sessaoRepository.save(any(SessaoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());
        when(auditService.describeOf(any())).thenReturn("Sessão Teste");

        agendaService.registrarFaltaNaoFaturada(sessao.getId(), "Administrador");

        assertEquals(SessaoStatus.FALTA, sessao.getStatus());
        assertEquals(Boolean.FALSE, sessao.getPlanoConsumoRegistrado());
        verify(planoSessaoService, never()).reservarSessao(plano.getId());
    }

    @Test
    void faltaFaturadaConsomePlano() {
        PlanoSessaoEntity plano = criarPlano();
        SessaoEntity sessao = criarSessao(SessaoStatus.AGENDADA, plano, false, OffsetDateTime.now().minusDays(1));

        when(sessaoRepository.findById(sessao.getId())).thenReturn(Optional.of(sessao));
        when(sessaoRepository.save(any(SessaoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());
        when(auditService.describeOf(any())).thenReturn("Sessão Teste");

        agendaService.registrarFaltaFaturada(sessao.getId(), "Administrador");

        assertEquals(SessaoStatus.FALTA_FATURADA, sessao.getStatus());
        assertEquals(Boolean.TRUE, sessao.getPlanoConsumoRegistrado());
        verify(planoSessaoService).reservarSessao(plano.getId());
    }

    @Test
    void devePermitirReabrirSessaoCanceladaComoPendenteSemReservarPlano() {
        PlanoSessaoEntity plano = criarPlano();
        SessaoEntity sessao = criarSessao(SessaoStatus.CANCELADA, plano, false, OffsetDateTime.now().minusDays(1));

        when(sessaoRepository.findById(sessao.getId())).thenReturn(Optional.of(sessao));
        when(sessaoRepository.save(any(SessaoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());
        when(auditService.describeOf(any())).thenReturn("Sessão Teste");

        agendaService.reabrirSessaoCancelada(sessao.getId());

        assertEquals(SessaoStatus.AGENDADA, sessao.getStatus());
        assertEquals(Boolean.FALSE, sessao.getPlanoConsumoRegistrado());
        verify(planoSessaoService, never()).reservarSessao(plano.getId());
    }

    @Test
    void deveExcluirSessaoEstornandoPlanoQuandoNecessario() {
        PlanoSessaoEntity plano = criarPlano();
        SessaoEntity sessao = criarSessao(SessaoStatus.REALIZADA, plano, true, OffsetDateTime.now().minusDays(1));

        when(sessaoRepository.findById(sessao.getId())).thenReturn(Optional.of(sessao));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());
        when(auditService.describeOf(any())).thenReturn("Sessão Teste");
        doNothing().when(sessaoRepository).delete(sessao);
        doNothing().when(sessaoRepository).flush();

        agendaService.excluirSessao(sessao.getId());

        verify(planoSessaoService).estornarSessao(plano.getId());
        verify(sessaoRepository).delete(sessao);
        verify(sessaoRepository).flush();
    }

    @Test
    void devePermitirEditarSessaoRealizadaMesmoEmHorarioPassado() {
        ClienteEntity cliente = criarCliente();
        PlanoSessaoEntity plano = criarPlano();
        OffsetDateTime inicioPassado = LocalDate.now()
                .minusDays(2)
                .atTime(9, 0)
                .atZone(ZoneId.of("America/Campo_Grande"))
                .toOffsetDateTime();
        SessaoEntity sessao = criarSessao(SessaoStatus.REALIZADA, plano, true, inicioPassado);
        AgendaManualForm form = new AgendaManualForm();
        form.setClienteId(cliente.getId());
        form.setPlanoSessaoId(plano.getId());
        form.setData(inicioPassado.toLocalDate());
        form.setHorario(inicioPassado.toLocalTime());
        form.setTipoAtendimento("Pilates Solo");
        form.setObservacoes("Sessão ajustada no histórico");

        when(sessaoRepository.findById(sessao.getId())).thenReturn(Optional.of(sessao));
        when(sessaoRepository.save(any(SessaoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc()).thenReturn(Optional.of(criarConfiguracao()));
        when(sessaoRepository.findByStatusInAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(any(), any(), any())).thenReturn(List.of());
        when(horarioBloqueadoRepository.findByStatusInAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(any(), any(), any()))
                .thenReturn(List.<HorarioBloqueadoEntity>of());
        when(bloqueioAgendaService.existeBloqueioAtivo(any(), any(), any())).thenReturn(false);
        when(pacienteService.buscarPorId(cliente.getId())).thenReturn(cliente);
        when(planoSessaoService.resolverPlanoParaSessao(cliente.getId(), plano.getId(), plano.getId())).thenReturn(plano);
        when(auditService.snapshotOf(any())).thenReturn(Map.of());
        when(auditService.describeOf(any())).thenReturn("Sessão Teste");

        assertDoesNotThrow(() -> agendaService.atualizarSessaoManual(sessao.getId(), form, "Administrador"));

        assertEquals("Pilates Solo", sessao.getTipoAtendimento());
        assertEquals("Sessão ajustada no histórico", sessao.getObservacoes());
        assertEquals(SessaoStatus.REALIZADA, sessao.getStatus());
    }

    @Test
    void deveCriarAgendamentoAvulsoSemReservarPlano() {
        ClienteEntity cliente = criarCliente();
        AgendaManualForm form = new AgendaManualForm();
        form.setClienteId(cliente.getId());
        form.setData(LocalDate.now().minusDays(1));
        form.setHorario(LocalTime.of(10, 0));
        form.setTipoAgendamento(TipoAgendamento.AVULSO);
        form.setValorAvulso(new BigDecimal("180.00"));
        form.setTipoAtendimento("Avaliação");

        when(configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc()).thenReturn(Optional.of(criarConfiguracao()));
        when(sessaoRepository.findByStatusInAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(any(), any(), any())).thenReturn(List.of());
        when(horarioBloqueadoRepository.findByStatusInAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(any(), any(), any()))
                .thenReturn(List.<HorarioBloqueadoEntity>of());
        when(bloqueioAgendaService.existeBloqueioAtivo(any(), any(), any())).thenReturn(false);
        when(pacienteService.buscarPorId(cliente.getId())).thenReturn(cliente);
        when(sessaoRepository.save(any(SessaoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SessaoEntity sessao = agendaService.agendarSessaoManual(form, "Administrador");

        assertEquals(TipoAgendamento.AVULSO, sessao.getTipoAgendamento());
        assertEquals(new BigDecimal("180.00"), sessao.getValorAvulso());
        assertEquals(null, sessao.getPlanoSessao());
        assertEquals(Boolean.FALSE, sessao.getPlanoConsumoRegistrado());
        verify(planoSessaoService, never()).reservarSessao(any());
    }

    @Test
    void deveValidarConflitoDeHorarioAntesDeAgendar() {
        ClienteEntity cliente = criarCliente();
        OffsetDateTime inicio = LocalDate.now().minusDays(1)
                .atTime(10, 0)
                .atZone(ZoneId.of("America/Campo_Grande"))
                .toOffsetDateTime();
        SessaoEntity conflito = criarSessao(SessaoStatus.AGENDADA, criarPlano(), true, inicio);
        AgendaManualForm form = criarFormAvulso(cliente, inicio.toLocalDate(), inicio.toLocalTime());

        when(configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc()).thenReturn(Optional.of(criarConfiguracao()));
        when(sessaoRepository.findByStatusInAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(any(), any(), any()))
                .thenReturn(List.of(conflito));

        assertThrows(BusinessException.class, () -> agendaService.agendarSessaoManual(form, "Administrador"));
    }

    @Test
    void deveValidarBloqueioAntesDeAgendar() {
        ClienteEntity cliente = criarCliente();
        AgendaManualForm form = criarFormAvulso(cliente, LocalDate.now().minusDays(1), LocalTime.of(10, 0));

        when(configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc()).thenReturn(Optional.of(criarConfiguracao()));
        when(sessaoRepository.findByStatusInAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(any(), any(), any())).thenReturn(List.of());
        when(horarioBloqueadoRepository.findByStatusInAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(any(), any(), any()))
                .thenReturn(List.<HorarioBloqueadoEntity>of());
        when(bloqueioAgendaService.existeBloqueioAtivo(any(), any(), any())).thenReturn(true);

        assertThrows(BusinessException.class, () -> agendaService.agendarSessaoManual(form, "Administrador"));
    }

    @Test
    void deveRemoverSlotsQuandoDataEstiverBloqueadaDiaInteiro() {
        LocalDate data = LocalDate.now().plusDays(3);
        ZoneId zoneId = ZoneId.of("America/Campo_Grande");
        BloqueioAgendaEntity bloqueio = BloqueioAgendaEntity.builder()
                .dataHoraInicio(data.atStartOfDay(zoneId).toOffsetDateTime())
                .dataHoraFim(data.atTime(LocalTime.MAX).atZone(zoneId).toOffsetDateTime())
                .diaInteiro(true)
                .motivo("Feriado")
                .ativo(true)
                .build();

        when(configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc()).thenReturn(Optional.of(criarConfiguracao()));
        when(sessaoRepository.findByDataHoraInicioBetweenOrderByDataHoraInicioAsc(any(), any())).thenReturn(List.of());
        when(horarioBloqueadoRepository.findByStatusInAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(any(), any(), any()))
                .thenReturn(List.<HorarioBloqueadoEntity>of());
        when(bloqueioAgendaService.listarAtivosDoDia(data)).thenReturn(List.of(bloqueio));

        assertEquals(List.of(), agendaService.gerarSlotsDisponiveis(data));
    }

    @Test
    void deveValidarHorarioDaClinicaAntesDeAgendar() {
        ClienteEntity cliente = criarCliente();
        AgendaManualForm form = criarFormAvulso(cliente, LocalDate.now().minusDays(1), LocalTime.of(7, 0));

        when(configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc()).thenReturn(Optional.of(criarConfiguracao()));

        assertThrows(BusinessException.class, () -> agendaService.agendarSessaoManual(form, "Administrador"));
    }

    private ConfiguracaoClinicaEntity criarConfiguracao() {
        return ConfiguracaoClinicaEntity.builder()
                .nomeClinica("Clínica Teste")
                .diasFuncionamento("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY")
                .horarioAbertura(LocalTime.of(8, 0))
                .horarioFechamento(LocalTime.of(18, 0))
                .duracaoSessaoMinutos(50)
                .timezoneId("America/Campo_Grande")
                .ativo(true)
                .build();
    }

    private ClienteEntity criarCliente() {
        ClienteEntity cliente = ClienteEntity.builder()
                .nomeCompleto("Paciente Teste")
                .telefone("65999990000")
                .email("paciente@example.com")
                .ativo(true)
                .build();
        cliente.setId(UUID.randomUUID());
        return cliente;
    }

    private PlanoSessaoEntity criarPlano() {
        PlanoSessaoEntity plano = PlanoSessaoEntity.builder()
                .cliente(criarCliente())
                .nomePlano("Plano Teste")
                .sessoesContratadas(10)
                .sessoesConsumidas(1)
                .valorTotal(new BigDecimal("1200.00"))
                .dataInicio(LocalDate.now().minusDays(10))
                .dataValidade(LocalDate.now().plusDays(20))
                .status(PlanoSessaoStatus.ATIVO)
                .build();
        plano.setId(UUID.randomUUID());
        return plano;
    }

    private SessaoEntity criarSessao(SessaoStatus status, PlanoSessaoEntity plano, boolean consumoRegistrado, OffsetDateTime inicio) {
        SessaoEntity sessao = SessaoEntity.builder()
                .cliente(plano.getCliente())
                .planoSessao(plano)
                .dataHoraInicio(inicio)
                .dataHoraFim(inicio.plusMinutes(50))
                .status(status)
                .tipoAtendimento("Pilates")
                .observacoes("Sessão teste")
                .planoConsumoRegistrado(consumoRegistrado)
                .build();
        sessao.setId(UUID.randomUUID());
        return sessao;
    }

    private AgendaManualForm criarFormAvulso(ClienteEntity cliente, LocalDate data, LocalTime horario) {
        AgendaManualForm form = new AgendaManualForm();
        form.setClienteId(cliente.getId());
        form.setData(data);
        form.setHorario(horario);
        form.setTipoAgendamento(TipoAgendamento.AVULSO);
        form.setValorAvulso(new BigDecimal("180.00"));
        return form;
    }
}
