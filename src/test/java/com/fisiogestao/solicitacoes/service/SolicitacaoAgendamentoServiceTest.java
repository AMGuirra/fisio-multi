package com.fisiogestao.solicitacoes.service;

import com.fisiogestao.agenda.HorarioBloqueadoEntity;
import com.fisiogestao.agenda.HorarioBloqueadoStatus;
import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.TipoAgendamento;
import com.fisiogestao.agenda.repository.HorarioBloqueadoRepository;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.agenda.service.BloqueioAgendaService;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.email.service.AgendamentoEmailNotificationService;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.PlanoSessaoEntity;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.pacientes.service.PlanoSessaoService;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import com.fisiogestao.solicitacoes.SolicitacaoStatus;
import com.fisiogestao.solicitacoes.form.AgendamentoOnlineForm;
import com.fisiogestao.solicitacoes.form.AprovacaoSolicitacaoForm;
import com.fisiogestao.solicitacoes.repository.SolicitacaoAgendamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitacaoAgendamentoServiceTest {

    @Mock
    private SolicitacaoAgendamentoRepository solicitacaoAgendamentoRepository;

    @Mock
    private HorarioBloqueadoRepository horarioBloqueadoRepository;

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private AgendaService agendaService;

    @Mock
    private BloqueioAgendaService bloqueioAgendaService;

    @Mock
    private PacienteService pacienteService;

    @Mock
    private PlanoSessaoService planoSessaoService;

    @Mock
    private AgendamentoEmailNotificationService agendamentoEmailNotificationService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private SolicitacaoAgendamentoService solicitacaoAgendamentoService;

    @Test
    void deveCadastrarPacienteSemCriarSessaoNemReservarPacote() {
        UUID solicitacaoId = UUID.randomUUID();
        ClienteEntity cliente = ClienteEntity.builder()
                .nomeCompleto("Antonio")
                .telefone("65999990000")
                .ativo(true)
                .build();
        cliente.setId(UUID.randomUUID());

        SolicitacaoAgendamentoEntity solicitacao = solicitacaoPendente();
        solicitacao.setId(solicitacaoId);

        when(solicitacaoAgendamentoRepository.findById(solicitacaoId)).thenReturn(Optional.of(solicitacao));
        when(pacienteService.localizarOuCriar(any(), eq(com.fisiogestao.auditoria.AuditEventOrigin.MANUAL), any(), any()))
                .thenReturn(cliente);
        when(solicitacaoAgendamentoRepository.save(any(SolicitacaoAgendamentoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClienteEntity resultado = solicitacaoAgendamentoService.cadastrarPaciente(solicitacaoId, new AprovacaoSolicitacaoForm());

        assertSame(cliente, resultado);
        verify(pacienteService).validarRelacionamentoSolicitacaoClienteUuid();
        verify(solicitacaoAgendamentoRepository).save(argThat(saved -> saved.getCliente() == cliente
                && saved.getStatus() == SolicitacaoStatus.PENDENTE));
        verify(planoSessaoService, never()).resolverPlanoParaSessao(any(), any(), any());
        verify(sessaoRepository, never()).save(any());
    }

    @Test
    void deveExigirCadastroAntesDeAprovar() {
        UUID solicitacaoId = UUID.randomUUID();
        SolicitacaoAgendamentoEntity solicitacao = solicitacaoPendente();
        solicitacao.setId(solicitacaoId);

        when(solicitacaoAgendamentoRepository.findById(solicitacaoId)).thenReturn(Optional.of(solicitacao));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> solicitacaoAgendamentoService.aprovarSolicitacao(solicitacaoId, new AprovacaoSolicitacaoForm())
        );

        assertEquals("Cadastre o paciente antes de aprovar a solicitação.", ex.getMessage());
        verifyNoInteractions(horarioBloqueadoRepository, pacienteService, planoSessaoService, sessaoRepository);
    }

    @Test
    void deveCriarSessaoDePlanoAoAprovarSolicitacao() {
        UUID solicitacaoId = UUID.randomUUID();
        ClienteEntity cliente = ClienteEntity.builder()
                .nomeCompleto("Antonio")
                .telefone("65999990000")
                .ativo(true)
                .build();
        cliente.setId(UUID.randomUUID());
        PlanoSessaoEntity plano = PlanoSessaoEntity.builder()
                .cliente(cliente)
                .nomePlano("Plano Teste")
                .sessoesContratadas(10)
                .sessoesConsumidas(2)
                .build();
        plano.setId(UUID.randomUUID());
        SolicitacaoAgendamentoEntity solicitacao = solicitacaoPendente();
        solicitacao.setId(solicitacaoId);
        solicitacao.setCliente(cliente);
        HorarioBloqueadoEntity bloqueio = HorarioBloqueadoEntity.builder()
                .status(HorarioBloqueadoStatus.PENDENTE)
                .dataHoraInicio(solicitacao.getDataHoraInicio())
                .dataHoraFim(solicitacao.getDataHoraFim())
                .build();
        bloqueio.setId(UUID.randomUUID());

        when(solicitacaoAgendamentoRepository.findById(solicitacaoId)).thenReturn(Optional.of(solicitacao));
        when(horarioBloqueadoRepository.findBySolicitacaoId(solicitacaoId)).thenReturn(Optional.of(bloqueio));
        when(planoSessaoService.resolverPlanoParaSessao(cliente.getId(), null, null)).thenReturn(plano);
        when(sessaoRepository.save(any(SessaoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(solicitacaoAgendamentoRepository.save(any(SolicitacaoAgendamentoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SessaoEntity resultado = solicitacaoAgendamentoService.aprovarSolicitacao(solicitacaoId, new AprovacaoSolicitacaoForm());

        assertEquals(TipoAgendamento.PLANO, resultado.getTipoAgendamento());
        assertSame(plano, resultado.getPlanoSessao());
        verify(sessaoRepository).save(argThat(sessao -> sessao.getTipoAgendamento() == TipoAgendamento.PLANO));
    }

    @Test
    void deveRejeitarCpfInvalidoNoAgendamentoPublico() {
        AgendamentoOnlineForm form = new AgendamentoOnlineForm();
        form.setNomePaciente("Antonio");
        form.setTelefone("67999999999");
        form.setLocalAtendimento("CLINICA");
        form.setCpf("111.111.111-11");

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> solicitacaoAgendamentoService.criarSolicitacaoPublica(form)
        );

        assertEquals("Informe um CPF válido.", ex.getMessage());
    }

    @Test
    void deveRejeitarTelefoneInvalidoNoAgendamentoPublico() {
        AgendamentoOnlineForm form = new AgendamentoOnlineForm();
        form.setNomePaciente("Antonio");
        form.setTelefone("999");
        form.setLocalAtendimento("CLINICA");

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> solicitacaoAgendamentoService.criarSolicitacaoPublica(form)
        );

        assertEquals("Informe um telefone válido com DDD (10 ou 11 dígitos).", ex.getMessage());
    }

    @Test
    void deveAceitarTelefoneECpfComMascaraNoAgendamentoPublico() {
        AgendamentoOnlineForm form = new AgendamentoOnlineForm();
        form.setNomePaciente("Antonio");
        form.setTelefone("(67) 99999-9999");
        form.setCpf("123.456.789-09");
        form.setLocalAtendimento("CLINICA");
        form.setData(java.time.LocalDate.now().plusDays(1));
        form.setHorario("09:00");

        when(agendaService.montarIntervalo(any(), any())).thenReturn(
                new AgendaService.IntervaloAgenda(
                        OffsetDateTime.now().plusDays(1),
                        OffsetDateTime.now().plusDays(1).plusMinutes(50)));

        ClienteEntity cliente = ClienteEntity.builder()
                .nomeCompleto("Antonio")
                .telefone("67999999999")
                .cpf("12345678909")
                .ativo(true)
                .build();
        cliente.setId(UUID.randomUUID());
        when(pacienteService.localizarOuCriar(any(), eq(com.fisiogestao.auditoria.AuditEventOrigin.ONLINE), any(), any()))
                .thenReturn(cliente);
        when(solicitacaoAgendamentoRepository.save(any(SolicitacaoAgendamentoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SolicitacaoAgendamentoEntity resultado = solicitacaoAgendamentoService.criarSolicitacaoPublica(form);

        assertEquals("67999999999", resultado.getTelefone());
        assertEquals("12345678909", resultado.getCpf());
    }

    @Test
    void devePersistirSomenteTextoPlanoEmCamposDaSolicitacaoPublica() {
        AgendamentoOnlineForm form = new AgendamentoOnlineForm();
        form.setNomePaciente("<strong>Paciente Ficticio</strong>");
        form.setTelefone("67999999999");
        form.setLocalAtendimento("CLINICA");
        form.setData(java.time.LocalDate.now().plusDays(1));
        form.setHorario("09:00");
        form.setObservacoes("Texto seguro<script>conteudo_executavel()</script>");

        when(agendaService.montarIntervalo(any(), any())).thenReturn(
                new AgendaService.IntervaloAgenda(
                        OffsetDateTime.now().plusDays(1),
                        OffsetDateTime.now().plusDays(1).plusMinutes(50)));
        ClienteEntity pacienteFicticio = ClienteEntity.builder()
                .nomeCompleto("Paciente Ficticio")
                .telefone("67999999999")
                .ativo(true)
                .build();
        pacienteFicticio.setId(UUID.randomUUID());
        when(pacienteService.localizarOuCriar(any(), eq(com.fisiogestao.auditoria.AuditEventOrigin.ONLINE), any(), any()))
                .thenReturn(pacienteFicticio);
        when(solicitacaoAgendamentoRepository.save(any(SolicitacaoAgendamentoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SolicitacaoAgendamentoEntity resultado = solicitacaoAgendamentoService.criarSolicitacaoPublica(form);

        verify(pacienteService).localizarOuCriar(
                argThat(paciente -> "Paciente Ficticio".equals(paciente.getNomeCompleto())),
                eq(com.fisiogestao.auditoria.AuditEventOrigin.ONLINE), any(), any());
        assertEquals("Texto seguro", resultado.getObservacoes());
        assertFalse(resultado.getObservacoes().contains("<"));
    }

    private SolicitacaoAgendamentoEntity solicitacaoPendente() {
        return SolicitacaoAgendamentoEntity.builder()
                .nomePaciente("Antonio")
                .telefone("65999990000")
                .email("antonio@example.com")
                .cpf("12345678901")
                .dataHoraInicio(OffsetDateTime.now().plusDays(1))
                .dataHoraFim(OffsetDateTime.now().plusDays(1).plusMinutes(50))
                .localAtendimento("CLINICA")
                .status(SolicitacaoStatus.PENDENTE)
                .build();
    }
}
