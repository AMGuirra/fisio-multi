package com.fisiogestao.auth;

import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "password_reset_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class PasswordResetTokenEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioSistemaEntity usuario;

    @Column(name = "token_hash", nullable = false, length = 128)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "requested_ip", length = 64)
    private String requestedIp;

    @Column(name = "requested_user_agent", length = 255)
    private String requestedUserAgent;

    public boolean ativoEm(OffsetDateTime dataHora) {
        return usedAt == null && expiresAt != null && expiresAt.isAfter(dataHora);
    }
}
