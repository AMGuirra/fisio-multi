package com.fisiogestao.prontuario.service;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.prontuario.ProntuarioSoapEntity;
import com.fisiogestao.prontuario.TipoRegistroProntuario;
import com.fisiogestao.prontuario.form.AvaliacaoForm;
import com.fisiogestao.prontuario.form.EvolucaoForm;
import com.fisiogestao.prontuario.repository.ProntuarioSoapRepository;
import com.fisiogestao.shared.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProntuarioServiceTest {

    @Mock
    private ProntuarioSoapRepository prontuarioSoapRepository;

    @Mock
    private PacienteService pacienteService;

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private ProntuarioAnexoService prontuarioAnexoService;

    @InjectMocks
    private ProntuarioService service;

    @Test
    void deveCriarAvaliacaoQuandoNaoExisteRegistroAnterior() {
        UUID clienteId = UUID.randomUUID();
        ClienteEntity cliente = clienteComId(clienteId);

        when(pacienteService.buscarPorId(clienteId)).thenReturn(cliente);
        when(prontuarioSoapRepository.findFirstByClienteIdAndTipoRegistro(clienteId, TipoRegistroProntuario.AVALIACAO))
                .thenReturn(Optional.empty());
        when(prontuarioSoapRepository.save(any(ProntuarioSoapEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AvaliacaoForm form = new AvaliacaoForm();
        form.setClienteId(clienteId);
        form.setTexto("  Queixa principal: dor lombar.  ");

        ProntuarioSoapEntity salvo = service.salvarAvaliacao(form, "fisio1");

        assertEquals(TipoRegistroProntuario.AVALIACAO, salvo.getTipoRegistro());
        assertEquals("Queixa principal: dor lombar.", salvo.getTextoRegistro());
        assertEquals("fisio1", salvo.getCriadoPor());
        verify(auditService).recordCreate(any(), any(), any(), any(), any(), any());
        verify(auditService, never()).recordUpdate(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void deveAtualizarAvaliacaoExistenteComAuditoriaDeAntesDepois() {
        UUID clienteId = UUID.randomUUID();
        ClienteEntity cliente = clienteComId(clienteId);

        ProntuarioSoapEntity existente = ProntuarioSoapEntity.builder()
                .cliente(cliente)
                .tipoRegistro(TipoRegistroProntuario.AVALIACAO)
                .textoRegistro("Texto antigo")
                .criadoPor("fisio1")
                .build();
        existente.setId(UUID.randomUUID());

        when(pacienteService.buscarPorId(clienteId)).thenReturn(cliente);
        when(prontuarioSoapRepository.findFirstByClienteIdAndTipoRegistro(clienteId, TipoRegistroProntuario.AVALIACAO))
                .thenReturn(Optional.of(existente));
        when(prontuarioSoapRepository.save(any(ProntuarioSoapEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of("textoRegistro", "Texto antigo"));

        AvaliacaoForm form = new AvaliacaoForm();
        form.setClienteId(clienteId);
        form.setTexto("Texto atualizado");

        ProntuarioSoapEntity salvo = service.salvarAvaliacao(form, "fisio2");

        assertEquals("Texto atualizado", salvo.getTextoRegistro());
        verify(auditService, never()).recordCreate(any(), any(), any(), any(), any(), any());
        verify(auditService).recordUpdate(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void deveRejeitarAvaliacaoVazia() {
        UUID clienteId = UUID.randomUUID();
        AvaliacaoForm form = new AvaliacaoForm();
        form.setClienteId(clienteId);
        form.setTexto("   ");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.salvarAvaliacao(form, "fisio1"));

        assertEquals("A avaliação não pode ficar vazia.", ex.getMessage());
        verify(prontuarioSoapRepository, never()).save(any());
    }

    @Test
    void deveRegistrarEvolucaoComTexto() {
        UUID clienteId = UUID.randomUUID();
        ClienteEntity cliente = clienteComId(clienteId);

        when(pacienteService.buscarPorId(clienteId)).thenReturn(cliente);
        when(prontuarioSoapRepository.save(any(ProntuarioSoapEntity.class)))
                .thenAnswer(invocation -> {
                    ProntuarioSoapEntity entidade = invocation.getArgument(0);
                    entidade.setId(UUID.randomUUID());
                    return entidade;
                });

        EvolucaoForm form = new EvolucaoForm();
        form.setClienteId(clienteId);
        form.setTexto("Paciente evoluiu bem, sem dor.");

        ProntuarioSoapEntity salvo = service.registrarEvolucao(form, "fisio1");

        assertEquals(TipoRegistroProntuario.EVOLUCAO, salvo.getTipoRegistro());
        assertEquals("Paciente evoluiu bem, sem dor.", salvo.getTextoRegistro());
        verify(prontuarioAnexoService, never()).salvarFotosClinicas(any(), any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void deveRejeitarEvolucaoVaziaSemFotos() {
        UUID clienteId = UUID.randomUUID();
        EvolucaoForm form = new EvolucaoForm();
        form.setClienteId(clienteId);
        form.setTexto("   ");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.registrarEvolucao(form, "fisio1"));

        assertEquals("Escreva a evolução ou anexe ao menos uma foto do acompanhamento.", ex.getMessage());
        verify(prontuarioSoapRepository, never()).save(any());
    }

    @Test
    void deveRejeitarSessaoDeOutroPaciente() {
        UUID clienteId = UUID.randomUUID();
        UUID sessaoId = UUID.randomUUID();
        ClienteEntity cliente = clienteComId(clienteId);
        ClienteEntity outroCliente = clienteComId(UUID.randomUUID());

        SessaoEntity sessao = SessaoEntity.builder().cliente(outroCliente).build();
        sessao.setId(sessaoId);

        lenient().when(pacienteService.buscarPorId(clienteId)).thenReturn(cliente);
        when(sessaoRepository.findById(sessaoId)).thenReturn(Optional.of(sessao));

        EvolucaoForm form = new EvolucaoForm();
        form.setClienteId(clienteId);
        form.setSessaoId(sessaoId);
        form.setTexto("Evolução qualquer.");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.registrarEvolucao(form, "fisio1"));

        assertEquals("A sessão informada não pertence ao paciente selecionado.", ex.getMessage());
        verify(prontuarioSoapRepository, never()).save(any());
    }

    @Test
    void deveRejeitarFotosSemConsentimentoNaEvolucao() {
        UUID clienteId = UUID.randomUUID();
        ClienteEntity cliente = clienteComId(clienteId);
        lenient().when(pacienteService.buscarPorId(clienteId)).thenReturn(cliente);

        EvolucaoForm form = new EvolucaoForm();
        form.setClienteId(clienteId);
        form.setTexto("Evolução com foto.");
        form.setFotos(List.of(new MockMultipartFile("fotos", "f.jpg", "image/jpeg", new byte[] {1})));
        form.setConsentimentoFotos(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.registrarEvolucao(form, "fisio1"));

        assertEquals(
                "Confirme que o paciente autorizou o registro fotográfico antes de enviar as fotos.",
                ex.getMessage());
        verify(prontuarioSoapRepository, never()).save(any());
        verify(prontuarioAnexoService, never()).salvarFotosClinicas(any(), any(), any(), any(), any(), any(), anyBoolean());
    }

    private ClienteEntity clienteComId(UUID id) {
        ClienteEntity cliente = ClienteEntity.builder().nomeCompleto("Paciente Teste").ativo(true).build();
        cliente.setId(id);
        return cliente;
    }
}
