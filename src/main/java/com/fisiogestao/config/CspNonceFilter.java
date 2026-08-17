package com.fisiogestao.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

public class CspNonceFilter extends OncePerRequestFilter {

    public static final String REQUEST_ATTRIBUTE = "cspNonce";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        byte[] random = new byte[18];
        SECURE_RANDOM.nextBytes(random);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        request.setAttribute(REQUEST_ATTRIBUTE, nonce);
        response.setHeader("Content-Security-Policy", policy(nonce));
        filterChain.doFilter(request, response);
    }

    private String policy(String nonce) {
        return "default-src 'self'; "
                + "script-src 'self' 'nonce-" + nonce + "'; "
                + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                + "font-src 'self' https://fonts.gstatic.com; "
                + "img-src 'self' data: blob:; "
                + "connect-src 'self' https://servicodados.ibge.gov.br https://viacep.com.br; "
                + "object-src 'none'; base-uri 'self'; frame-ancestors 'self'; form-action 'self'";
    }
}
