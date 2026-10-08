-- V56: associa todos os dados de negócio a uma empresa e migra os registros existentes
-- para a empresa legada "Clínica Principal" (00000000-0000-0000-0000-000000000001).

DO $$
DECLARE
    legada CONSTANT UUID := '00000000-0000-0000-0000-000000000001';
    tabela TEXT;
    -- Dados de clínica: empresa_id obrigatório.
    obrigatorias TEXT[] := ARRAY[
        'configuracao_clinica',
        'cliente',
        'cliente_password_reset_token',
        'solicitacao_agendamento',
        'horario_bloqueado',
        'bloqueio_agenda',
        'recorrencia',
        'sessao',
        'plano_sessao',
        'lancamento_financeiro',
        'prontuario_soap',
        'prontuario_anexo',
        'lista_espera',
        'user_role_menu_permission'
    ];
    -- Tabelas compartilhadas com a plataforma: empresa_id nulo = registro global (superadmin/Gestor).
    opcionais TEXT[] := ARRAY[
        'usuario_sistema',
        'auditoria_sistema',
        'email_log',
        'email_configuracao'
    ];
BEGIN
    FOREACH tabela IN ARRAY obrigatorias || opcionais LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN empresa_id UUID', tabela);
        EXECUTE format('UPDATE %I SET empresa_id = %L WHERE empresa_id IS NULL', tabela, legada);
        EXECUTE format(
            'ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (empresa_id) REFERENCES empresa (id)',
            tabela, 'fk_' || tabela || '_empresa'
        );
        EXECUTE format('CREATE INDEX %I ON %I (empresa_id)', 'idx_' || tabela || '_empresa', tabela);
    END LOOP;

    FOREACH tabela IN ARRAY obrigatorias LOOP
        EXECUTE format('ALTER TABLE %I ALTER COLUMN empresa_id SET NOT NULL', tabela);
    END LOOP;
END $$;

-- SMTP existente era da clínica: passa a ser o SMTP da empresa legada.
UPDATE email_configuracao
SET scope_type = 'CLINICA',
    scope_id   = empresa_id
WHERE empresa_id IS NOT NULL;

-- Índices compostos para as consultas mais frequentes filtradas por empresa.
CREATE INDEX idx_sessao_empresa_inicio ON sessao (empresa_id, data_hora_inicio);
CREATE INDEX idx_cliente_empresa_nome ON cliente (empresa_id, nome_completo);
CREATE INDEX idx_lancamento_empresa_recebimento ON lancamento_financeiro (empresa_id, data_recebimento);
CREATE INDEX idx_solicitacao_agendamento_empresa_status ON solicitacao_agendamento (empresa_id, status);
CREATE INDEX idx_auditoria_sistema_empresa_data ON auditoria_sistema (empresa_id, data_hora_evento DESC);
CREATE INDEX idx_email_log_empresa_data ON email_log (empresa_id, created_at DESC);
CREATE INDEX idx_bloqueio_agenda_empresa_inicio ON bloqueio_agenda (empresa_id, data_hora_inicio);
CREATE INDEX idx_lista_espera_empresa_status ON lista_espera (empresa_id, status);
