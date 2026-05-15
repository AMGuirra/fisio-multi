-- Normaliza valores historicos da auditoria para os enums atuais da aplicacao.
-- Sem isso, qualquer registro antigo com modulo/tipo/origem desconhecido quebra
-- a leitura JPA da pagina /auditoria.

UPDATE auditoria_sistema
   SET modulo = 'PACIENTES'
 WHERE modulo IN ('PACIENTE', 'CLIENTE', 'CLIENTES');

UPDATE auditoria_sistema
   SET modulo = 'AGENDA'
 WHERE modulo IN ('AGENDAMENTO', 'AGENDAMENTOS', 'CALENDARIO', 'SESSAO', 'SESSOES');

UPDATE auditoria_sistema
   SET modulo = 'BLOQUEIOS'
 WHERE modulo IN ('BLOQUEIO', 'HORARIO_BLOQUEADO', 'HORARIOS_BLOQUEADOS');

UPDATE auditoria_sistema
   SET modulo = 'USUARIOS'
 WHERE modulo IN ('USUARIO', 'USER', 'USERS');

UPDATE auditoria_sistema
   SET modulo = 'AUTENTICACAO'
 WHERE modulo IN ('AUTH', 'LOGIN', 'SEGURANCA', 'SECURITY');

UPDATE auditoria_sistema
   SET modulo = 'PRONTUARIO'
 WHERE modulo IN ('PRONTUARIOS', 'SOAP');

UPDATE auditoria_sistema
   SET modulo = 'PLANOS'
 WHERE modulo IN ('PACOTE', 'PACOTES', 'PACOTE_SESSAO', 'PACOTES_SESSOES', 'PLANOS_SESSOES');

UPDATE auditoria_sistema
   SET modulo = 'CONFIGURACOES'
 WHERE modulo IN ('CONFIGURACAO', 'CONFIG', 'CONFIG_CLINICA', 'CONFIG_EMAIL', 'SYSTEM_STATUS', 'STATUS_SISTEMA');

UPDATE auditoria_sistema
   SET modulo = 'SOLICITACOES'
 WHERE modulo IN ('SOLICITACAO', 'SOLICITACAO_AGENDAMENTO');

UPDATE auditoria_sistema
   SET modulo = 'AUDITORIA'
 WHERE modulo IN ('AUDIT', 'AUDITORIA_SISTEMA');

UPDATE auditoria_sistema
   SET modulo = 'CONFIGURACOES'
 WHERE modulo IS NULL
    OR TRIM(modulo) = ''
    OR modulo NOT IN (
        'PACIENTES',
        'AGENDA',
        'BLOQUEIOS',
        'USUARIOS',
        'AUTENTICACAO',
        'PRONTUARIO',
        'PLANOS',
        'FINANCEIRO',
        'RELATORIOS',
        'CONFIGURACOES',
        'SOLICITACOES',
        'EMAIL',
        'AUDITORIA'
    );

UPDATE auditoria_sistema
   SET tipo_acao = 'CREATE'
 WHERE tipo_acao IN ('INSERT', 'CADASTRO', 'CADASTRAR', 'CRIAR', 'INCLUIR', 'INCLUSAO', 'CREATE_MANUAL');

UPDATE auditoria_sistema
   SET tipo_acao = 'UPDATE'
 WHERE tipo_acao IN ('EDIT', 'EDITAR', 'EDICAO', 'ALTERAR', 'ALTERACAO', 'SAVE', 'SALVAR');

UPDATE auditoria_sistema
   SET tipo_acao = 'DELETE_LOGICAL'
 WHERE tipo_acao IN ('DELETE', 'DELETAR', 'EXCLUIR', 'EXCLUSAO', 'REMOVE', 'REMOVER', 'SOFT_DELETE');

UPDATE auditoria_sistema
   SET tipo_acao = 'DELETE_PHYSICAL'
 WHERE tipo_acao IN ('HARD_DELETE', 'PHYSICAL_DELETE', 'DELETE_PERMANENT');

UPDATE auditoria_sistema
   SET tipo_acao = 'CANCEL'
 WHERE tipo_acao IN ('CANCELAR', 'CANCELAMENTO', 'CANCELED', 'CANCELLED');

UPDATE auditoria_sistema
   SET tipo_acao = 'CONFIRM'
 WHERE tipo_acao IN ('CONFIRMAR', 'CONFIRMACAO', 'CONFIRMED');

UPDATE auditoria_sistema
   SET tipo_acao = 'REJECT'
 WHERE tipo_acao IN ('RECUSAR', 'RECUSA', 'REJEITAR', 'REJEICAO', 'REJECTED');

