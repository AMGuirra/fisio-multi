package com.fisiogestao.config;

import com.fisiogestao.auth.InMemoryUserCatalog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableMethodSecurity   // habilita @PreAuthorize / @PostAuthorize para proteção em profundidade nos services/controllers
public class SecurityConfig {

    /**
     * Chave usada para assinar tokens "lembre-me". Configure via APP_SECURITY_REMEMBER_ME_KEY.
     * Valor padrao fixo garante que tokens sobrevivam a restarts, mas o ideal e definir
     * um valor aleatorio longo em producao.
     */
    @Value("${app.security.remember-me-key:fisiogestao-default-remember-me-key-troque-em-producao}")
    private String rememberMeKey;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuditAuthenticationSuccessHandler auditAuthenticationSuccessHandler,
            AuditLogoutHandler auditLogoutHandler
    ) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/esqueci-minha-senha",
                                "/resetar-senha",
                                "/error",
                                "/agendamento-online",
                                "/agendamento-online/sucesso",
                                "/api/slots",
                                "/paciente/**",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/css/**",
                                "/js/**"
                        ).permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").hasRole("ADMIN")
                        .requestMatchers("/configuracoes/clinica", "/configuracao/clinica").hasRole("ADMIN")
                        .requestMatchers("/admin/system-status").hasRole("ADMIN")
                        .requestMatchers("/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/funcoes-usuarios/**").hasRole("ADMIN")
                        // Métricas do dashboard contêm dados financeiros sensíveis (receita 12 meses, ocupação).
                        // Restritas a ADMIN e FINANCEIRO; demais roles operacionais não requerem visibilidade agregada.
                        .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "FINANCEIRO")
                        // Endpoints internos de agenda, pacientes e solicitações: qualquer usuário autenticado.
                        // Explicitado aqui para blindar contra mudança futura no anyRequest().
                        .requestMatchers("/api/agendamentos/**", "/api/pacientes/**", "/api/solicitacoes/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(auditAuthenticationSuccessHandler)
                        .permitAll()
                )
                // HTTP Basic desabilitado: nao ha uso em app SSR com Thymeleaf e expoe vetor extra
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout
                        .addLogoutHandler(auditLogoutHandler)
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        // Chave injetada via @Value — configure APP_SECURITY_REMEMBER_ME_KEY em producao
                        .key(rememberMeKey)
                        .tokenValiditySeconds(7 * 24 * 60 * 60) // 7 dias
                )
                // Gestao de sessao: um usuario, uma sessao ativa por vez
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .expiredUrl("/login?expirado")
                )
                .headers(headers -> headers
                        // Impede clickjacking (apenas same-origin pode embutir em iframe)
                        .frameOptions(frame -> frame.sameOrigin())
                        // Impede sniffing de content-type pelo navegador
                        .contentTypeOptions(ct -> {})
                        // HSTS: forca HTTPS por 1 ano, incluindo subdomains
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        // Referrer-Policy: nao vaza URL de origem em requests cross-origin
                        .referrerPolicy(rp -> rp
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )
                        // Content-Security-Policy: restringe fontes de scripts, estilos e frames
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline'; " +
                                        "style-src 'self' 'unsafe-inline'; " +
                                        "img-src 'self' data:; " +
                                        "font-src 'self'; " +
                                        "frame-ancestors 'self'; " +
                                        "form-action 'self'"
                                )
                        )
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler((request, response, denied) -> {
                            if (denied instanceof InvalidCsrfTokenException || denied instanceof MissingCsrfTokenException) {
                                response.sendRedirect(request.getContextPath() + "/login?expired");
                            } else {
                                response.sendError(403);
                            }
                        })
                );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(InMemoryUserCatalog userCatalog) {
        return username -> userCatalog.buscarCredenciais(username)
                .map(user -> User.withUsername(user.username())
                        .password(user.passwordHash())
                        .roles(user.role().name())
                        .build())
                .map(UserDetails.class::cast)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado."));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Registra o filtro de rate limiting com prioridade maxima para garantir
     * que ele atue antes dos filtros do Spring Security.
     *
     * <p>Rotas cobertas (POST only):</p>
     * <ul>
     *   <li>/login — 10 tentativas em 15 min, redirect para /login?bloqueado</li>
     *   <li>/esqueci-minha-senha — 5 por minuto, JSON 429</li>
     *   <li>/agendamento-online — 5 por minuto, JSON 429</li>
     * </ul>
     */
    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration() {
        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitingFilter());
        registration.addUrlPatterns("/login", "/esqueci-minha-senha", "/agendamento-online");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("rateLimitingFilter");
        return registration;
    }

    /**
     * Necessario para que o controle de sessoes simultaneas (maximumSessions)
     * funcione corretamente com o Spring Security.
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
