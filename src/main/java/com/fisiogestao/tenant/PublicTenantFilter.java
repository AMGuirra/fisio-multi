package com.fisiogestao.tenant;

import com.fisiogestao.empresa.EmpresaAcessoService;
import com.fisiogestao.empresa.EmpresaEntity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Abre o escopo de tenant das rotas públicas (agendamento online, portal do paciente,
 * horários disponíveis). A empresa vem do slug {@code ?clinica=} ou de um token já
 * vinculado a ela; clínicas sem acesso liberado não expõem páginas públicas.
 */
public class PublicTenantFilter extends OncePerRequestFilter {

    public static final String ATRIBUTO_EMPRESA_PUBLICA = "empresaPublica";
    public static final String PARAM_CLINICA = "clinica";

    private final EmpresaPublicaResolver resolver;

    public PublicTenantFilter(EmpresaPublicaResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return tipoRota(caminho(request)) == TipoRota.NENHUMA;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String caminho = caminho(request);
        Optional<EmpresaEntity> empresa = switch (tipoRota(caminho)) {
            case TOKEN_PORTAL -> resolver.porTokenPortal(tokenPortal(caminho));
            case RESET_PORTAL -> request.getParameter("token") != null
                    ? resolver.porTokenResetPaciente(request.getParameter("token"))
                    : resolver.porSlugOuPadrao(request.getParameter(PARAM_CLINICA));
            case SLUG -> resolver.porSlugOuPadrao(request.getParameter(PARAM_CLINICA));
            case NENHUMA -> Optional.empty();
        };

        if (empresa.isEmpty()) {
            if (tipoRota(caminho) == TipoRota.TOKEN_PORTAL || tipoRota(caminho) == TipoRota.RESET_PORTAL) {
                // Token inexistente: as telas do portal já tratam como "link inválido" sem dados.
                TenantContext.Escopo anterior = TenantContext.abrir(TenantContext.NENHUMA_EMPRESA);
                try {
                    filterChain.doFilter(request, response);
                } finally {
                    TenantContext.fechar(anterior);
                }
                return;
            }
            negar(request, response, "nao-encontrada");
            return;
        }

        EmpresaEntity encontrada = empresa.get();
        if (EmpresaAcessoService.avaliar(encontrada, OffsetDateTime.now()).isPresent()) {
            negar(request, response, "indisponivel");
            return;
        }

        request.setAttribute(ATRIBUTO_EMPRESA_PUBLICA, new EmpresaPublica(encontrada.getId(), encontrada.getSlug(), encontrada.getNome()));
        TenantContext.Escopo anterior = TenantContext.abrir(encontrada.getId());
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.fechar(anterior);
        }
    }

    private void negar(HttpServletRequest request, HttpServletResponse response, String motivo) throws IOException {
        if (caminho(request).startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"erro\":\"Clínica não encontrada ou indisponível.\"}");
            return;
        }
        response.sendRedirect(request.getContextPath() + "/clinica-indisponivel?motivo=" + motivo);
    }

    static TipoRota tipoRota(String caminho) {
        if (caminho.equals("/agendamento-online") || caminho.startsWith("/agendamento-online/")
                || caminho.equals("/lista-espera-publica") || caminho.startsWith("/lista-espera-publica/")
                || caminho.equals("/api/slots")
                || caminho.equals("/paciente/login")
                || caminho.equals("/paciente/esqueci-minha-senha")) {
            return TipoRota.SLUG;
        }
        if (caminho.equals("/paciente/resetar-senha")) {
            return TipoRota.RESET_PORTAL;
        }
        if (caminho.startsWith("/paciente/") && tokenPortal(caminho) != null) {
            return TipoRota.TOKEN_PORTAL;
        }
        return TipoRota.NENHUMA;
    }

    static UUID tokenPortal(String caminho) {
        String resto = caminho.substring("/paciente/".length());
        int barra = resto.indexOf('/');
        String segmento = barra >= 0 ? resto.substring(0, barra) : resto;
        try {
            return UUID.fromString(segmento);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String caminho(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contexto = request.getContextPath();
        return contexto != null && !contexto.isEmpty() && uri.startsWith(contexto) ? uri.substring(contexto.length()) : uri;
    }

    /**
     * Sufixo {@code ?clinica=slug} da clínica pública resolvida nesta requisição, para manter a
     * empresa em redirecionamentos de páginas públicas. Vazio fora de rotas públicas.
     */
    public static String parametroClinica() {
        var atributos = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        Object publica = atributos == null ? null
                : atributos.getAttribute(ATRIBUTO_EMPRESA_PUBLICA, org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
        return publica instanceof EmpresaPublica empresa ? "?clinica=" + empresa.slug() : "";
    }

    enum TipoRota { SLUG, TOKEN_PORTAL, RESET_PORTAL, NENHUMA }

    /** Dados públicos mínimos da clínica disponíveis para os templates. */
    public record EmpresaPublica(UUID id, String slug, String nome) {
    }
}
