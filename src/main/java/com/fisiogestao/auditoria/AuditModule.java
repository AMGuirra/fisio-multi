package com.fisiogestao.auditoria;

public enum AuditModule {
    PACIENTES("Pacientes"),
    AGENDA("Agenda"),
    BLOQUEIOS("Bloqueios"),
    USUARIOS("Usuários"),
    AUTENTICACAO("Autenticação"),
    PRONTUARIO("Prontuário"),
    PACOTES("Pacotes"),
    FINANCEIRO("Financeiro"),
    RELATORIOS("Relatórios"),
    CONFIGURACOES("Configurações"),
    SOLICITACOES("Solicitações"),
    EMAIL("E-mail"),
    AUDITORIA("Auditoria");

    private final String label;

    AuditModule(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
