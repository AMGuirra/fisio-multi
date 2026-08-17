package com.fisiogestao.config;

import com.fisiogestao.FisioGestaoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionSecurityConfigurationTest {

    @Test
    void perfilProdDeveExigirBancoEChavesViaAmbiente() throws Exception {
        String config = Files.readString(Path.of("src/main/resources/application-prod.yml"));

        assertTrue(config.contains("${SPRING_DATASOURCE_URL}"));
        assertTrue(config.contains("${SPRING_DATASOURCE_USERNAME}"));
        assertTrue(config.contains("${SPRING_DATASOURCE_PASSWORD}"));
        assertTrue(config.contains("${APP_ENCRYPTION_KEY}"));
        assertTrue(config.contains("${APP_SECURITY_REMEMBER_ME_KEY}"));
        assertFalse(config.contains(":postgres"));
    }

    @Test
    void blueprintDeProducaoDeveAtivarPerfilProd() throws Exception {
        String render = Files.readString(Path.of("render.yaml"));
        assertTrue(render.contains("SPRING_PROFILES_ACTIVE"));
        assertTrue(render.contains("value: \"prod\""));
    }

    @Test
    void perfilProdSemVariaveisObrigatoriasDeveFalharAoSubirContexto() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addFirst(new MapPropertySource(
                "startupFailureTest", Map.of("logging.level.root", "OFF")));

        SpringApplication application = new SpringApplication(FisioGestaoApplication.class);
        application.setEnvironment(environment);
        application.setAdditionalProfiles("prod");
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);

        RuntimeException failure = assertThrows(RuntimeException.class, application::run);
        String causas = mensagensDasCausas(failure);
        assertTrue(causas.contains("Unable to determine Dialect")
                        || causas.contains("Failed to determine a suitable driver")
                        || causas.contains("Failed to configure a DataSource")
                        || causas.contains("Could not resolve placeholder"),
                "O contexto deve recusar o datasource sem configuracao produtiva valida.");
    }

    @Test
    void producaoDeveRecusarBaseUrlLocalMesmoComChaveForte() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        SecurityConfig config = new SecurityConfig(environment);
        ReflectionTestUtils.setField(config, "rememberMeKey", "[STRONG_KEY_FROM_ENV]");
        ReflectionTestUtils.setField(config, "appBaseUrl", "http://localhost:8081");

        assertThrows(IllegalStateException.class, config::validarAppBaseUrl);
    }

    @Test
    void producaoDeveRecusarIpv6PrivadoOuLinkLocal() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        SecurityConfig config = new SecurityConfig(environment);
        ReflectionTestUtils.setField(config, "rememberMeKey", "[STRONG_KEY_FROM_ENV]");

        ReflectionTestUtils.setField(config, "appBaseUrl", "https://[fc00::1]");
        assertThrows(IllegalStateException.class, config::validarAppBaseUrl);

        ReflectionTestUtils.setField(config, "appBaseUrl", "https://[fe80::1]");
        assertThrows(IllegalStateException.class, config::validarAppBaseUrl);
    }

    private String mensagensDasCausas(Throwable failure) {
        StringBuilder result = new StringBuilder();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current.getMessage() != null) {
                result.append(current.getMessage()).append('\n');
            }
        }
        return result.toString();
    }
}
