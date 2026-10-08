package com.fisiogestao.empresa;

/** Motivos pelos quais uma clínica não pode acessar o sistema. */
public enum MotivoAcessoNegado {
    BLOQUEADO("Acesso bloqueado", "O acesso da sua clínica está bloqueado."),
    INATIVO("Clínica inativa", "A clínica está inativa."),
    TESTE_EXPIRADO("Período de teste encerrado", "O período de teste da sua clínica foi finalizado.");

    private final String titulo;
    private final String mensagem;

    MotivoAcessoNegado(String titulo, String mensagem) {
        this.titulo = titulo;
        this.mensagem = mensagem;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public static MotivoAcessoNegado fromParam(String valor) {
        if (valor == null) {
            return BLOQUEADO;
        }
        for (MotivoAcessoNegado motivo : values()) {
            if (motivo.name().equalsIgnoreCase(valor.trim())) {
                return motivo;
            }
        }
        return BLOQUEADO;
    }
}
