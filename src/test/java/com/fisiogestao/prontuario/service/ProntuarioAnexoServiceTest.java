package com.fisiogestao.prontuario.service;

import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.prontuario.ProntuarioAnexoEntity;
import com.fisiogestao.prontuario.TipoAnexoProntuario;
import com.fisiogestao.prontuario.repository.ProntuarioAnexoRepository;
import com.fisiogestao.shared.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProntuarioAnexoServiceTest {

    @Mock
    private ProntuarioAnexoRepository anexoRepository;

    @Mock
    private PacienteService pacienteService;

    @Mock
    private AuditService auditService;

    @Mock
    private ImagemClinicaProcessor imagemClinicaProcessor;

    @InjectMocks
    private ProntuarioAnexoService service;

    @Test
    void deveSalvarAnexoValidoNormalizandoDescricao() {
        UUID clienteId = UUID.randomUUID();
        ClienteEntity cliente = ClienteEntity.builder().nomeCompleto("Paciente").ativo(true).build();
        cliente.setId(clienteId);
        byte[] conteudoPdf = "%PDF-1.4 conteudo de teste".getBytes(StandardCharsets.US_ASCII);
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "laudo.pdf",
                "application/pdf",
                conteudoPdf
        );

        when(pacienteService.buscarPorId(clienteId)).thenReturn(cliente);
        when(anexoRepository.save(any(ProntuarioAnexoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProntuarioAnexoEntity salvo = service.salvar(clienteId, null, "  Laudo inicial  ", arquivo, "admin");

        assertEquals(cliente, salvo.getCliente());
        assertEquals("laudo.pdf", salvo.getNomeOriginal());
        assertEquals("application/pdf", salvo.getTipoMime());
        assertEquals("Laudo inicial", salvo.getDescricao());
        assertEquals("admin", salvo.getCriadoPor());
        assertEquals(TipoAnexoProntuario.DOCUMENTO, salvo.getTipoAnexo());
        verify(auditService).recordCreate(any(), any(), any(), any(), any(), any());
    }

    @Test
    void deveRejeitarTipoMimeNaoPermitido() {
        UUID clienteId = UUID.randomUUID();
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "script.exe",
                "application/octet-stream",
                new byte[] {1}
        );

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.salvar(clienteId, null, null, arquivo, "admin")
        );

        assertEquals("Tipo de arquivo não permitido. Envie PDF, JPG, PNG, WebP ou DOCX.", ex.getMessage());
        verify(anexoRepository, never()).save(any());
    }

    @Test
    void deveRejeitarPdfComAssinaturaBinariaInvalida() {
        UUID clienteId = UUID.randomUUID();
        ClienteEntity cliente = ClienteEntity.builder().nomeCompleto("Paciente").ativo(true).build();
        cliente.setId(clienteId);
        lenient().when(pacienteService.buscarPorId(clienteId)).thenReturn(cliente);

        MockMultipartFile arquivoForjado = new MockMultipartFile(
                "arquivo",
                "falso.pdf",
                "application/pdf",
                "não é um pdf de verdade".getBytes(StandardCharsets.UTF_8)
        );

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.salvar(clienteId, null, null, arquivoForjado, "admin")
        );

        assertEquals("O conteúdo do arquivo não corresponde ao tipo declarado.", ex.getMessage());
        verify(anexoRepository, never()).save(any());
    }

    @Test
    void deveBuscarAnexoParaDownload() {
        UUID id = UUID.randomUUID();
        ProntuarioAnexoEntity anexo = ProntuarioAnexoEntity.builder()
                .nomeOriginal("imagem.png")
                .tipoMime("image/png")
                .dados(new byte[] {9})
                .build();
        anexo.setId(id);
        when(anexoRepository.findById(id)).thenReturn(Optional.of(anexo));

        assertEquals(anexo, service.buscarParaDownload(id));
    }

    @Test
    void deveSalvarFotosClinicasValidasComConsentimento() {
        UUID clienteId = UUID.randomUUID();
        UUID evolucaoId = UUID.randomUUID();
        ClienteEntity cliente = ClienteEntity.builder().nomeCompleto("Paciente").ativo(true).build();
        cliente.setId(clienteId);

        byte[] bytesOriginais = {1, 2, 3, 4};
        byte[] bytesHigienizados = {5, 6, 7};
        MockMultipartFile foto = new MockMultipartFile("fotos", "joelho.jpg", "image/jpeg", bytesOriginais);

        when(pacienteService.buscarPorId(clienteId)).thenReturn(cliente);
        when(imagemClinicaProcessor.higienizar(any(), any())).thenReturn(bytesHigienizados);
        when(anexoRepository.save(any(ProntuarioAnexoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<ProntuarioAnexoEntity> salvos = service.salvarFotosClinicas(
                clienteId, evolucaoId, List.of(foto), "Joelho direito", "Redução do edema", "admin", true);

        assertEquals(1, salvos.size());
        ProntuarioAnexoEntity salvo = salvos.get(0);
        assertEquals(TipoAnexoProntuario.FOTO_CLINICA, salvo.getTipoAnexo());
        assertEquals("Joelho direito", salvo.getRegiaoCorporal());
        assertEquals(evolucaoId, salvo.getProntuarioSoapId());
        assertTrue(salvo.getConsentimentoConfirmadoEm() != null);
        verify(imagemClinicaProcessor).validarAssinatura(bytesOriginais, "image/jpeg");
    }

    @Test
    void deveRejeitarFotosSemConsentimento() {
        UUID clienteId = UUID.randomUUID();
        MockMultipartFile foto = new MockMultipartFile("fotos", "joelho.jpg", "image/jpeg", new byte[] {1});

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.salvarFotosClinicas(
                        clienteId, null, List.of(foto), "Joelho", "Observação", "admin", false)
        );

        assertEquals(
                "Confirme que o paciente autorizou o registro fotográfico antes de enviar as fotos.",
                ex.getMessage());
        verify(anexoRepository, never()).save(any());
    }

    @Test
    void deveRejeitarQuantidadeDeFotosAcimaDoLimite() {
        UUID clienteId = UUID.randomUUID();
        List<MultipartFile> fotos = java.util.stream.IntStream.range(0, ProntuarioAnexoService.MAX_FOTOS_POR_EVOLUCAO + 1)
                .mapToObj(i -> (MultipartFile) new MockMultipartFile("fotos", "f" + i + ".jpg", "image/jpeg", new byte[] {1}))
                .toList();

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.salvarFotosClinicas(clienteId, null, fotos, "Joelho", "Observação", "admin", true)
        );

        assertTrue(ex.getMessage().contains("no máximo"));
        verify(anexoRepository, never()).save(any());
    }

    @Test
    void deveRejeitarFotoComMimeNaoPermitido() {
        UUID clienteId = UUID.randomUUID();
        ClienteEntity cliente = ClienteEntity.builder().nomeCompleto("Paciente").ativo(true).build();
        cliente.setId(clienteId);
        lenient().when(pacienteService.buscarPorId(clienteId)).thenReturn(cliente);

        MockMultipartFile arquivo = new MockMultipartFile("fotos", "laudo.pdf", "application/pdf", new byte[] {1});

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.salvarFotosClinicas(clienteId, null, List.of(arquivo), "Joelho", "Observação", "admin", true)
        );

        assertEquals("Envie apenas fotos em JPEG, PNG ou WebP.", ex.getMessage());
        verify(anexoRepository, never()).save(any());
    }
}
