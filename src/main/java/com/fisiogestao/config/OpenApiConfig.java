package com.fisiogestao.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracao global da documentacao OpenAPI 3 (Swagger UI).
 *
 * <p>Acessivel em {@code /swagger-ui.html} apenas para usuarios administradores.
 * As operacoes que exigem autenticacao estao marcadas com {@code @SecurityRequirement(name = "basicAuth")}
 * e utilizam HTTP Basic como mecanismo de autenticacao para facilitar testes diretos pela UI.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fisioGestaoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FisioGestao API")
                        .description("""
                                API REST do **FisioGestao** - sistema de gestao para clinicas de fisioterapia.

                                ## Autenticacao

                                Endpoints protegidos requerem autenticacao. Na Swagger UI, utilize o botao
                                **Authorize** e informe as credenciais de acesso (usuario e senha da plataforma).

                                ## Rotas publicas

                                - `GET /api/slots` - consulta de horarios disponiveis (agendamento online, sem auth)
                                """)
                        .version("1.0")
                        .contact(new Contact()
                                .name("FisioGestao")
                                .email("contato@fisiogestao.com")))
                .components(new Components()
                        .addSecuritySchemes("basicAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("Credenciais de acesso ao sistema (usuario e senha)")));
    }
}
