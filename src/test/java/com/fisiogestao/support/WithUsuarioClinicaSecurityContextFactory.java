package com.fisiogestao.support;

import com.fisiogestao.auth.UserRole;
import com.fisiogestao.auth.UsuarioAutenticado;
import com.fisiogestao.auth.UsuarioSistemaEntity;
import com.fisiogestao.auth.UsuarioSistemaRepository;
import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.empresa.EmpresaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.UUID;

public class WithUsuarioClinicaSecurityContextFactory implements WithSecurityContextFactory<WithUsuarioClinica> {

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioSistemaRepository usuarioRepository;

    @Override
    public SecurityContext createSecurityContext(WithUsuarioClinica anotacao) {
        UserRole role = UserRole.valueOf(anotacao.roles()[0]);
        UUID empresaId = null;
        String slug = null;
        if (!anotacao.empresa().isBlank()) {
            EmpresaEntity empresa = empresaRepository.findBySlug(anotacao.empresa())
                    .orElseThrow(() -> new IllegalStateException("Empresa de teste não encontrada: " + anotacao.empresa()));
            empresaId = empresa.getId();
            slug = empresa.getSlug();
        }
        UUID usuarioId = (empresaId == null
                ? usuarioRepository.findByEmpresaIdIsNullAndUsername(anotacao.username())
                : usuarioRepository.findByEmpresaIdAndUsername(empresaId, anotacao.username()))
                .map(UsuarioSistemaEntity::getId)
                .orElse(null);
        UsuarioAutenticado principal = new UsuarioAutenticado(usuarioId, anotacao.username(), "senha", role, empresaId, slug);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        return context;
    }
}
