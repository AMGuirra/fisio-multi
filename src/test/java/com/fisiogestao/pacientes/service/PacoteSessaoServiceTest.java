package com.fisiogestao.pacientes.service;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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
        when(sessaoRepository.existsByPacoteSessaoId(pacoteId)).thenReturn(false);
        when(lancamentoFinanceiroRepository.existsByPacoteSessaoIdAndStatus(pacoteId, StatusLancamentoFinanceiro.PAGO)).thenReturn(false);
        when(lancamentoFinanceiroRepository.findByPacoteSessaoId(pacoteId)).thenReturn(List.of(lancamento));

        assertDoesNotThrow(() -> pacoteSessaoService.excluir(pacoteId));

        verify(lancamentoFinanceiroRepository).deleteAll(List.of(lancamento));
        verify(pacoteSessaoRepository).delete(pacote);
    }

    @Test
    void deveImpedirExclusaoDePacoteComHistoricoDeSessoes() {
        UUID pacoteId = UUID.randomUUID();
        PacoteSessaoEntity pacote = PacoteSessaoEntity.builder()
                .cliente(ClienteEntity.builder().nomeCompleto("Maria").ativo(true).build())
                .nomePacote("Plano Mensal")
                .sessoesContratadas(10)
                .sessoesConsumidas(0)
                .valorTotal(BigDecimal.TEN)
                .dataInicio(LocalDate.now())
                .status(PacoteSessaoStatus.ATIVO)
                .build();
        pacote.setId(pacoteId);

        when(pacoteSessaoRepository.findById(pacoteId)).thenReturn(Optional.of(pacote));
        when(sessaoRepository.existsByPacoteSessaoId(pacoteId)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> pacoteSessaoService.excluir(pacoteId));

        assertEquals("Este plano de tratamento possui histórico de sessões vinculado e não pode ser excluído.", ex.getMessage());
        verify(pacoteSessaoRepository, never()).delete(pacote);
    }
}
