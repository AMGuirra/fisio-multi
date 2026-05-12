package com.fisiogestao.prontuario.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Projeção de metadados de um anexo do prontuário.
 *
 * <p>Usado nas listagens para evitar carregar o campo {@code dados} (BYTEA),
 * que só deve ser lido ao realizar o download/visualização do arquivo.</p>
 */
public class ProntuarioAnexoResumoDto {

    private final UUID id;
    private final String nomeOriginal;
    private final String tipoMime;
    private final long tamanhoBytes;
    private final String descricao;
    private final String criadoPor;
    private final OffsetDateTime createdAt;
    private final UUID prontuarioSoapId;

    public ProntuarioAnexoResumoDto(
            UUID id,
            String nomeOriginal,
            String tipoMime,
            long tamanhoBytes,
            String descricao,
            String criadoPor,
            OffsetDateTime createdAt,
            UUID prontuarioSoapId
    ) {
        this.id = id;
        this.nomeOriginal = nomeOriginal;
        this.tipoMime = tipoMime;
        this.tamanhoBytes = tamanhoBytes;
        this.descricao = descricao;
        this.criadoPor = criadoPor;
        this.createdAt = createdAt;
        this.prontuarioSoapId = prontuarioSoapId;
    }

    public UUID getId() { return id; }
    public String getNomeOriginal() { return nomeOriginal; }
    public String getTipoMime() { return tipoMime; }
    public long getTamanhoBytes() { return tamanhoBytes; }
    public String getDescricao() { return descricao; }
    public String getCriadoPor() { return criadoPor; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public UUID getProntuarioSoapId() { return prontuarioSoapId; }

    /** Tamanho formatado para exibição (B / KB / MB). */
    public String getTamanhoFormatado() {
        if (tamanhoBytes < 1_024) {
            return tamanhoBytes + " B";
        }
        if (tamanhoBytes < 1_024 * 1_024) {
            return String.format("%.1f KB", tamanhoBytes / 1_024.0);
        }
        return String.format("%.1f MB", tamanhoBytes / (1_024.0 * 1_024));
    }

    /** Indica se o arquivo é um PDF (pode ser exibido inline no browser). */
    public boolean isPdf() {
        return "application/pdf".equals(tipoMime);
    }

    /** Indica se o arquivo é uma imagem (pode ser exibida inline no browser). */
    public boolean isImagem() {
        return tipoMime != null && tipoMime.startsWith("image/");
    }

    /** Retorna o nome do ícone Lucide adequado ao tipo do arquivo. */
    public String getIcone() {
        if (isPdf()) return "file-text";
        if (isImagem()) return "image";
        if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(tipoMime)) {
            return "file-type-2";
        }
        return "file";
    }
}
