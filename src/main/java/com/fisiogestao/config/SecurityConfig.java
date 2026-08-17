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
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import java.net.URI;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

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

    @PostConstruct
    void validarAppBaseUrl() {
        if (!isRuntimeProducao()) {
            return;
        }
        try {
            URI baseUri = URI.create(appBaseUrl);
            String scheme = baseUri.getScheme();
            String host = baseUri.getHost();
            String normalizedHost = host == null ? null : host.toLowerCase(java.util.Locale.ROOT);
            boolean esquemaSeguro = "https".equalsIgnoreCase(scheme);
            boolean hostPublico = normalizedHost != null
                    && !"localhost".equals(normalizedHost)
                    && !normalizedHost.endsWith(".localhost")
                    && !normalizedHost.startsWith("127.")
                    && !"::1".equals(normalizedHost)
                    && !isIpv4PrivadoOuLocal(normalizedHost)
                    && !isIpv6PrivadoOuLocal(normalizedHost);
            if (!baseUri.isAbsolute() || !esquemaSeguro || !hostPublico
                    || baseUri.getUserInfo() != null || baseUri.getQuery() != null || baseUri.getFragment() != null) {
                throw new IllegalArgumentException("base-url insegura");
            }
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "Configure APP_BASE_URL com uma URL HTTPS publica e canonica antes de iniciar em producao."
            );
        }
    }

    private boolean isIpv4PrivadoOuLocal(String host) {
        String[] partes = host.split("\\.");
        if (partes.length != 4) {
            return false;
        }
        try {
            int first = Integer.parseInt(partes[0]);
            int second = Integer.parseInt(partes[1]);
            return first == 0 || first == 10 || first == 127
                    || (first == 100 && second >= 64 && second <= 127)
                    || (first == 169 && second == 254)
                    || (first == 172 && second >= 16 && second <= 31)
                    || (first == 192 && second == 168);
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean isIpv6PrivadoOuLocal(String host) {
        if (!host.contains(":")) {
            return false;
        }
        String literal = host.startsWith("[") && host.endsWith("]")
                ? host.substring(1, host.length() - 1)
                : host;
        try {
            InetAddress address = InetAddress.getByName(literal);
            if (!(address instanceof Inet6Address)) {
                return false;
            }
            byte[] bytes = address.getAddress();
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isMulticastAddress()
                    || (bytes[0] & 0xfe) == 0xfc;
        } catch (UnknownHostException ex) {
            return true;
        }
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
                        // "/api/financeiro/**" é reservado preventivamente: nenhum @RestController
                        // usa esse prefixo hoje, mas qualquer endpoint futuro nele já nasce protegido.
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

        http.addFilterBefore(new CspNonceFilter(), HeaderWriterFilter.class);

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
     *   <li>/paciente/login — 10 tentativas em 15 min, JSON 429</li>
     *   <li>/paciente/esqueci-minha-senha — 5 por minuto, JSON 429</li>
     * </ul>
     */
    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration() {
        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitingFilter());
        registration.addUrlPatterns(
                "/login",
                "/esqueci-minha-senha",
                "/agendamento-online",
                "/lista-espera-publica",
                "/paciente/login",
                "/paciente/esqueci-minha-senha"
        );
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
