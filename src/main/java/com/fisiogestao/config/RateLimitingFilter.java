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

/**
 * Filtro de rate limiting para rotas públicas sensíveis.
 *
 * <p>Estratégia: Bucket4j in-memory com janela deslizante por IP.
 * O IP é extraído do header {@code X-Forwarded-For} (necessário no Render, que opera
 * atrás de reverse proxy), com fallback para {@code getRemoteAddr()}.</p>
 *
 * <p>Limites configurados:
 * <ul>
 *   <li>POST {@code /esqueci-minha-senha}: 5 requisições por minuto por IP</li>
 *   <li>POST {@code /agendamento-online}: 5 requisições por minuto por IP</li>
 * </ul>
 * </p>
 *
 * <p>Quando o limite é excedido, retorna HTTP 429 com header {@code Retry-After: 60}.</p>
 *
 * <p>Nota de escala: esta implementação é in-memory e não sobrevive a múltiplas instâncias.
 * Para scale-out horizontal, substituir o {@code ConcurrentHashMap} por Bucket4j com
 * backend Redis ou Hazelcast.</p>
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static final int LIMITE_POR_MINUTO = 5;
    private static final String HEADER_RETRY_AFTER = "Retry-After";
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    /** Cache de buckets por IP. Cresce com o tráfego mas se descarta automaticamente ao reiniciar. */
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

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

        String ip = extrairIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, this::criarBucket);

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

    /**
     * Cria um Bucket com limite de {@value #LIMITE_POR_MINUTO} tokens por minuto,
     * reabastecido gradualmente (greedy refill) para evitar burst no início de cada janela.
     */
    private Bucket criarBucket(String ip) {
        Refill refill = Refill.greedy(LIMITE_POR_MINUTO, Duration.ofMinutes(1));
        Bandwidth limit = Bandwidth.classic(LIMITE_POR_MINUTO, refill);
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Verifica se a rota e o método HTTP estão sujeitos ao rate limiting.
     */
    private boolean isRotaProtegida(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        return uri.startsWith("/esqueci-minha-senha") || uri.startsWith("/agendamento-online");
    }

    /**
     * Extrai o IP real do cliente respeitando o header {@code X-Forwarded-For} do Render.
     *
     * <p>Usa apenas o primeiro IP da cadeia para evitar spoofing via múltiplos proxies.
     * Em produção no Render, o primeiro IP da lista é sempre o do cliente real.</p>
     */
    private String extrairIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Pega só o primeiro IP da lista (cliente original), ignora os intermediários
            String primeiroIp = xForwardedFor.split(",")[0].strip();
            if (!primeiroIp.isEmpty()) {
                return primeiroIp;
            }
        }
        return request.getRemoteAddr();
    }
}
