package com.fisiogestao.cancelamento;

public enum OrigemSolicitacaoCancelamento {
    /** Feita por um ADMIN_CLINICA autenticado (empresa identificada pela sessão). */
    SISTEMA,
    /** Feita pela página pública de suporte (empresa identificada por CPF/CNPJ, se possível). */
    PUBLICA
}
