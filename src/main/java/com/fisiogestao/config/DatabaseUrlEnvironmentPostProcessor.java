package com.fisiogestao.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "renderDatabaseUrlCompatibility";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (hasDatasourceUrlConfigured(environment)) {
            return;
        }

        String databaseUrl = trimToNull(environment.getProperty("DATABASE_URL"));
        if (!StringUtils.hasText(databaseUrl)) {
            return;
        }

        ParsedDatabaseUrl parsed = parseDatabaseUrl(databaseUrl);
        if (parsed == null) {
            return;
        }

        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put("SPRING_DATASOURCE_URL", parsed.jdbcUrl());
        overrides.put("spring.datasource.url", parsed.jdbcUrl());

        if (!hasText(environment.getProperty("SPRING_DATASOURCE_USERNAME")) && StringUtils.hasText(parsed.username())) {
            overrides.put("SPRING_DATASOURCE_USERNAME", parsed.username());
            overrides.put("spring.datasource.username", parsed.username());
        }

        if (!hasText(environment.getProperty("SPRING_DATASOURCE_PASSWORD")) && parsed.password() != null) {
            overrides.put("SPRING_DATASOURCE_PASSWORD", parsed.password());
            overrides.put("spring.datasource.password", parsed.password());
        }

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, overrides));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private static boolean hasDatasourceUrlConfigured(ConfigurableEnvironment environment) {
        return hasText(environment.getProperty("SPRING_DATASOURCE_URL"))
                || hasText(environment.getProperty("spring.datasource.url"));
    }

    private static boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static ParsedDatabaseUrl parseDatabaseUrl(String rawUrl) {
        try {
            String normalized = rawUrl.startsWith("jdbc:") ? rawUrl.substring("jdbc:".length()) : rawUrl;
            URI uri = URI.create(normalized);
            String scheme = uri.getScheme();

            if (!"postgres".equalsIgnoreCase(scheme) && !"postgresql".equalsIgnoreCase(scheme)) {
                return null;
            }

            String host = uri.getHost();
            if (!StringUtils.hasText(host)) {
                return null;
            }

            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getRawPath();
            if (!StringUtils.hasText(path) || "/".equals(path)) {
                return null;
            }

            String database = decode(path.startsWith("/") ? path.substring(1) : path);
            if (!StringUtils.hasText(database)) {
                return null;
            }

            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;
            if (StringUtils.hasText(uri.getRawQuery())) {
                jdbcUrl = jdbcUrl + "?" + uri.getRawQuery();
            }

            String username = null;
            String password = null;
            String userInfo = uri.getRawUserInfo();
            if (StringUtils.hasText(userInfo)) {
                String[] parts = userInfo.split(":", 2);
                username = decode(parts[0]);
                password = parts.length > 1 ? decode(parts[1]) : null;
            }

            return new ParsedDatabaseUrl(jdbcUrl, username, password);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record ParsedDatabaseUrl(String jdbcUrl, String username, String password) {
    }
}
