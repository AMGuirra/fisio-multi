package com.fisiogestao.pacientes.service;

import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.financeiro.repository.LancamentoFinanceiroRepository;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.repository.ClienteRepository;
import com.fisiogestao.pacientes.repository.PacoteSessaoRepository;
import com.fisiogestao.prontuario.repository.ProntuarioSoapRepository;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.solicitacoes.repository.SolicitacaoAgendamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private PacoteSessaoRepository pacoteSessaoRepository;

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private ProntuarioSoapRepository prontuarioSoapRepository;

    @Mock
    private SolicitacaoAgendamentoRepository solicitacaoAgendamentoRepository;

    @Mock
    private LancamentoFinanceiroRepository lancamentoFinanceiroRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PacienteService pacienteService;

    @Test
    void deveExcluirPacienteSemHistoricoNemMovimentacao() {
        UUID clienteId = UUID.randomUUID();
        ClienteEntity cliente = ClienteEntity.builder()
                .nomeCompleto("Maria")
                .ativo(true)
                .build();
        cliente.setId(clienteId);

        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(pacoteSessaoRepository.existsByClienteId(clienteId)).thenReturn(false);
        when(sessaoRepository.existsByClienteId(clienteId)).thenReturn(false);
        when(prontuarioSoapRepository.existsByClienteId(clienteId)).thenReturn(false);
        when(solicitacaoAgendamentoRepository.existsByClienteId(clienteId)).thenReturn(false);
        when(lancamentoFinanceiroRepository.existsByClienteId(clienteId)).thenReturn(false);

        assertDoesNotThrow(() -> pacienteService.excluir(clienteId));

        verify(clienteRepository).delete(cliente);
    }

    @Test
    void deveImpedirExclusaoDePacienteComPacotesVinculados() {
        UUID clienteId = UUID.randomUUID();
        ClienteEntity cliente = ClienteEntity.builder()
                .nomeCompleto("Maria")
                .ativo(true)
                .build();
        cliente.setId(clienteId);

        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(pacoteSessaoRepository.existsByClienteId(clienteId)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> pacienteService.excluir(clienteId));

        assertEquals("Exclua primeiro os planos de tratamento vinculados a este paciente.", ex.getMessage());
        verify(clienteRepository, never()).delete(cliente);
    }
}
