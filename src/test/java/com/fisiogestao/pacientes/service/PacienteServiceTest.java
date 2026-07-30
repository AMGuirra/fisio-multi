package com.fisiogestao.pacientes.service;

import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.agenda.repository.HorarioBloqueadoRepository;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.financeiro.repository.LancamentoFinanceiroRepository;
import com.fisiogestao.listaespera.ListaEsperaRepository;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.repository.ClienteRepository;
import com.fisiogestao.pacientes.repository.PlanoSessaoRepository;
import com.fisiogestao.portal.PacientePasswordResetTokenRepository;
import com.fisiogestao.prontuario.repository.ProntuarioAnexoRepository;
import com.fisiogestao.prontuario.repository.ProntuarioSoapRepository;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.solicitacoes.repository.SolicitacaoAgendamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private PlanoSessaoRepository pacoteSessaoRepository;

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private ProntuarioSoapRepository prontuarioSoapRepository;

    @Mock
    private SolicitacaoAgendamentoRepository solicitacaoAgendamentoRepository;

    @Mock
    private LancamentoFinanceiroRepository lancamentoFinanceiroRepository;

    @Mock
    private ProntuarioAnexoRepository prontuarioAnexoRepository;

    @Mock
    private ListaEsperaRepository listaEsperaRepository;

    @Mock
    private HorarioBloqueadoRepository horarioBloqueadoRepository;

    @Mock
    private PacientePasswordResetTokenRepository pacientePasswordResetTokenRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private ConfiguracaoClinicaRepository configuracaoClinicaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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
        when(solicitacaoAgendamentoRepository.findByClienteIdOrderByCreatedAtDesc(clienteId))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> pacienteService.excluir(clienteId));

        verify(clienteRepository).delete(cliente);
    }

    @Test
    void deveExcluirEmCascataTodosOsVinculosDoPaciente() {
        UUID clienteId = UUID.randomUUID();
        ClienteEntity cliente = ClienteEntity.builder()
                .nomeCompleto("Maria")
                .ativo(true)
                .build();
        cliente.setId(clienteId);

        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(solicitacaoAgendamentoRepository.findByClienteIdOrderByCreatedAtDesc(clienteId))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> pacienteService.excluir(clienteId));

        verify(prontuarioAnexoRepository).deleteByClienteId(clienteId);
        verify(prontuarioSoapRepository).deleteByClienteId(clienteId);
        verify(lancamentoFinanceiroRepository).deleteByClienteId(clienteId);
        verify(sessaoRepository).deleteByClienteId(clienteId);
        verify(pacoteSessaoRepository).limparPlanoAnteriorPorCliente(clienteId);
        verify(pacoteSessaoRepository).deleteByClienteId(clienteId);
        verify(solicitacaoAgendamentoRepository).deleteByClienteId(clienteId);
        verify(listaEsperaRepository).deleteByClienteId(clienteId);
        verify(pacientePasswordResetTokenRepository).deleteByClienteId(clienteId);
        verify(clienteRepository).delete(cliente);
    }

    @Test
    void deveValidarRelacionamentoSolicitacaoClienteQuandoSchemaEstiverCorreto() {
        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(String.class), anyString(), anyString()))
                .thenReturn("uuid", "uuid");

        assertDoesNotThrow(() -> pacienteService.validarRelacionamentoSolicitacaoClienteUuid());
    }

    @Test
    void deveFalharQuandoRelacionamentoSolicitacaoClienteNaoForUuid() {
        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(String.class), anyString(), anyString()))
                .thenReturn("uuid", "bigint");

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> pacienteService.validarRelacionamentoSolicitacaoClienteUuid()
        );

        assertEquals(
                "Banco incompatível: a coluna solicitacao_agendamento.cliente_id está como bigint. "
                        + "Ajuste o schema para UUID (script sugerido em docs/sql/correcao_cliente_id_uuid.sql).",
                ex.getMessage()
        );
    }

    private ClienteEntity pacienteComSenha(String cpf, String telefone, String senhaHash) {
        ClienteEntity cliente = ClienteEntity.builder()
                .nomeCompleto("Paciente Teste")
                .cpf(cpf)
                .telefone(telefone)
                .senhaPortal(senhaHash)
                .ativo(true)
                .build();
        cliente.setId(UUID.randomUUID());
        return cliente;
    }

    @Test
    void deveAutenticarPorCpfComMascaraQuandoSenhaConfere() {
        ClienteEntity cliente = pacienteComSenha("12345678909", null, "hash-valido");
        when(clienteRepository.findFirstByCpf("12345678909")).thenReturn(Optional.of(cliente));
        when(passwordEncoder.matches("Senha123", "hash-valido")).thenReturn(true);

        PacienteService.ResultadoAutenticacaoPortal resultado =
                pacienteService.autenticarPortalPorTipo("cpf", "123.456.789-09", "Senha123");

        assertEquals(PacienteService.StatusAutenticacaoPortal.SUCESSO, resultado.status());
        assertEquals(cliente, resultado.paciente());
    }

    @Test
    void deveAutenticarPorCelularComMascaraQuandoSenhaConfere() {
        ClienteEntity cliente = pacienteComSenha(null, "67999999999", "hash-valido");
        when(clienteRepository.findAllByTelefone("67999999999")).thenReturn(List.of(cliente));
        when(passwordEncoder.matches("Senha123", "hash-valido")).thenReturn(true);

        PacienteService.ResultadoAutenticacaoPortal resultado =
                pacienteService.autenticarPortalPorTipo("celular", "(67) 99999-9999", "Senha123");

        assertEquals(PacienteService.StatusAutenticacaoPortal.SUCESSO, resultado.status());
        assertEquals(cliente, resultado.paciente());
    }

    @Test
    void deveRetornarInvalidoQuandoSenhaEstaErrada() {
        ClienteEntity cliente = pacienteComSenha("12345678909", null, "hash-valido");
        when(clienteRepository.findFirstByCpf("12345678909")).thenReturn(Optional.of(cliente));
        when(passwordEncoder.matches(eq("senha-errada"), anyString())).thenReturn(false);

        PacienteService.ResultadoAutenticacaoPortal resultado =
                pacienteService.autenticarPortalPorTipo("cpf", "12345678909", "senha-errada");

        assertEquals(PacienteService.StatusAutenticacaoPortal.INVALIDO, resultado.status());
        assertNull(resultado.paciente());
    }

    @Test
    void deveRetornarInvalidoQuandoIdentificadorNaoExiste() {
        when(clienteRepository.findFirstByCpf("12345678909")).thenReturn(Optional.empty());

        PacienteService.ResultadoAutenticacaoPortal resultado =
                pacienteService.autenticarPortalPorTipo("cpf", "12345678909", "qualquer-coisa");

        assertEquals(PacienteService.StatusAutenticacaoPortal.INVALIDO, resultado.status());
    }

    @Test
    void deveRetornarCelularAmbiguoQuandoMaisDeUmPacienteTemMesmoCelular() {
        ClienteEntity p1 = pacienteComSenha("11111111111", "67999999999", "hash-1");
        ClienteEntity p2 = pacienteComSenha("22222222222", "67999999999", "hash-2");
        when(clienteRepository.findAllByTelefone("67999999999")).thenReturn(List.of(p1, p2));

        PacienteService.ResultadoAutenticacaoPortal resultado =
                pacienteService.autenticarPortalPorTipo("celular", "67999999999", "qualquer-coisa");

        assertEquals(PacienteService.StatusAutenticacaoPortal.CELULAR_AMBIGUO, resultado.status());
        assertNull(resultado.paciente());
    }

    @Test
    void deveAutenticarPorEmailPorCompatibilidadeComContasAntigas() {
        ClienteEntity cliente = ClienteEntity.builder()
                .nomeCompleto("Paciente Antigo")
                .email("paciente@example.com")
                .senhaPortal("hash-valido")
                .ativo(true)
                .build();
        cliente.setId(UUID.randomUUID());
        when(clienteRepository.findFirstByEmailIgnoreCase("paciente@example.com")).thenReturn(Optional.of(cliente));
        when(passwordEncoder.matches("Senha123", "hash-valido")).thenReturn(true);

        PacienteService.ResultadoAutenticacaoPortal resultado =
                pacienteService.autenticarPortalPorTipo("cpf", "paciente@example.com", "Senha123");

        assertEquals(PacienteService.StatusAutenticacaoPortal.SUCESSO, resultado.status());
    }

    @Test
    void naoDeveAutenticarPacienteSemSenhaDefinida() {
        ClienteEntity cliente = pacienteComSenha("12345678909", null, null);
        when(clienteRepository.findFirstByCpf("12345678909")).thenReturn(Optional.of(cliente));

        PacienteService.ResultadoAutenticacaoPortal resultado =
                pacienteService.autenticarPortalPorTipo("cpf", "12345678909", "qualquer-coisa");

        assertEquals(PacienteService.StatusAutenticacaoPortal.INVALIDO, resultado.status());
    }
}
