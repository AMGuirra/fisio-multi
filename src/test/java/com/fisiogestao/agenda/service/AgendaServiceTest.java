package com.fisiogestao.agenda.service;

import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.HorarioBloqueadoEntity;
import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.form.AgendaManualForm;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.agenda.repository.HorarioBloqueadoRepository;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.email.service.AgendamentoEmailNotificationService;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.PacoteSessaoEntity;
import com.fisiogestao.pacientes.PacoteSessaoStatus;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.pacientes.service.PacoteSessaoService;
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
import static org.mockito.ArgumentMatchers.any;
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
    private PacienteService pacienteService;

    @Mock
    private PacoteSessaoService pacoteSessaoService;

    @Mock
    private AgendamentoEmailNotificationService agendamentoEmailNotificationService;

    @Mock
    private BloqueioAgendaService bloqueioAgendaService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AgendaService agendaService;

    @Test
    void devePermitirCancelarSessaoRealizadaEstornandoPacote() {
        PacoteSessaoEntity pacote = criarPacote();
        SessaoEntity sessao = criarSessao(SessaoStatus.REALIZADA, pacote, true, OffsetDateTime.now().minusDays(1));

        when(sessaoRepository.findById(sessao.getId())).thenReturn(Optional.of(sessao));
        when(sessaoRepository.save(any(SessaoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        agendaService.cancelarSessao(sessao.getId(), "Cancelamento administrativo");

        assertEquals(SessaoStatus.CANCELADA, sessao.getStatus());
        assertEquals(Boolean.FALSE, sessao.getPacoteConsumoRegistrado());
        verify(pacoteSessaoService).estornarSessao(pacote.getId());
    }

    @Test
    void devePermitirMarcarSessaoCanceladaComoRealizadaReservandoPacote() {
        PacoteSessaoEntity pacote = criarPacote();
        SessaoEntity sessao = criarSessao(SessaoStatus.CANCELADA, pacote, false, OffsetDateTime.now().minusDays(1));

        when(sessaoRepository.findById(sessao.getId())).thenReturn(Optional.of(sessao));
        when(sessaoRepository.save(any(SessaoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of());

        agendaService.marcarComoRealizada(sessao.getId(), "Atendimento confirmado");

        assertEquals(SessaoStatus.REALIZADA, sessao.getStatus());
        assertEquals(Boolean.TRUE, sessao.getPacoteConsumoRegistrado());
        verify(pacoteSessaoService).reservarSessao(pacote.getId());
    }

    @Test
    void devePermitirEditarSessaoRealizadaMesmoEmHorarioPassado() {
        ClienteEntity cliente = criarCliente();
        PacoteSessaoEntity pacote = criarPacote();
        OffsetDateTime inicioPassado = LocalDate.now()
                .minusDays(2)
                .atTime(9, 0)
                .atZone(ZoneId.of("America/Campo_Grande"))
                .toOffsetDateTime();
        SessaoEntity sessao = criarSessao(SessaoStatus.REALIZADA, pacote, true, inicioPassado);
        AgendaManualForm form = new AgendaManualForm();
        form.setClienteId(cliente.getId());
        form.setPacoteSessaoId(pacote.getId());
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
        when(pacoteSessaoService.resolverPacoteParaSessao(cliente.getId(), pacote.getId(), pacote.getId())).thenReturn(pacote);
        when(auditService.snapshotOf(any())).thenReturn(Map.of());
        when(auditService.describeOf(any())).thenReturn("Sessão Teste");

        assertDoesNotThrow(() -> agendaService.atualizarSessaoManual(sessao.getId(), form, "Administrador"));

        assertEquals("Pilates Solo", sessao.getTipoAtendimento());
        assertEquals("Sessão ajustada no histórico", sessao.getObservacoes());
        assertEquals(SessaoStatus.REALIZADA, sessao.getStatus());
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

    private PacoteSessaoEntity criarPacote() {
        PacoteSessaoEntity pacote = PacoteSessaoEntity.builder()
                .cliente(criarCliente())
                .nomePacote("Plano Teste")
                .sessoesContratadas(10)
                .sessoesConsumidas(1)
                .valorTotal(new BigDecimal("1200.00"))
                .dataInicio(LocalDate.now().minusDays(10))
                .dataValidade(LocalDate.now().plusDays(20))
                .status(PacoteSessaoStatus.ATIVO)
                .build();
        pacote.setId(UUID.randomUUID());
        return pacote;
    }

    private SessaoEntity criarSessao(SessaoStatus status, PacoteSessaoEntity pacote, boolean consumoRegistrado, OffsetDateTime inicio) {
        SessaoEntity sessao = SessaoEntity.builder()
                .cliente(pacote.getCliente())
                .pacoteSessao(pacote)
                .dataHoraInicio(inicio)
                .dataHoraFim(inicio.plusMinutes(50))
                .status(status)
                .tipoAtendimento("Pilates")
                .observacoes("Sessão teste")
                .pacoteConsumoRegistrado(consumoRegistrado)
                .build();
        sessao.setId(UUID.randomUUID());
        return sessao;
    }
}