UPDATE auditoria_sistema
   SET tipo_acao = 'RESCHEDULE'
 WHERE tipo_acao IN ('REAGENDAR', 'REAGENDAMENTO', 'RESCHEDULED');

UPDATE auditoria_sistema
   SET tipo_acao = 'COMPLETE'
 WHERE tipo_acao IN ('CONCLUIR', 'CONCLUSAO', 'CONCLUIDO', 'DONE');

UPDATE auditoria_sistema
   SET tipo_acao = 'NO_SHOW'
 WHERE tipo_acao IN ('FALTA', 'NO_SHOW_BILLABLE', 'ABSENCE');

UPDATE auditoria_sistema
   SET tipo_acao = 'INACTIVATE'
 WHERE tipo_acao IN ('INATIVAR', 'INATIVACAO', 'DESATIVAR', 'DEACTIVATE');

UPDATE auditoria_sistema
   SET tipo_acao = 'ACTIVATE'
 WHERE tipo_acao IN ('ATIVAR', 'ATIVACAO', 'REATIVAR', 'REENABLE');

UPDATE auditoria_sistema
   SET tipo_acao = 'RESTORE'
 WHERE tipo_acao IN ('RESTAURAR', 'RESTAURACAO');

UPDATE auditoria_sistema
   SET tipo_acao = 'EXPORT_PDF'
 WHERE tipo_acao IN ('PDF', 'EXPORTAR_PDF');

UPDATE auditoria_sistema
   SET tipo_acao = 'EXPORT_EXCEL'
 WHERE tipo_acao IN ('EXCEL', 'XLSX', 'EXPORTAR_EXCEL');

UPDATE auditoria_sistema
   SET tipo_acao = 'SEND_EMAIL'
 WHERE tipo_acao IN ('EMAIL', 'ENVIAR_EMAIL', 'ENVIO_EMAIL');

UPDATE auditoria_sistema
   SET tipo_acao = 'RESET_PASSWORD'
 WHERE tipo_acao IN ('PASSWORD_RESET', 'RESET_SENHA', 'RECUPERAR_SENHA');

UPDATE auditoria_sistema
   SET tipo_acao = 'MARK_AS_PAID'
 WHERE tipo_acao IN ('PAGAR', 'MARCAR_PAGO', 'BAIXA', 'BAIXAR');

UPDATE auditoria_sistema
   SET tipo_acao = 'UNDO_PAYMENT'
 WHERE tipo_acao IN ('ESTORNAR', 'ESTORNO', 'DESFAZER_BAIXA');

UPDATE auditoria_sistema
   SET tipo_acao = 'UPDATE'
 WHERE tipo_acao IS NULL
    OR TRIM(tipo_acao) = ''
    OR tipo_acao NOT IN (
        'CREATE',
        'UPDATE',
        'DELETE_LOGICAL',
        'DELETE_PHYSICAL',
        'CANCEL',
        'CONFIRM',
        'REJECT',
        'RESCHEDULE',
        'COMPLETE',
        'NO_SHOW',
        'INACTIVATE',
        'ACTIVATE',
        'RESTORE',
        'EXPORT_PDF',
        'EXPORT_EXCEL',
        'SEND_EMAIL',
        'LOGIN',
        'LOGOUT',
        'RESET_PASSWORD',
        'MARK_AS_PAID',
        'UNDO_PAYMENT'
    );

UPDATE auditoria_sistema
   SET origem_evento = 'MANUAL'
 WHERE origem_evento IN ('WEB', 'USER', 'USUARIO', 'ADMIN', 'OPERADOR');

UPDATE auditoria_sistema
   SET origem_evento = 'SISTEMA'
 WHERE origem_evento IN ('SYSTEM', 'APP', 'AUTOMATICO', 'AUTOMATICA');

UPDATE auditoria_sistema
   SET origem_evento = 'INTEGRACAO'
 WHERE origem_evento IN ('INTEGRATION', 'API', 'EXTERNO', 'EXTERNAL');

UPDATE auditoria_sistema
   SET origem_evento = 'ONLINE'
 WHERE origem_evento IN ('PUBLICO', 'PUBLIC', 'PORTAL', 'PACIENTE');

UPDATE auditoria_sistema
   SET origem_evento = 'IMPORTACAO'
 WHERE origem_evento IN ('IMPORT', 'IMPORTAR');

UPDATE auditoria_sistema
   SET origem_evento = 'SISTEMA'
 WHERE origem_evento IS NULL
    OR TRIM(origem_evento) = ''
    OR origem_evento NOT IN ('MANUAL', 'SISTEMA', 'INTEGRACAO', 'ONLINE', 'IMPORTACAO');
