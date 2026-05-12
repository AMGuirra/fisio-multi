package com.fisiogestao.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Filtro de rate limiting para rotas publicas sensiveis.
 *
 * <p>Estrategia: Bucket4j in-memory por IP, com expiracao por inatividade para
 * evitar crescimento indefinido do cache em ambientes expostos a trafego publico.</p>
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static final int LIMITE_POR_MINUTO = 5;
    private static final String HEADER_RETRY_AFTER = "Retry-After";
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    private final ConcurrentHashMap<String, BucketEntry> buckets = new ConcurrentHashMap<>();
    private final long bucketTtlNanos;
    private final long cleanupIntervalNanos;
    private final LongSupplier ticker;
    private volatile long ultimaLimpezaNanos;

    public RateLimitingFilter() {
        this(Duration.ofMinutes(10), Duration.ofMinutes(1), System::nanoTime);
    }

    RateLimitingFilter(Duration bucketTtl, Duration cleanupInterval, LongSupplier ticker) {
        this.bucketTtlNanos = bucketTtl.toNanos();
        this.cleanupIntervalNanos = cleanupInterval.toNanos();
        this.ticker = ticker;
        this.ultimaLimpezaNanos = ticker.getAsLong();
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (!isRotaProtegida(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        long agora = ticker.getAsLong();
        limparBucketsExpiradosSeNecessario(agora);

        String ip = extrairIp(request);
        Bucket bucket = bucketPara(ip, agora);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            LOGGER.warn("Rate limit excedido: ip={} rota={}", ip, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HEADER_RETRY_AFTER, "60");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"erro\":\"Muitas tentativas. Aguarde um minuto antes de tentar novamente.\"}"
            );
        }
    }

    private Bucket bucketPara(String ip, long agora) {
        return buckets.compute(ip, (key, entry) -> {
            if (entry == null || expirado(entry, agora)) {
                return new BucketEntry(criarBucket(), agora);
            }
            return entry.atualizarUltimoAcesso(agora);
        }).bucket();
    }

    private void limparBucketsExpiradosSeNecessario(long agora) {
        if (agora - ultimaLimpezaNanos < cleanupIntervalNanos) {
            return;
        }

        ultimaLimpezaNanos = agora;
        buckets.entrySet().removeIf(entry -> expirado(entry.getValue(), agora));
    }

    private boolean expirado(BucketEntry entry, long agora) {
        return agora - entry.ultimoAcessoNanos() > bucketTtlNanos;
    }

    private Bucket criarBucket() {
        Refill refill = Refill.greedy(LIMITE_POR_MINUTO, Duration.ofMinutes(1));
        Bandwidth limit = Bandwidth.classic(LIMITE_POR_MINUTO, refill);
        return Bucket.builder().addLimit(limit).build();
    }

    private boolean isRotaProtegida(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        return uri.startsWith("/esqueci-minha-senha") || uri.startsWith("/agendamento-online");
    }

    private String extrairIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String primeiroIp = xForwardedFor.split(",")[0].strip();
            if (!primeiroIp.isEmpty()) {
                return primeiroIp;
            }
        }
        return request.getRemoteAddr();
    }

    int bucketCacheSize() {
        return buckets.size();
    }

    private record BucketEntry(Bucket bucket, long ultimoAcessoNanos) {

        private BucketEntry atualizarUltimoAcesso(long agora) {
            return new BucketEntry(bucket, agora);
        }
    }
}
