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

    @Test
    void deveBloquearSextaTentativaNaMesmaRotaEIp() throws ServletException, IOException {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = postPublico("203.0.113.10");

        for (int tentativa = 0; tentativa < 5; tentativa++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, chain);
            assertEquals(200, response.getStatus());
        }

        MockHttpServletResponse bloqueada = new MockHttpServletResponse();
        filter.doFilterInternal(request, bloqueada, chain);

        assertEquals(429, bloqueada.getStatus());
        assertEquals("60", bloqueada.getHeader("Retry-After"));
        verify(chain, times(5)).doFilter(eq(request), any());
    }

    @Test
    void deveUsarPrimeiroIpDoXForwardedFor() throws ServletException, IOException {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = postPublico("198.51.100.8, 10.0.0.1");

        for (int tentativa = 0; tentativa < 5; tentativa++) {
            filter.doFilterInternal(request, new MockHttpServletResponse(), chain);
        }

        MockHttpServletRequest mesmoClienteComProxyDiferente = postPublico("198.51.100.8, 10.0.0.2");
        MockHttpServletResponse bloqueada = new MockHttpServletResponse();
        filter.doFilterInternal(mesmoClienteComProxyDiferente, bloqueada, chain);

        assertEquals(429, bloqueada.getStatus());
    }

    @Test
    void naoLimitaGetNemRotasInternas() throws ServletException, IOException {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(eq(request), any());
    }

    @Test
    void removeBucketsSemAcessoAposTtl() throws ServletException, IOException {
        AtomicLong ticker = new AtomicLong();
        RateLimitingFilter filtroComTtlCurto = new RateLimitingFilter(
                Duration.ofSeconds(10),
                Duration.ofSeconds(1),
                ticker::get
        );

        filtroComTtlCurto.doFilterInternal(postPublico("203.0.113.20"), new MockHttpServletResponse(), mock(FilterChain.class));
        filtroComTtlCurto.doFilterInternal(postPublico("203.0.113.21"), new MockHttpServletResponse(), mock(FilterChain.class));
        assertEquals(2, filtroComTtlCurto.bucketCacheSize());

        ticker.set(Duration.ofSeconds(12).toNanos());

        filtroComTtlCurto.doFilterInternal(postPublico("203.0.113.22"), new MockHttpServletResponse(), mock(FilterChain.class));

        assertEquals(1, filtroComTtlCurto.bucketCacheSize());
    }

    private MockHttpServletRequest postPublico(String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/agendamento-online");
        request.addHeader("X-Forwarded-For", forwardedFor);
        request.setRemoteAddr("10.0.0.10");
        return request;
    }
}
