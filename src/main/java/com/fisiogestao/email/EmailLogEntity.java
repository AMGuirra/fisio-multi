package com.fisiogestao.email;

import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "email_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class EmailLogEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private EmailLogType tipo;

    /** Empresa que originou o e-mail; {@code null} para notificações da plataforma. */
    @Column(name = "empresa_id", updatable = false)
    private java.util.UUID empresaId;

    @Column(name = "destinatario_principal", length = 160)
    private String destinatarioPrincipal;

    @Column(columnDefinition = "TEXT")
    private String destinatarios;

    @Column(nullable = false, length = 220)
    private String assunto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmailLogStatus status;

    @Column(name = "mensagem_erro", columnDefinition = "TEXT")
    private String mensagemErro;

    @Column(name = "template_id", length = 120)
    private String templateId;

    @Column(name = "usuario_disparador", length = 80)
    private String usuarioDisparador;

    @Column(name = "entidade_tipo", length = 80)
    private String entidadeTipo;

    @Column(name = "entidade_id")
    private UUID entidadeId;

    @Column(columnDefinition = "TEXT")
    private String observacoes;
}
