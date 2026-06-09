package com.fisiogestao.config;

import com.fisiogestao.auth.InMemoryUserCatalog;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
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

    private static final String DEFAULT_REMEMBER_ME_KEY = "fisiogestao-default-remember-me-key-troque-em-producao";

    /**
     * Chave usada para assinar tokens "lembre-me". Configure via APP_SECURITY_REMEMBER_ME_KEY.
     * Valor padrao fixo garante que tokens sobrevivam a restarts, mas o ideal e definir
     * um valor aleatorio longo em producao.
     */
    @Value("${app.security.remember-me-key:" + DEFAULT_REMEMBER_ME_KEY + "}")
    private String rememberMeKey;

    @Value("${app.base-url:http://localhost:8081}")
    private String appBaseUrl;

    private final Environment environment;

    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validarRememberMeKey() {
        if (!DEFAULT_REMEMBER_ME_KEY.equals(rememberMeKey) || !isRuntimeProducao()) {
            return;
        }
        throw new IllegalStateException(
                "Configure APP_SECURITY_REMEMBER_ME_KEY com um valor aleatorio longo antes de iniciar em producao."
        );
    }

    private boolean isRuntimeProducao() {
        if (environment.matchesProfiles("prod", "production")) {
            return true;
        }
        String render = environment.getProperty("RENDER");
        if ("true".equalsIgnoreCase(render)) {
            return true;
        }
        return appBaseUrl != null
                && !appBaseUrl.contains("localhost")
                && !appBaseUrl.contains("127.0.0.1");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuditAuthenticationSuccessHandler auditAuthenticationSuccessHandler,
            AuditLogoutHandler auditLogoutHandler
    ) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // ── Recursos públicos estáticos e páginas sem autenticação ──────
                        .requestMatchers(
                                "/",
                                "/home",
                                "/login",
                                "/esqueci-minha-senha",
                                "/resetar-senha",
                                "/error",
                                "/agendamento-online",
                                "/agendamento-online/sucesso",
                                "/lista-espera-publica",
                                "/lista-espera-publica/sucesso",
                                "/paciente/**",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/css/**",
                                "/js/**",
                                "/favicon.svg"
                        ).permitAll()
                        // ── API pública: slots de horário para o formulário de agendamento ──
                        // Não requer login — serve a visitantes anônimos.
                        .requestMatchers("/api/slots").permitAll()
                        // ── Swagger — apenas ADMIN tem visibilidade da documentação ─────────
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").hasRole("ADMIN")
                        // ── Administração do sistema: acesso fino controlado por
                        // UserMenuAccessInterceptor / UserMenuAccessService.podeAcessar(),
                        // que respeita as permissões configuradas em Funções de Usuários ──
                        .requestMatchers(
                                "/configuracoes/**",
                                "/configuracao/**",
                                "/admin/**",
                                "/usuarios/**",
                                "/funcoes-usuarios/**"
                        ).authenticated()
                        // ── APIs de dados financeiros: restritas a ADMIN e FINANCEIRO ────────
                        // Contêm receita agregada, ocupação e projeções — dados sensíveis.
                        .requestMatchers("/api/dashboard/**", "/api/financeiro/**").hasAnyRole("ADMIN", "FINANCEIRO")
                        // ── APIs internas autenticadas ────────────────────────────────────────
                        // Qualquer role válida pode acessar. Explicitado para blindar contra
                        // mudanças acidentais no anyRequest() mais abaixo.
                        .requestMatchers(
                                "/api/agendamentos/**",
                                "/api/pacientes/**",
                                "/api/solicitacoes/**",
                                "/api/calendario/**",
                                "/api/relatorios/**"
                        ).authenticated()
                        // ── Tudo o mais exige autenticação ────────────────────────────────────
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
                        // ── Content-Security-Policy ──────────────────────────────────────
                        // connect-src: permite fetch para IBGE (cidades) e ViaCEP (CEP)
                        //   usados na página pública de agendamento online.
                        // font-src: Google Fonts é carregado via fonts.gstatic.com.
                        // style-src: Google Fonts stylesheet vem de fonts.googleapis.com;
                        //   'unsafe-inline' é necessário enquanto houver inline styles residuais.
                        //   Remover 'unsafe-inline' de style-src quando todo CSS estiver externalizado.
                        // script-src: 'unsafe-inline' cobre scripts <script> inline no Thymeleaf
                        //   (ex: lucide.createIcons()). Migrar para nonce quando possível.
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline'; " +
                                        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                                        "font-src 'self' https://fonts.gstatic.com; " +
                                        "img-src 'self' data:; " +
                                        "connect-src 'self' " +
                                            "https://servicodados.ibge.gov.br " +
                                            "https://viacep.com.br; " +
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
