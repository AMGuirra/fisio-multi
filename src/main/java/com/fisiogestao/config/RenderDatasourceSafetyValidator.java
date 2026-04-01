package com.fisiogestao.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class RenderDatasourceSafetyValidator implements ApplicationRunner {

    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        if (!isRenderEnvironment()) {
            return;
        }

        boolean possuiDatasourceExplicito = hasEnvironmentVariable("DATABASE_URL")
                || hasEnvironmentVariable("SPRING_DATASOURCE_URL")
                || hasEnvironmentVariable("DB_HOST");

        String datasourceUrl = trimToNull(environment.getProperty("spring.datasource.url"));
        if (!possuiDatasourceExplicito) {
            throw new IllegalStateException("""
                    Deploy inseguro bloqueado: o serviço está subindo no Render sem uma configuração explícita de banco.
                    Defina DATABASE_URL ou SPRING_DATASOURCE_URL/SPRING_DATASOURCE_USERNAME/SPRING_DATASOURCE_PASSWORD
                    no serviço do Render antes de publicar uma nova versão.
                    """.trim());
        }

        if (datasourceUrl != null && (datasourceUrl.contains("localhost") || datasourceUrl.contains("127.0.0.1"))) {
            throw new IllegalStateException("""
                    Deploy inseguro bloqueado: a aplicação está apontando para um banco local em ambiente Render.
                    Ajuste DATABASE_URL ou SPRING_DATASOURCE_URL para o PostgreSQL persistente do serviço.
                    """.trim());
        }
    }

    private boolean isRenderEnvironment() {
        return hasEnvironmentVariable("RENDER")
                || hasEnvironmentVariable("RENDER_EXTERNAL_HOSTNAME")
                || containsOnRenderDomain(environment.getProperty("app.base-url"))
                || containsOnRenderDomain(System.getenv("APP_BASE_URL"));
    }

    private boolean containsOnRenderDomain(String value) {
        String normalized = trimToNull(value);
        return normalized != null && normalized.contains(".onrender.com");
    }

    private boolean hasEnvironmentVariable(String key) {
        return StringUtils.hasText(System.getenv(key));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
