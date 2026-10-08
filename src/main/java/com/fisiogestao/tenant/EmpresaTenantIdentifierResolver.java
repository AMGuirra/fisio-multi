package com.fisiogestao.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import java.util.UUID;

/**
 * Informa ao Hibernate qual empresa está ativa ao abrir cada sessão. Combinado com
 * {@code @TenantId} em {@link TenantEntity}, o Hibernate filtra automaticamente JPQL,
 * Criteria/Specifications e buscas por chave ({@code findById}), além de preencher
 * {@code empresa_id} nos inserts e rejeitar valores divergentes.
 *
 * <p>Nenhuma empresa é tratada como "root": não existe modo que desligue o filtro.</p>
 */
public class EmpresaTenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID> {

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        return TenantContext.empresaIdAtual().orElse(TenantContext.NENHUMA_EMPRESA);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
