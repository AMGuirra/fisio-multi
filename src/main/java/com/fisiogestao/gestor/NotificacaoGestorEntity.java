package com.fisiogestao.gestor;

import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Alerta exibido no painel do Gestor. */
@Entity
@Table(name = "notificacao_gestor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacaoGestorEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoNotificacaoGestor tipo;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String mensagem;

    @Column(name = "empresa_id")
    private UUID empresaId;

    @Column(name = "referencia_id")
    private UUID referenciaId;

    @Column(length = 255)
    private String link;

    @Builder.Default
    @Column(nullable = false)
    private Boolean lida = false;
}
