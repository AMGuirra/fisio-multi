package com.fisiogestao.prontuario.service;

import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.prontuario.ProntuarioAnexoEntity;
import com.fisiogestao.prontuario.repository.ProntuarioAnexoRepository;
import com.fisiogestao.shared.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private ProntuarioAnexoService service;

    @Test
    void deveSalvarAnexoValidoNormalizandoDescricao() {
        UUID clienteId = UUID.randomUUID();
        ClienteEntity cliente = ClienteEntity.builder().nomeCompleto("Paciente").ativo(true).build();
        cliente.setId(clienteId);
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "laudo.pdf",
                "application/pdf",
                new byte[] {1, 2, 3}
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
        assertArrayEquals(new byte[] {1, 2, 3}, salvo.getDados());
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
}
