package com.fisiogestao.auth;

import com.fisiogestao.empresa.MotivoAcessoNegado;
import org.springframework.security.authentication.AccountStatusException;

/** Credenciais corretas, mas a clínica do usuário não pode acessar o sistema. */
public class EmpresaAcessoNegadoException extends AccountStatusException {

    private final transient MotivoAcessoNegado motivo;

    public EmpresaAcessoNegadoException(MotivoAcessoNegado motivo) {
        super(motivo.getMensagem());
        this.motivo = motivo;
    }

    public MotivoAcessoNegado getMotivo() {
        return motivo;
    }
}
