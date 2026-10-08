package com.fisiogestao.tenant;

import com.fisiogestao.shared.web.BusinessException;

/** Lançada quando uma operação de clínica é executada sem empresa no contexto. */
public class TenantAusenteException extends BusinessException {

    public TenantAusenteException() {
        super("Operação disponível apenas para usuários vinculados a uma clínica.");
    }
}
