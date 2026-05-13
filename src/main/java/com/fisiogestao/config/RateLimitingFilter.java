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
 * Filtro de rate limiting para rotas publicas e de autenticacao sensiveis.
 *
 * <p>Estrategia: Bucket4j in-memory por (IP + rota), com limites e janelas
 * distintos por rota e expiracao por inatividade para evitar crescimento
 * indefinido do cache.</p>
 *
 * <ul>
 *   <li>/login — 10 tentativas em 15 minutos; resposta: redirect com parametro de erro</li>
 *   <li>/esqueci-minha-senha — 5 tentativas por minuto; resposta: JSON 429</li>
 *   <li>/agendamento-online — 5 tentativas por minuto; resposta: JSON 429</li>
 * </ul>
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static final String HEADER_RETRY_AFTER = "Retry-After";
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    /**
     * Configuracao de limite por rota.
     *
     * @param prefixo           prefixo da URI (POST only)
     * @param limite            numero maximo de requisicoes na janela
     * @param janela            duracao da janela de contagem
     * @param redirecionarLogin true = redireciona para /login?bloqueado (formularios web);
     *                          false = retorna JSON 429 (endpoints publicos de API/form)
     */
    enum RotaProtegida {
        LOGIN("/login", 10, Duration.ofMinutes(15), true),
        ESQUECI_SENHA("/esqueci-minha-senha", 5, Duration.ofMinutes(1), false),
        AGENDAMENTO_ONLINE("/agendamento-online", 5, Duration.ofMinutes(1), false);

        final String prefixo;
        final int limite;
        final Duration janela;
        final boolean redirecionarLogin;

        RotaProtegida(String prefixo, int limite, Duration janela, boolean redirecionarLogin) {
            this.prefixo = prefixo;
            this.limite = limite;
            this.janela = janela;
            this.redirecionarLogin = redirecionarLogin;
        }
    }

    private final ConcurrentHashMap<String, BucketEntry> buckets = new ConcurrentHashMap<>();
    private final long bucketTtlNanos;
    private final long cleanupIntervalNanos;
    private final LongSupplier ticker;
    private volatile long ultimaLimpezaNanos;

    public RateLimitingFilter() {
        this(Duration.ofMinutes(20), Duration.ofMinutes(1), System::nanoTime);
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

        RotaProtegida rota = resolverRota(request);
        if (rota == null) {
            filterChain.doFilter(request, response);
            return;
        }

        long agora = ticker.getAsLong();
        limparBucketsExpiradosSeNecessario(agora);

        String ip = extrairIp(request);
        // chave composta: evita que o limite de uma rota consuma o bucket de outra
        String bucketKey = ip + ":" + rota.name();
        Bucket bucket = bucketPara(bucketKey, rota, agora);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            long retryAfterSeconds = rota.janela.toSeconds();
            LOGGER.warn("Rate limit excedido: ip={} rota={}", ip, request.getRequestURI());

            if (rota.redirecionarLogin) {
                response.sendRedirect("/login?bloqueado");
            } else {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader(HEADER_RETRY_AFTER, String.valueOf(retryAfterSeconds));
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"erro\":\"Muitas tentativas. Aguarde antes de tentar novamente.\"}"
                );
            }
        }
    }

    private Bucket bucketPara(String key, RotaProtegida rota, long agora) {
        return buckets.compute(key, (k, entry) -> {
            if (entry == null || expirado(entry, agora)) {
                return new BucketEntry(criarBucket(rota), agora);
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

    private Bucket criarBucket(RotaProtegida rota) {
        Refill refill = Refill.greedy(rota.limite, rota.janela);
        Bandwidth limit = Bandwidth.classic(rota.limite, refill);
        return Bucket.builder().addLimit(limit).build();
    }

    /** Retorna a configuracao de rota se a requisicao deve ser limitada, ou null caso contrario. */
    RotaProtegida resolverRota(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        String uri = request.getRequestURI();
        for (RotaProtegida rota : RotaProtegida.values()) {
            if (uri.startsWith(rota.prefixo)) {
                return rota;
            }
        }
        return null;
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
