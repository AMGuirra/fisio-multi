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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void deveExcluirPacoteSemHistoricoRemovendoLancamentosPendentes() {
        UUID pacoteId = UUID.randomUUID();
        PacoteSessaoEntity pacote = PacoteSessaoEntity.builder()
                .cliente(ClienteEntity.builder().nomeCompleto("Maria").ativo(true).build())
                .nomePacote("Plano Mensal")
                .sessoesContratadas(10)
                .sessoesConsumidas(0)
                .valorTotal(BigDecimal.TEN)
                .dataInicio(LocalDate.now())
                .status(PacoteSessaoStatus.CANCELADO)
                .build();
        pacote.setId(pacoteId);
        LancamentoFinanceiroEntity lancamento = LancamentoFinanceiroEntity.builder()
                .status(StatusLancamentoFinanceiro.PENDENTE)
                .descricao("Plano de tratamento")
                .categoria("Planos de tratamento")
                .tipo(com.fisiogestao.financeiro.TipoLancamentoFinanceiro.RECEITA)
                .valor(BigDecimal.TEN)
                .dataCompetencia(LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime())
                .build();

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
        ClienteEntity cliente = ClienteEntity.builder().nomeCompleto("Maria").ativo(true).build();
        PacoteSessaoEntity pacote = PacoteSessaoEntity.builder()
                .cliente(cliente)
                .nomePacote("Plano Mensal")
                .sessoesContratadas(10)
                .sessoesConsumidas(0)
                .valorTotal(BigDecimal.TEN)
                .dataInicio(LocalDate.now())
                .status(PacoteSessaoStatus.ATIVO)
                .build();
        pacote.setId(pacoteId);
        SessaoEntity sessao = SessaoEntity.builder()
                .cliente(cliente)
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
        verify(sessaoRepository).save(sessao);
        verify(pacoteSessaoRepository).delete(pacote);
    }

    @Test
    void devePreservarLancamentoPagoAoExcluirPacote() {
        UUID pacoteId = UUID.randomUUID();
        PacoteSessaoEntity pacote = PacoteSessaoEntity.builder()
                .cliente(ClienteEntity.builder().nomeCompleto("Maria").ativo(true).build())
                .nomePacote("Plano Mensal")
                .sessoesContratadas(10)
                .sessoesConsumidas(3)
                .valorTotal(BigDecimal.TEN)
                .dataInicio(LocalDate.now())
                .status(PacoteSessaoStatus.ATIVO)
                .build();
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
}
