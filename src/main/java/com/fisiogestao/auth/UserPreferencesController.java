package com.fisiogestao.auth;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;

@Controller
@RequiredArgsConstructor
public class UserPreferencesController {

    private final InMemoryUserCatalog userCatalog;

    @PostMapping("/preferencias/tema")
    public String atualizarTema(
            @RequestParam("tema") UserThemePreference tema,
            Authentication authentication,
            HttpServletRequest request
    ) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        userCatalog.atualizarTema(authentication.getName(), tema);
        return "redirect:" + resolveRedirectPath(request);
    }

    private String resolveRedirectPath(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "/dashboard";
        }

        try {
            URI uri = URI.create(referer);
            if (referer.startsWith("//")) {
                return "/dashboard";
            }

            if (uri.isAbsolute() && !isSameOrigin(uri, request)) {
                return "/dashboard";
            }

            String path = uri.getRawPath();
            if (path == null || path.isBlank()) {
                return "/dashboard";
            }

            String query = uri.getRawQuery();
            return query == null || query.isBlank() ? path : path + "?" + query;
        } catch (IllegalArgumentException ex) {
            return "/dashboard";
        }
    }

    private boolean isSameOrigin(URI uri, HttpServletRequest request) {
        if (uri.getHost() == null || uri.getScheme() == null) {
            return true;
        }

        int requestPort = normalizePort(request.getServerPort(), request.getScheme());
        int refererPort = normalizePort(uri.getPort(), uri.getScheme());

        return request.getServerName().equalsIgnoreCase(uri.getHost())
                && request.getScheme().equalsIgnoreCase(uri.getScheme())
                && requestPort == refererPort;
    }

    private int normalizePort(int port, String scheme) {
        if (port >= 0) {
            return port;
        }
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
    }
}
