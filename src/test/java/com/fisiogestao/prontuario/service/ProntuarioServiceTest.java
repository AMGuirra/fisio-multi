package com.fisiogestao.prontuario.service;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.prontuario.ProntuarioSoapEntity;
import com.fisiogestao.prontuario.TipoRegistroProntuario;
import com.fisiogestao.prontuario.form.AvaliacaoForm;
import com.fisiogestao.prontuario.form.EvolucaoEditForm;
import com.fisiogestao.prontuario.form.EvolucaoForm;
import com.fisiogestao.prontuario.repository.ProntuarioSoapRepository;
import com.fisiogestao.shared.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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
        when(prontuarioSoapRepository.saveAndFlush(any(ProntuarioSoapEntity.class)))
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

    /**
     * Regressão: duas requisições concorrentes de criação de avaliação (duplo clique/duplo
     * envio) leem "sem avaliação existente" antes de qualquer commit e tentam inserir ao
     * mesmo tempo. A segunda colide com o índice único parcial
     * uq_prontuario_soap_avaliacao_por_cliente (migração V50) e o repositório propaga
     * DataIntegrityViolationException — que antes subia crua até o usuário como HTTP 500.
     * A cobertura ponta a ponta com duas transações reais concorrentes está em
     * {@link ProntuarioServiceConcurrencyTest}; este teste isola só a tradução da exceção.
     */
    @Test
    void deveConverterViolacaoDoIndiceUnicoDeAvaliacaoEmBusinessException() {
        UUID clienteId = UUID.randomUUID();
        ClienteEntity cliente = clienteComId(clienteId);

        when(pacienteService.buscarPorId(clienteId)).thenReturn(cliente);
        when(prontuarioSoapRepository.findFirstByClienteIdAndTipoRegistro(clienteId, TipoRegistroProntuario.AVALIACAO))
                .thenReturn(Optional.empty());
        when(prontuarioSoapRepository.saveAndFlush(any(ProntuarioSoapEntity.class)))
                .thenThrow(violacaoDeConstraint("uq_prontuario_soap_avaliacao_por_cliente"));

        AvaliacaoForm form = new AvaliacaoForm();
        form.setClienteId(clienteId);
        form.setTexto("Avaliação enviada duas vezes.");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.salvarAvaliacao(form, "fisio1"));

        assertEquals(
                "Esta avaliação já foi salva em outra requisição. Atualize a página e confira o conteúdo antes de salvar novamente.",
                ex.getMessage());
        verify(auditService, never()).recordCreate(any(), any(), any(), any(), any(), any());
    }

    /**
     * Uma DataIntegrityViolationException de OUTRA constraint (ex.: FK de cliente, corrompida
     * por uma condição de corrida externa) não deve ser mascarada com a mensagem de "duplo
     * salvamento" — isso enganaria quem for investigar um problema diferente. O serviço deve
     * deixar a exceção original subir intacta.
     */
    @Test
    void naoConverteViolacaoDeOutraConstraintComoDuploSalvamento() {
        UUID clienteId = UUID.randomUUID();
        ClienteEntity cliente = clienteComId(clienteId);

        when(pacienteService.buscarPorId(clienteId)).thenReturn(cliente);
        when(prontuarioSoapRepository.findFirstByClienteIdAndTipoRegistro(clienteId, TipoRegistroProntuario.AVALIACAO))
                .thenReturn(Optional.empty());
        DataIntegrityViolationException outraViolacao = violacaoDeConstraint("fk_prontuario_soap_cliente");
        when(prontuarioSoapRepository.saveAndFlush(any(ProntuarioSoapEntity.class)))
                .thenThrow(outraViolacao);

        AvaliacaoForm form = new AvaliacaoForm();
        form.setClienteId(clienteId);
        form.setTexto("Avaliação qualquer.");

        DataIntegrityViolationException propagada = assertThrows(
                DataIntegrityViolationException.class,
                () -> service.salvarAvaliacao(form, "fisio1"));

        assertEquals(outraViolacao, propagada);
    }

    private DataIntegrityViolationException violacaoDeConstraint(String nomeConstraint) {
        ConstraintViolationException causa = new ConstraintViolationException(
                "constraint violada", null, nomeConstraint);
        return new DataIntegrityViolationException("violação de integridade", causa);
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

    @Test
    void deveAtualizarTextoDeEvolucaoExistenteComAuditoria() {
        UUID clienteId = UUID.randomUUID();
        UUID evolucaoId = UUID.randomUUID();
        ClienteEntity cliente = clienteComId(clienteId);

        ProntuarioSoapEntity existente = ProntuarioSoapEntity.builder()
                .cliente(cliente)
                .tipoRegistro(TipoRegistroProntuario.EVOLUCAO)
                .textoRegistro("Texto original")
                .criadoPor("fisio1")
                .build();
        existente.setId(evolucaoId);

        when(prontuarioSoapRepository.findById(evolucaoId)).thenReturn(Optional.of(existente));
        when(prontuarioSoapRepository.save(any(ProntuarioSoapEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(auditService.snapshotOf(any())).thenReturn(Map.of("textoRegistro", "Texto original"));

        EvolucaoEditForm form = new EvolucaoEditForm();
        form.setClienteId(clienteId);
        form.setTexto("Texto atualizado");

        ProntuarioSoapEntity salvo = service.atualizarEvolucao(evolucaoId, form, "fisio2");

        assertEquals("Texto atualizado", salvo.getTextoRegistro());
        verify(auditService).recordUpdate(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void deveRejeitarEdicaoDeEvolucaoVazia() {
        UUID clienteId = UUID.randomUUID();
        UUID evolucaoId = UUID.randomUUID();

        EvolucaoEditForm form = new EvolucaoEditForm();
        form.setClienteId(clienteId);
        form.setTexto("   ");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.atualizarEvolucao(evolucaoId, form, "fisio1"));

        assertEquals("A evolução não pode ficar vazia.", ex.getMessage());
        verify(prontuarioSoapRepository, never()).save(any());
    }

    @Test
    void deveRejeitarEdicaoDeEvolucaoDeOutroPaciente() {
        UUID clienteId = UUID.randomUUID();
        UUID evolucaoId = UUID.randomUUID();
        ClienteEntity cliente = clienteComId(clienteId);
        ClienteEntity outroCliente = clienteComId(UUID.randomUUID());

        ProntuarioSoapEntity existente = ProntuarioSoapEntity.builder()
                .cliente(cliente)
                .tipoRegistro(TipoRegistroProntuario.EVOLUCAO)
                .textoRegistro("Texto original")
                .criadoPor("fisio1")
                .build();
        existente.setId(evolucaoId);

        when(prontuarioSoapRepository.findById(evolucaoId)).thenReturn(Optional.of(existente));

        EvolucaoEditForm form = new EvolucaoEditForm();
        form.setClienteId(outroCliente.getId());
        form.setTexto("Tentativa de edição cruzada.");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.atualizarEvolucao(evolucaoId, form, "fisio1"));

        assertEquals("A evolução informada não pertence ao paciente selecionado.", ex.getMessage());
        verify(prontuarioSoapRepository, never()).save(any());
    }

    @Test
    void deveExcluirEvolucaoComAuditoria() {
        UUID clienteId = UUID.randomUUID();
        UUID evolucaoId = UUID.randomUUID();
        ClienteEntity cliente = clienteComId(clienteId);

        ProntuarioSoapEntity existente = ProntuarioSoapEntity.builder()
                .cliente(cliente)
                .tipoRegistro(TipoRegistroProntuario.EVOLUCAO)
                .textoRegistro("Texto a excluir")
                .criadoPor("fisio1")
                .build();
        existente.setId(evolucaoId);

        when(prontuarioSoapRepository.findById(evolucaoId)).thenReturn(Optional.of(existente));
        when(auditService.snapshotOf(any())).thenReturn(Map.of("textoRegistro", "Texto a excluir"));

        service.excluirEvolucao(evolucaoId, clienteId, "fisio2");

        verify(prontuarioSoapRepository).delete(existente);
        verify(auditService).recordAction(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void naoPermiteEditarRegistroDeAvaliacaoComoEvolucao() {
        UUID clienteId = UUID.randomUUID();
        UUID avaliacaoId = UUID.randomUUID();
        ClienteEntity cliente = clienteComId(clienteId);

        ProntuarioSoapEntity avaliacao = ProntuarioSoapEntity.builder()
                .cliente(cliente)
                .tipoRegistro(TipoRegistroProntuario.AVALIACAO)
                .textoRegistro("Avaliação principal")
                .criadoPor("fisio1")
                .build();
        avaliacao.setId(avaliacaoId);

        when(prontuarioSoapRepository.findById(avaliacaoId)).thenReturn(Optional.of(avaliacao));

        EvolucaoEditForm form = new EvolucaoEditForm();
        form.setClienteId(clienteId);
        form.setTexto("Tentando editar avaliação pelo fluxo de evolução.");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.atualizarEvolucao(avaliacaoId, form, "fisio1"));

        assertEquals("Registro de evolução não encontrado.", ex.getMessage());
        verify(prontuarioSoapRepository, never()).save(any());
    }

    private ClienteEntity clienteComId(UUID id) {
        ClienteEntity cliente = ClienteEntity.builder().nomeCompleto("Paciente Teste").ativo(true).build();
        cliente.setId(id);
        return cliente;
    }
}
