package com.fisiogestao.prontuario;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Documento clínico anexado ao prontuário de um paciente.
 *
 * <p>O binário é armazenado como {@code BYTEA} diretamente no PostgreSQL,
 * eliminando dependência de armazenamento externo. O campo {@code dados}
 * é marcado com {@code @JsonIgnore} para não ser incluído nos snapshots de auditoria.</p>
 *
 * <p>O vínculo com {@code prontuario_soap_id} é opcional — um documento pode
 * pertencer ao histórico geral do paciente sem estar ligado a uma sessão específica.</p>
 */
@Entity
@Table(name = "prontuario_anexo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ProntuarioAnexoEntity extends BaseEntity {

    /** Paciente ao qual o documento pertence. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEntity cliente;

    /**
     * Vínculo opcional com um registro SOAP específico.
     * Armazenado como UUID puro (sem relacionamento JPA) para permitir
     * consultas de metadados sem carregar o binário de outras entidades.
     */
    @Column(name = "prontuario_soap_id")
    private UUID prontuarioSoapId;

    /** Nome original do arquivo enviado pelo usuário. */
    @Column(name = "nome_original", nullable = false, length = 255)
    private String nomeOriginal;

    /** Tipo MIME do arquivo (ex.: {@code application/pdf}, {@code image/jpeg}). */
    @Column(name = "tipo_mime", nullable = false, length = 100)
    private String tipoMime;

    /** Tamanho do arquivo em bytes. */
    @Column(name = "tamanho_bytes", nullable = false)
    private long tamanhoBytes;

    /**
     * Conteúdo binário do arquivo.
     * Excluído da serialização JSON para não poluir logs de auditoria.
     */
    @JsonIgnore
    @Column(name = "dados", nullable = false, columnDefinition = "BYTEA")
    private byte[] dados;

    /** Descrição livre fornecida pelo profissional ao fazer o upload (opcional). */
    @Column(name = "descricao", length = 500)
    private String descricao;

    /** Login do usuário que realizou o upload. */
    @Column(name = "criado_por", nullable = false, length = 80)
    private String criadoPor;

    /** DOCUMENTO (laudo/PDF/DOCX genérico) ou FOTO_CLINICA (acompanhamento fotográfico). */
    @Column(name = "tipo_anexo", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TipoAnexoProntuario tipoAnexo = TipoAnexoProntuario.DOCUMENTO;

    /** Região corporal fotografada. Aplicável apenas a FOTO_CLINICA. */
    @Column(name = "regiao_corporal", length = 120)
    private String regiaoCorporal;

    /**
     * Data/hora em que o profissional confirmou que o paciente autorizou o registro
     * fotográfico para acompanhamento clínico. Aplicável apenas a FOTO_CLINICA.
     */
    @Column(name = "consentimento_confirmado_em")
    private OffsetDateTime consentimentoConfirmadoEm;

    /** Hash SHA-256 do conteúdo do arquivo, para verificação de integridade. */
    @Column(name = "hash_sha256", length = 64)
    private String hashSha256;
}
