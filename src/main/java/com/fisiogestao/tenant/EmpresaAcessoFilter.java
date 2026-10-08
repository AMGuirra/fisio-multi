package com.fisiogestao.tenant;

import com.fisiogestao.auth.UsuarioAutenticado;
import com.fisiogestao.empresa.EmpresaAcessoService;
import com.fisiogestao.empresa.MotivoAcessoNegado;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * Revalida, em toda requisição autenticada, se a clínica do usuário ainda pode acessar
 * o sistema. Se a empresa foi bloqueada, inativada ou o teste expirou, a sessão existente
 * é invalidada imediatamente e o usuário é levado à tela de acesso negado.
 */
public class EmpresaAcessoFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmpresaAcessoFilter.class);
    private static final Set<String> PREFIXOS_LIVRES = Set.of(
            "/css/", "/js/", "/favicon.svg", "/acesso-negado", "/suporte/", "/error", "/actuator/health"
    );

    private final EmpresaAcessoService empresaAcessoService;
    private final String rememberMeCookie;

    public EmpresaAcessoFilter(EmpresaAcessoService empresaAcessoService, String rememberMeCookie) {
        this.empresaAcessoService = empresaAcessoService;
        this.rememberMeCookie = rememberMeCookie;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String uri = request.getRequestURI();
        return PREFIXOS_LIVRES.stream().anyMatch(uri::startsWith);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        Optional<UsuarioAutenticado> usuario = TenantContext.usuarioAutenticado();
        if (usuario.isEmpty() || usuario.get().isSuperAdmin()) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<MotivoAcessoNegado> negado = empresaAcessoService.avaliar(usuario.get().getEmpresaId());
        if (negado.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        LOGGER.info("Sessão encerrada: empresa {} sem acesso ({}).", usuario.get().getEmpresaId(), negado.get());
        encerrarSessao(request, response);
        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"erro\":\"" + negado.get().getMensagem() + "\"}");
            return;
        }
        response.sendRedirect(request.getContextPath() + "/acesso-negado?motivo=" + negado.get().name());
    }

    private void encerrarSessao(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        Cookie cookie = new Cookie(rememberMeCookie, null);
        cookie.setMaxAge(0);
        cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        response.addCookie(cookie);
    }
}
