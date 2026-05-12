package com.fisiogestao.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração global da documentação OpenAPI 3 (Swagger UI).
 *
 * <p>Acessível em {@code /swagger-ui.html} sem autenticação (rota liberada no SecurityConfig).
 * As operações que exigem autenticação estão marcadas com {@code @SecurityRequirement(name = "basicAuth")}
 * e utilizam HTTP Basic como mecanismo de autenticação para facilitar testes diretos pela UI.</p>
 *
 * <p>Em produção, os endpoints de API também aceitam autenticação via sessão (form login).
 * Para adicionar suporte a HTTP Basic como alternativa de autenticação nas rotas de API,
 * ative {@code httpBasic()} no {@link SecurityConfig}.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fisioGestaoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FisioGestão API")
                        .description("""
                                API REST do **FisioGestão** — sistema de gestão para clínicas de fisioterapia.

                                ## Autenticação

                                Endpoints protegidos requerem autenticação. Na Swagger UI, utilize o botão
                                **Authorize** e informe as credenciais de acesso (usuário e senha da plataforma).

                                ## Rotas públicas

                                - `GET /api/slots` — consulta de horários disponíveis (agendamento online, sem auth)
                                """)
                        .version("1.0")
                        .contact(new Contact()
                                .name("FisioGestão")
                                .email("contato@fisiogestao.com")))
                .components(new Components()
                        .addSecuritySchemes("basicAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("Credenciais de acesso ao sistema (usuário e senha)")));
    }
}
