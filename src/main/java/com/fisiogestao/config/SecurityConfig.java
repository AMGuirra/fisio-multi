package com.fisiogestao.config;

import com.fisiogestao.auth.InMemoryUserCatalog;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.config.Customizer;
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

@Configuration
public class SecurityConfig {

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
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(auditAuthenticationSuccessHandler)
                        .permitAll()
                )
                .httpBasic(Customizer.withDefaults())
                .logout(logout -> logout
                        .addLogoutHandler(auditLogoutHandler)
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .rememberMe(Customizer.withDefaults())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
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
     */
    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration() {
        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitingFilter());
        registration.addUrlPatterns("/esqueci-minha-senha", "/agendamento-online");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("rateLimitingFilter");
        return registration;
    }
}
