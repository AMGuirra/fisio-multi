package com.fisiogestao.support;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Autentica o teste com o principal real do sistema ({@code UsuarioAutenticado}), vinculado à
 * empresa informada pelo slug. Substitui {@code @WithMockUser}, cujo principal não carrega empresa
 * e por isso não enxerga nenhum dado de clínica (o isolamento é fail-closed).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithSecurityContext(factory = WithUsuarioClinicaSecurityContextFactory.class)
public @interface WithUsuarioClinica {

    String username() default "admin";

    String[] roles() default {"ADMIN_CLINICA"};

    /** Slug da empresa; vazio para o SUPER_ADMIN. */
    String empresa() default "clinica-principal";
}
