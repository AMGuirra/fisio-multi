package com.fisiogestao.prontuario.service;

import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.config.ClinicTimezoneHolder;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.prontuario.ProntuarioAnexoEntity;
import com.fisiogestao.prontuario.TipoAnexoProntuario;
import com.fisiogestao.prontuario.dto.ProntuarioAnexoResumoDto;
import com.fisiogestao.prontuario.repository.ProntuarioAnexoRepository;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProntuarioAnexoService {

    /** Limite máximo por arquivo (documento ou foto): 15 MB. */
    static final long MAX_TAMANHO_BYTES = 15L * 1024 * 1024;

    /** Limite de fotos por evolução — mantém o upload leve e a UI previsível. */
    static final int MAX_FOTOS_POR_EVOLUCAO = 6;

    /** Limite do somatório de fotos por evolução (abaixo do max-request-size do multipart). */
    static final long MAX_TAMANHO_TOTAL_FOTOS_BYTES = 18L * 1024 * 1024;

    /** Tipos MIME aceitos para documentos genéricos. */
    private static final Set<String> TIPOS_DOCUMENTO_PERMITIDOS = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    /** Tipos MIME aceitos para fotografias de acompanhamento clínico. */
    private static final Set<String> TIPOS_FOTO_PERMITIDOS = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F'};
    private static final byte[] ZIP_MAGIC = {0x50, 0x4B, 0x03, 0x04};

    private final ProntuarioAnexoRepository anexoRepository;
    private final PacienteService pacienteService;
    private final AuditService auditService;
    private final ImagemClinicaProcessor imagemClinicaProcessor;

    /**
     * Lista os metadados dos anexos de um paciente sem carregar o binário.
     */
    public List<ProntuarioAnexoResumoDto> listarPorPaciente(UUID clienteId) {
        return anexoRepository.findResumosByClienteId(clienteId);
    }

    /** Apenas documentos genéricos (exclui fotos clínicas). */
    public List<ProntuarioAnexoResumoDto> listarDocumentosPorPaciente(UUID clienteId) {
        return anexoRepository.findResumosByClienteIdAndTipoAnexo(clienteId, TipoAnexoProntuario.DOCUMENTO);
    }

    /** Fotos clínicas vinculadas a uma evolução específica. */
    public List<ProntuarioAnexoResumoDto> listarFotosPorEvolucao(UUID prontuarioSoapId) {
        return anexoRepository.findFotosByProntuarioSoapId(prontuarioSoapId);
    }

    /**
     * Valida e persiste um novo documento genérico no prontuário do paciente.
     *
     * @param clienteId        UUID do paciente
     * @param prontuarioSoapId UUID do registro ao qual vincular (opcional)
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
        if (tipoMime == null || !TIPOS_DOCUMENTO_PERMITIDOS.contains(tipoMime)) {
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

        validarAssinaturaDocumento(dados, tipoMime);

        String nomeOriginal = sanitizarNomeArquivo(arquivo.getOriginalFilename());

        ProntuarioAnexoEntity entidade = ProntuarioAnexoEntity.builder()
                .cliente(cliente)
                .prontuarioSoapId(prontuarioSoapId)
                .nomeOriginal(nomeOriginal)
                .tipoMime(tipoMime)
                .tamanhoBytes(dados.length)
                .dados(dados)
                .descricao(descricao != null && !descricao.isBlank() ? descricao.trim() : null)
                .criadoPor(usuario)
                .tipoAnexo(TipoAnexoProntuario.DOCUMENTO)
                .hashSha256(sha256Hex(dados))
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
     * Valida e persiste as fotos de acompanhamento clínico de uma evolução, em bloco atômico:
     * se qualquer foto falhar a validação, nenhuma é gravada (a transação é revertida pelo
     * método chamador, que também gravou o registro de evolução).
     */
    @Transactional
    public List<ProntuarioAnexoEntity> salvarFotosClinicas(
            UUID clienteId,
            UUID prontuarioSoapId,
            List<MultipartFile> fotos,
            String regiaoCorporal,
            String observacaoClinica,
            String usuario,
            boolean consentimentoConfirmado
    ) {
        if (fotos == null || fotos.isEmpty()) {
            return List.of();
        }
        if (fotos.size() > MAX_FOTOS_POR_EVOLUCAO) {
            throw new BusinessException(
                    "Envie no máximo " + MAX_FOTOS_POR_EVOLUCAO + " fotos por evolução.");
        }
        if (!consentimentoConfirmado) {
            throw new BusinessException(
                    "Confirme que o paciente autorizou o registro fotográfico antes de enviar as fotos.");
        }
        if (regiaoCorporal == null || regiaoCorporal.isBlank()) {
            throw new BusinessException("Informe a região corporal fotografada.");
        }
        if (observacaoClinica == null || observacaoClinica.isBlank()) {
            throw new BusinessException("Informe uma observação clínica para as fotos.");
        }

        long tamanhoTotal = fotos.stream().mapToLong(MultipartFile::getSize).sum();
        if (tamanhoTotal > MAX_TAMANHO_TOTAL_FOTOS_BYTES) {
            throw new BusinessException("O total das fotos enviadas excede o limite permitido por evolução.");
        }

        ClienteEntity cliente = pacienteService.buscarPorId(clienteId);
        String regiao = regiaoCorporal.trim();
        String observacao = observacaoClinica.trim();
        OffsetDateTime consentimentoEm = ClinicTimezoneHolder.now();

        List<ProntuarioAnexoEntity> salvos = new ArrayList<>();
        for (MultipartFile foto : fotos) {
            salvos.add(processarESalvarFoto(cliente, prontuarioSoapId, foto, regiao, observacao, usuario, consentimentoEm));
        }
        return salvos;
    }

    private ProntuarioAnexoEntity processarESalvarFoto(
            ClienteEntity cliente,
            UUID prontuarioSoapId,
            MultipartFile foto,
            String regiaoCorporal,
            String observacaoClinica,
            String usuario,
            OffsetDateTime consentimentoEm
    ) {
        if (foto == null || foto.isEmpty()) {
            throw new BusinessException("Um dos arquivos enviados está vazio ou corrompido.");
        }

        String tipoMime = foto.getContentType();
        if (tipoMime == null || !TIPOS_FOTO_PERMITIDOS.contains(tipoMime)) {
            throw new BusinessException("Envie apenas fotos em JPEG, PNG ou WebP.");
        }
        if (foto.getSize() > MAX_TAMANHO_BYTES) {
            throw new BusinessException("Cada foto deve ter no máximo 15 MB.");
        }

        byte[] originais;
        try {
            originais = foto.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Não foi possível ler uma das fotos enviadas. Tente novamente.");
        }

        imagemClinicaProcessor.validarAssinatura(originais, tipoMime);
        byte[] higienizados = imagemClinicaProcessor.higienizar(originais, tipoMime);

        String nomeOriginal = sanitizarNomeArquivo(foto.getOriginalFilename());

        ProntuarioAnexoEntity entidade = ProntuarioAnexoEntity.builder()
                .cliente(cliente)
                .prontuarioSoapId(prontuarioSoapId)
                .nomeOriginal(nomeOriginal)
                .tipoMime(tipoMime)
                .tamanhoBytes(higienizados.length)
                .dados(higienizados)
                .descricao(observacaoClinica)
                .criadoPor(usuario)
                .tipoAnexo(TipoAnexoProntuario.FOTO_CLINICA)
                .regiaoCorporal(regiaoCorporal)
                .consentimentoConfirmadoEm(consentimentoEm)
                .hashSha256(sha256Hex(higienizados))
                .build();

        ProntuarioAnexoEntity salvo = anexoRepository.save(entidade);

        auditService.recordCreate(
                AuditModule.PRONTUARIO,
                "prontuario_anexo",
                salvo,
                null,
                "Foto de acompanhamento clínico anexada à evolução de " + cliente.getNomeCompleto() + ".",
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

    /** Carrega uma foto clínica específica, garantindo que não é um documento genérico. */
    public ProntuarioAnexoEntity buscarFotoClinica(UUID id) {
        ProntuarioAnexoEntity anexo = buscarParaDownload(id);
        if (anexo.getTipoAnexo() != TipoAnexoProntuario.FOTO_CLINICA) {
            throw new BusinessException("Arquivo não encontrado.");
        }
        return anexo;
    }

    /** Gera uma miniatura (JPEG, lado maior limitado) de uma foto clínica, sem transferir o binário completo. */
    public byte[] gerarMiniatura(UUID id, int ladoMaximo) {
        ProntuarioAnexoEntity foto = buscarFotoClinica(id);
        return imagemClinicaProcessor.gerarMiniatura(foto.getDados(), ladoMaximo);
    }

    /**
     * Exclui permanentemente um anexo (documento ou foto) e registra o evento na auditoria.
     * O anexo precisa pertencer ao paciente informado, como já ocorre na exclusão de evolução.
     */
    @Transactional
    public void excluir(UUID id, UUID clienteId, String usuario) {
        ProntuarioAnexoEntity entidade = anexoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Arquivo não encontrado."));
        if (clienteId == null || !clienteId.equals(entidade.getCliente().getId())) {
            throw new BusinessException("O arquivo informado não pertence ao paciente selecionado.");
        }

        String nomeOriginal = entidade.getNomeOriginal();
        String entidadeId = id.toString();
        var snapshotAnterior = auditService.snapshotOf(entidade);
        boolean isFoto = entidade.getTipoAnexo() == TipoAnexoProntuario.FOTO_CLINICA;

        anexoRepository.delete(entidade);

        auditService.recordAction(
                AuditModule.PRONTUARIO,
                "prontuario_anexo",
                entidadeId,
                (isFoto ? "Foto clínica '" : "Arquivo '") + nomeOriginal + "' excluído do prontuário.",
                AuditActionType.DELETE_PHYSICAL,
                snapshotAnterior,
                null,
                null,
                "Exclusão de " + (isFoto ? "foto clínica" : "documento clínico") + " pelo usuário " + usuario + ".",
                AuditEventOrigin.MANUAL
        );
    }

    private void validarAssinaturaDocumento(byte[] dados, String tipoMime) {
        if (dados == null || dados.length < 4) {
            throw new BusinessException("Arquivo inválido ou corrompido.");
        }
        boolean valido = switch (tipoMime) {
            case "application/pdf" -> matchesAt(dados, 0, PDF_MAGIC);
            case "image/jpeg", "image/png", "image/webp" -> {
                try {
                    imagemClinicaProcessor.validarAssinatura(dados, tipoMime);
                    yield true;
                } catch (BusinessException ex) {
                    yield false;
                }
            }
            // DOCX é um contêiner ZIP (Office Open XML): valida apenas a assinatura ZIP,
            // já que o projeto não depende de uma biblioteca de inspeção de conteúdo (ex.: Tika).
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                    matchesAt(dados, 0, ZIP_MAGIC);
            default -> false;
        };
        if (!valido) {
            throw new BusinessException("O conteúdo do arquivo não corresponde ao tipo declarado.");
        }
    }

    private boolean matchesAt(byte[] dados, int offset, byte[] assinatura) {
        if (dados.length < offset + assinatura.length) {
            return false;
        }
        for (int i = 0; i < assinatura.length; i++) {
            if (dados[offset + i] != assinatura[i]) {
                return false;
            }
        }
        return true;
    }

    /** Remove qualquer componente de caminho e caracteres de controle do nome enviado pelo cliente. */
    private String sanitizarNomeArquivo(String nomeOriginal) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            return "arquivo";
        }
        String base = Paths.get(nomeOriginal.replace('\\', '/')).getFileName().toString();
        String semControle = base.replaceAll("[\\p{Cntrl}]", "").trim();
        if (semControle.isEmpty()) {
            return "arquivo";
        }
        return semControle.length() > 255 ? semControle.substring(0, 255) : semControle;
    }

    private String sha256Hex(byte[] dados) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(dados));
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException("Não foi possível processar o arquivo enviado.");
        }
    }
}
