package com.fisiogestao.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserMenuAccessInterceptor userMenuAccessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userMenuAccessInterceptor)
                .excludePathPatterns(
                        "/login",
                        "/logout",
                        "/error",
                        "/esqueci-minha-senha",
                        "/resetar-senha",
                        "/preferencias/tema",
                        "/lista-espera-publica",
                        "/lista-espera-publica/**",
                        "/paciente/**",
                        "/agendamento-online",
                        "/agendamento-online/**",
                        "/api/slots",
                        "/actuator/health",
                        "/actuator/health/**",
                        "/css/**",
                        "/js/**",
                        "/favicon.svg",
                        "/cadastro",
                        "/cadastro/**",
                        "/suporte/**",
                        "/acesso-negado",
                        "/clinica-indisponivel"
                );
    }
}
