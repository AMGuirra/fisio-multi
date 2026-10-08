package com.fisiogestao.auth;

import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario_sistema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class UsuarioSistemaEntity extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String nome;

    @Column(nullable = false, length = 40)
    private String username;

    /** Empresa do usuário; {@code null} somente para {@link UserRole#SUPER_ADMIN}. */
    @Column(name = "empresa_id", updatable = false)
    private java.util.UUID empresaId;

    @Column(length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Default
    @Enumerated(EnumType.STRING)
    @Column(name = "tema_interface", nullable = false, length = 12)
    private UserThemePreference temaInterface = UserThemePreference.LIGHT;
}
