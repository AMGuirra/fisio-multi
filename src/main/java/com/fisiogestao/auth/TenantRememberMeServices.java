package com.fisiogestao.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

import java.util.Collection;
import java.util.UUID;

/**
 * "Lembrar-me" baseado no ID do usuário: usernames se repetem entre clínicas, então o cookie
 * identifica a conta pelo UUID. No auto-login, o vínculo e o status da clínica são revalidados.
 */
public class TenantRememberMeServices extends TokenBasedRememberMeServices {

    public TenantRememberMeServices(String key, UsuarioSistemaRepository repository, UsuarioAuthenticationProvider provider) {
        super(key, carregarPorId(repository, provider));
    }

    @Override
    protected String retrieveUserName(Authentication authentication) {
        if (authentication.getPrincipal() instanceof UsuarioAutenticado usuario && usuario.getId() != null) {
            return usuario.getId().toString();
        }
        return super.retrieveUserName(authentication);
    }

    @Override
    protected Authentication createSuccessfulAuthentication(HttpServletRequest request, UserDetails user) {
        UserDetails principal = user instanceof CookieUser cookieUser ? cookieUser.usuario() : user;
        RememberMeAuthenticationToken auth = new RememberMeAuthenticationToken(getKey(), principal, principal.getAuthorities());
        auth.setDetails(getAuthenticationDetailsSource().buildDetails(request));
        return auth;
    }

    private static UserDetailsService carregarPorId(UsuarioSistemaRepository repository, UsuarioAuthenticationProvider provider) {
        return id -> {
            UUID uuid;
            try {
                uuid = UUID.fromString(id);
            } catch (IllegalArgumentException ex) {
                throw new UsernameNotFoundException("Sessão lembrada inválida.");
            }
            UsuarioSistemaEntity usuario = repository.findById(uuid)
                    .orElseThrow(() -> new UsernameNotFoundException("Sessão lembrada inválida."));
            return new CookieUser(provider.montarPrincipal(usuario), usuario.getPasswordHash());
        };
    }

    /** Adaptador cujo "username" é o UUID, para que a assinatura do cookie confira. */
    private record CookieUser(UsuarioAutenticado usuario, String passwordHash) implements UserDetails {
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return usuario.getAuthorities();
        }

        @Override
        public String getPassword() {
            return passwordHash;
        }

        @Override
        public String getUsername() {
            return usuario.getId().toString();
        }
    }
}
