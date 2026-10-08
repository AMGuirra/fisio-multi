package com.fisiogestao.auditoria;

import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "auditoria_sistema")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "data_hora_evento", nullable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", nullable = false))
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class AuditEventEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private AuditModule modulo;

    @Column(nullable = false, length = 80)
    private String entidade;

    @Column(name = "entidade_id", length = 80)
    private String entidadeId;

    @Column(name = "descricao_registro", length = 255)
    private String descricaoRegistro;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_acao", nullable = false, length = 40)
    private AuditActionType tipoAcao;

    @Column(name = "campo_alterado", length = 255)
    private String campoAlterado;

    @Column(name = "valor_anterior", columnDefinition = "TEXT")
    private String valorAnterior;

    @Column(name = "valor_novo", columnDefinition = "TEXT")
    private String valorNovo;

    @Column(name = "payload_anterior_json", columnDefinition = "TEXT")
    private String payloadAnteriorJson;

    @Column(name = "payload_novo_json", columnDefinition = "TEXT")
    private String payloadNovoJson;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    /** Empresa do evento; {@code null} para ações da plataforma (Gestor/superadmin). */
    @Column(name = "empresa_id", updatable = false)
    private java.util.UUID empresaId;

    @Column(name = "usuario_id")
    private java.util.UUID usuarioId;

    @Column(name = "usuario_nome", length = 160)
    private String usuarioNome;

    @Column(name = "perfil_usuario", length = 60)
    private String perfilUsuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_evento", nullable = false, length = 40)
    private AuditEventOrigin origemEvento;

    @Column(name = "ip_origem", length = 64)
    private String ipOrigem;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "unidade_id", length = 80)
    private String unidadeId;

    @Column(name = "sucesso_operacao", nullable = false)
    private boolean sucessoOperacao;

    @Column(name = "alteracao_critica", nullable = false)
    private boolean alteracaoCritica;

    @OneToMany(mappedBy = "auditEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt asc")
    @Builder.Default
    private List<AuditFieldChangeEntity> fieldChanges = new ArrayList<>();

    public void addFieldChange(AuditFieldChangeEntity fieldChange) {
        if (fieldChange == null) {
            return;
        }
        fieldChange.setAuditEvent(this);
        fieldChanges.add(fieldChange);
    }
}
