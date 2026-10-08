package com.fisiogestao.auth;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Principal da sessão autenticada. Carrega a empresa resolvida no servidor durante o login;
 * é a única fonte de tenant para usuários autenticados (ver {@code TenantContext}).
 *
 * <p>Igualdade por {@code id}: usuários homônimos de clínicas diferentes são principais distintos
 * (importante para o controle de sessão única do Spring Security).</p>
 */
public class UsuarioAutenticado implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String username;
    private String passwordHash;
    private final UserRole role;
    private final UUID empresaId;
    private final String empresaSlug;

    public UsuarioAutenticado(UUID id, String username, String passwordHash, UserRole role, UUID empresaId, String empresaSlug) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.empresaId = empresaId;
        this.empresaSlug = empresaSlug;
    }

    public static UsuarioAutenticado of(UsuarioSistemaEntity usuario, String empresaSlug) {
        return new UsuarioAutenticado(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getPasswordHash(),
                usuario.getRole(),
                usuario.getEmpresaId(),
                empresaSlug
        );
    }

    public UUID getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public String getEmpresaSlug() {
        return empresaSlug;
    }

    public boolean isSuperAdmin() {
        return role != null && role.isSuperAdmin();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UsuarioAutenticado outro)) {
            return false;
        }
        if (id != null || outro.id != null) {
            return Objects.equals(id, outro.id);
        }
        return Objects.equals(username, outro.username) && Objects.equals(empresaId, outro.empresaId);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : Objects.hash(username, empresaId);
    }

    @Override
    public String toString() {
        return "UsuarioAutenticado[" + username + ", empresa=" + empresaId + ", role=" + role + "]";
    }
}
