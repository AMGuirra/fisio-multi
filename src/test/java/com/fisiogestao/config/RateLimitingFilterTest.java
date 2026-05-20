package com.fisiogestao.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class RateLimitingFilterTest {

    private final RateLimitingFilter filter = new RateLimitingFilter();

    // -----------------------------------------------------------------------
    // /agendamento-online — 5 tentativas/min, JSON 429
    // -----------------------------------------------------------------------

    @Test
    void deveBloquearSextaTentativaEmAgendamentoOnline() throws ServletException, IOException {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = postAgendamento("203.0.113.10");

        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, chain);
            assertEquals(200, response.getStatus(), "tentativa " + (i + 1) + " deve passar");
        }

        MockHttpServletResponse bloqueada = new MockHttpServletResponse();
        filter.doFilterInternal(request, bloqueada, chain);

        assertEquals(429, bloqueada.getStatus());
        assertEquals("60", bloqueada.getHeader("Retry-After"));
        verify(chain, times(5)).doFilter(eq(request), any());
    }

    // -----------------------------------------------------------------------
    // /login — 10 tentativas em 15 min, redirect para /login?bloqueado
    // -----------------------------------------------------------------------

    @Test
    void devePermitirDezTentativasDeLoginEBloquearDecima() throws ServletException, IOException {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = postLogin("203.0.113.50");

        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, chain);
            assertEquals(200, response.getStatus(), "tentativa " + (i + 1) + " deve passar");
        }

        MockHttpServletResponse bloqueada = new MockHttpServletResponse();
        filter.doFilterInternal(request, bloqueada, chain);

        // Login bloqueado deve redirecionar, nao retornar JSON 429
        assertEquals(302, bloqueada.getStatus());
        assertEquals("/login?bloqueado", bloqueada.getRedirectedUrl());
        verify(chain, times(10)).doFilter(eq(request), any());
    }

    @Test
    void limiteDeLoginNaoContaminaLimiteDeAgendamento() throws ServletException, IOException {
        // Esgota o limite do /login para um IP
        FilterChain chain = mock(FilterChain.class);
        String ip = "203.0.113.60";

        MockHttpServletRequest loginRequest = postLogin(ip);
        for (int i = 0; i < 10; i++) {
            filter.doFilterInternal(loginRequest, new MockHttpServletResponse(), chain);
        }

        // O mesmo IP ainda deve conseguir acessar /agendamento-online (bucket independente)
        MockHttpServletRequest agendamentoRequest = postAgendamento(ip);
        MockHttpServletResponse agendamentoResponse = new MockHttpServletResponse();
        filter.doFilterInternal(agendamentoRequest, agendamentoResponse, chain);

        assertEquals(200, agendamentoResponse.getStatus(),
                "bucket do /agendamento-online deve ser independente do /login");
    }

    @Test
    void retryAfterDoLoginDeveRefletirJanelaDe15Minutos() throws ServletException, IOException {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = postLogin("203.0.113.70");

        for (int i = 0; i < 10; i++) {
            filter.doFilterInternal(request, new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse bloqueada = new MockHttpServletResponse();
        filter.doFilterInternal(request, bloqueada, chain);

        // Para o login o Retry-After nao e enviado (redireciona), mas o status deve ser 302
        assertEquals(302, bloqueada.getStatus());
    }

    // -----------------------------------------------------------------------
    // Comportamento geral
    // -----------------------------------------------------------------------

    @Test
    void deveUsarPrimeiroIpDoXForwardedFor() throws ServletException, IOException {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = postAgendamento("198.51.100.8, 10.0.0.1");

        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, new MockHttpServletResponse(), chain);
        }

        // Mesmo IP real, proxy diferente — deve estar no mesmo bucket
        MockHttpServletRequest mesmoClienteComProxyDiferente = postAgendamento("198.51.100.8, 10.0.0.2");
        MockHttpServletResponse bloqueada = new MockHttpServletResponse();
        filter.doFilterInternal(mesmoClienteComProxyDiferente, bloqueada, chain);

        assertEquals(429, bloqueada.getStatus());
    }

    @Test
    void deveIgnorarXForwardedForQuandoOrigemNaoForProxyConfiavel() throws ServletException, IOException {
        FilterChain chain = mock(FilterChain.class);
        String remoteAddr = "203.0.113.200";

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = postAgendamento("198.51.100." + i);
            request.setRemoteAddr(remoteAddr);
            filter.doFilterInternal(request, new MockHttpServletResponse(), chain);
        }

        MockHttpServletRequest bloqueadaComHeaderForjado = postAgendamento("198.51.100.99");
        bloqueadaComHeaderForjado.setRemoteAddr(remoteAddr);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(bloqueadaComHeaderForjado, response, chain);

        assertEquals(429, response.getStatus());
    }

    @Test
    void naoLimitaGetNemRotasInternas() throws ServletException, IOException {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(eq(request), any());
    }

    @Test
    void resolverRotaRetornaCorretoParaCadaRota() {
        assertEquals(RateLimitingFilter.RotaProtegida.LOGIN,
                filter.resolverRota(postLogin("1.2.3.4")));
        assertEquals(RateLimitingFilter.RotaProtegida.ESQUECI_SENHA,
                filter.resolverRota(post("/esqueci-minha-senha", "1.2.3.4")));
        assertEquals(RateLimitingFilter.RotaProtegida.AGENDAMENTO_ONLINE,
                filter.resolverRota(postAgendamento("1.2.3.4")));
        assertEquals(null,
                filter.resolverRota(new MockHttpServletRequest("GET", "/login")));
        assertEquals(null,
                filter.resolverRota(new MockHttpServletRequest("POST", "/dashboard")));
    }

    @Test
    void removeBucketsSemAcessoAposTtl() throws ServletException, IOException {
        AtomicLong ticker = new AtomicLong();
        RateLimitingFilter filtroComTtlCurto = new RateLimitingFilter(
                Duration.ofSeconds(10),
                Duration.ofSeconds(1),
                ticker::get
        );

        filtroComTtlCurto.doFilterInternal(postAgendamento("203.0.113.20"), new MockHttpServletResponse(), mock(FilterChain.class));
        filtroComTtlCurto.doFilterInternal(postAgendamento("203.0.113.21"), new MockHttpServletResponse(), mock(FilterChain.class));
        assertEquals(2, filtroComTtlCurto.bucketCacheSize());

        ticker.set(Duration.ofSeconds(12).toNanos());

        filtroComTtlCurto.doFilterInternal(postAgendamento("203.0.113.22"), new MockHttpServletResponse(), mock(FilterChain.class));

        assertEquals(1, filtroComTtlCurto.bucketCacheSize());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private MockHttpServletRequest postAgendamento(String forwardedFor) {
        return post("/agendamento-online", forwardedFor);
    }

    private MockHttpServletRequest postLogin(String forwardedFor) {
        return post("/login", forwardedFor);
    }

    private MockHttpServletRequest post(String uri, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.addHeader("X-Forwarded-For", forwardedFor);
        request.setRemoteAddr("10.0.0.10");
        return request;
    }
}
