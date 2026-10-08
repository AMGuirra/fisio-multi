package com.fisiogestao.support;

import com.fisiogestao.empresa.EmpresaLegada;
import com.fisiogestao.empresa.EmpresaRepository;
import com.fisiogestao.tenant.TenantContext;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

/**
 * Abre o escopo da "Clínica Principal" apenas durante {@code @BeforeEach}/{@code @AfterEach},
 * para que o preparo de dados grave registros na empresa legada. Durante o corpo do teste o
 * escopo é fechado: requisições MockMvc e chamadas de serviço usam somente a empresa do usuário
 * autenticado, exatamente como em produção.
 */
public class EmpresaLegadaSetupExtension
        implements BeforeEachCallback, BeforeTestExecutionCallback, AfterTestExecutionCallback, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(EmpresaLegadaSetupExtension.class);

    @Override
    public void beforeEach(ExtensionContext context) {
        abrir(context);
    }

    /**
     * No corpo do teste, o escopo passa a ser a empresa do usuário autenticado pelo teste (se houver).
     * Isso é necessário porque o Spring Security limpa o SecurityContextHolder ao fim de cada
     * requisição MockMvc; sem isso, as verificações feitas no próprio teste após a requisição
     * ficariam sem empresa. Testes anônimos (rotas públicas) ficam sem escopo algum.
     */
    @Override
    public void beforeTestExecution(ExtensionContext context) {
        fechar(context);
        var autenticacao = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof com.fisiogestao.auth.UsuarioAutenticado usuario
                && usuario.getEmpresaId() != null) {
            context.getStore(NAMESPACE).put("anterior", TenantContext.abrir(usuario.getEmpresaId()));
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        fechar(context);
        abrir(context);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        fechar(context);
    }

    public static UUID empresaLegadaId(ExtensionContext context) {
        return SpringExtension.getApplicationContext(context)
                .getBean(EmpresaRepository.class)
                .findBySlug(EmpresaLegada.SLUG)
                .orElseThrow()
                .getId();
    }

    private void abrir(ExtensionContext context) {
        TenantContext.Escopo anterior = TenantContext.abrir(empresaLegadaId(context));
        context.getStore(NAMESPACE).put("anterior", anterior);
    }

    private void fechar(ExtensionContext context) {
        TenantContext.Escopo anterior = context.getStore(NAMESPACE).remove("anterior", TenantContext.Escopo.class);
        if (anterior != null) {
            TenantContext.fechar(anterior);
        }
    }
}
