package com.fisiogestao.prontuario.service;

import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.prontuario.ProntuarioAnexoEntity;
import com.fisiogestao.prontuario.dto.ProntuarioAnexoResumoDto;
import com.fisiogestao.prontuario.repository.ProntuarioAnexoRepository;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProntuarioAnexoService {

    /** Limite máximo por arquivo: 15 MB. */
    private static final long MAX_TAMANHO_BYTES = 15L * 1024 * 1024;

    /** Tipos MIME aceitos pelo sistema. */
    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final ProntuarioAnexoRepository anexoRepository;
    private final PacienteService pacienteService;
    private final AuditService auditService;

    /**
     * Lista os metadados dos anexos de um paciente sem carregar o binário.
     */
    public List<ProntuarioAnexoResumoDto> listarPorPaciente(UUID clienteId) {
        return anexoRepository.findResumosByClienteId(clienteId);
    }

    /**
     * Valida e persiste um novo arquivo no prontuário do paciente.
     *
     * @param clienteId        UUID do paciente
     * @param prontuarioSoapId UUID do registro SOAP ao qual vincular (opcional)
     * @param descricao        Descrição livre do documento (opcional)
     * @param arquivo          Arquivo enviado via multipart
     * @param usuario          Login do usuário autenticado
     */
    @Transactional
    public ProntuarioAnexoEntity salvar(
            UUID clienteId,
            UUID prontuarioSoapId,
            String descricao,
            MultipartFile arquivo,
            String usuario
    ) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new BusinessException("Nenhum arquivo foi enviado.");
        }

        String tipoMime = arquivo.getContentType();
        if (tipoMime == null || !TIPOS_PERMITIDOS.contains(tipoMime)) {
            throw new BusinessException(
                    "Tipo de arquivo não permitido. Envie PDF, JPG, PNG, WebP ou DOCX.");
        }

        if (arquivo.getSize() > MAX_TAMANHO_BYTES) {
            throw new BusinessException("O arquivo excede o limite de 15 MB.");
        }

        ClienteEntity cliente = pacienteService.buscarPorId(clienteId);

        byte[] dados;
        try {
            dados = arquivo.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Não foi possível ler o arquivo. Tente novamente.");
        }

        String nomeOriginal = arquivo.getOriginalFilename() != null
                ? arquivo.getOriginalFilename()
                : "documento";

        ProntuarioAnexoEntity entidade = ProntuarioAnexoEntity.builder()
                .cliente(cliente)
                .prontuarioSoapId(prontuarioSoapId)
                .nomeOriginal(nomeOriginal)
                .tipoMime(tipoMime)
                .tamanhoBytes(arquivo.getSize())
                .dados(dados)
                .descricao(descricao != null && !descricao.isBlank() ? descricao.trim() : null)
                .criadoPor(usuario)
                .build();

        ProntuarioAnexoEntity salvo = anexoRepository.save(entidade);

        auditService.recordCreate(
                AuditModule.PRONTUARIO,
                "prontuario_anexo",
                salvo,
                null,
                "Arquivo '" + nomeOriginal + "' anexado ao prontuário.",
                AuditEventOrigin.MANUAL
        );

        return salvo;
    }

    /**
     * Carrega o arquivo completo (incluindo binário) para download ou exibição inline.
     */
    public ProntuarioAnexoEntity buscarParaDownload(UUID id) {
        return anexoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Arquivo não encontrado."));
    }

    /**
     * Exclui permanentemente um anexo e registra o evento na auditoria.
     */
    @Transactional
    public void excluir(UUID id, String usuario) {
        ProntuarioAnexoEntity entidade = anexoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Arquivo não encontrado."));

        String nomeOriginal = entidade.getNomeOriginal();
        String entidadeId = id.toString();

        anexoRepository.delete(entidade);

        auditService.recordAction(
                AuditModule.PRONTUARIO,
                "prontuario_anexo",
                entidadeId,
                "Arquivo '" + nomeOriginal + "' excluído do prontuário.",
                AuditActionType.DELETE_PHYSICAL,
                null,
                null,
                null,
                "Exclusão de documento clínico pelo usuário " + usuario + ".",
                AuditEventOrigin.MANUAL
        );
    }
}
