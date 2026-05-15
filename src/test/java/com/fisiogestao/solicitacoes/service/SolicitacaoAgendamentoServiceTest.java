package com.fisiogestao.solicitacoes.service;

import com.fisiogestao.agenda.repository.HorarioBloqueadoRepository;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.agenda.service.BloqueioAgendaService;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.email.service.AgendamentoEmailNotificationService;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.pacientes.service.PlanoSessaoService;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import com.fisiogestao.solicitacoes.SolicitacaoStatus;
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
