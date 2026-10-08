package com.fisiogestao.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Criação automática do SUPER_ADMIN. O padrão {@code admin/admin} existe apenas para uso
 * local/desenvolvimento: em produção a criação fica desligada (application-prod.yml) e,
 * mesmo se ligada, a senha padrão é recusada.
 */
@Component
@ConfigurationProperties(prefix = "app.bootstrap-super-admin")
public class SuperAdminBootstrapProperties {

    public static final String SENHA_LOCAL_PADRAO = "admin";

    private boolean enabled;
    private String nome = "Superadministrador";
    private String username = "admin";
    private String email;
    private String password = SENHA_LOCAL_PADRAO;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
