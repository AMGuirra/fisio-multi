-- V57: unicidades por empresa, integridade entre registros da mesma empresa e papéis de usuário.

-- ── Papéis ─────────────────────────────────────────────────────────────────────
-- O antigo ADMIN passa a ser o administrador da clínica; SUPER_ADMIN é global (sem empresa).
UPDATE usuario_sistema SET role = 'ADMIN_CLINICA' WHERE role = 'ADMIN';
UPDATE user_role_menu_permission SET role = 'ADMIN_CLINICA' WHERE role = 'ADMIN';

ALTER TABLE usuario_sistema
    ADD CONSTRAINT ck_usuario_sistema_role CHECK (
        role IN ('SUPER_ADMIN', 'ADMIN_CLINICA', 'FISIOTERAPEUTA', 'RECEPCAO', 'ASSESSORIA', 'FINANCEIRO')
    ),
    -- Somente o superadmin existe fora de uma empresa; todo outro papel pertence a uma clínica.
    ADD CONSTRAINT ck_usuario_sistema_empresa_por_papel CHECK (
        (role = 'SUPER_ADMIN' AND empresa_id IS NULL) OR (role <> 'SUPER_ADMIN' AND empresa_id IS NOT NULL)
    );

-- ── Unicidades que eram globais e passam a ser por empresa ────────────────────
ALTER TABLE usuario_sistema DROP CONSTRAINT IF EXISTS usuario_sistema_username_key;
DROP INDEX IF EXISTS usuario_sistema_username_key;
CREATE UNIQUE INDEX uk_usuario_sistema_empresa_username
    ON usuario_sistema (empresa_id, username) WHERE empresa_id IS NOT NULL;
CREATE UNIQUE INDEX uk_usuario_sistema_plataforma_username
    ON usuario_sistema (username) WHERE empresa_id IS NULL;

DROP INDEX IF EXISTS uk_usuario_sistema_email_lower;
CREATE UNIQUE INDEX uk_usuario_sistema_empresa_email_lower
    ON usuario_sistema (empresa_id, LOWER(email)) WHERE email IS NOT NULL AND empresa_id IS NOT NULL;
CREATE UNIQUE INDEX uk_usuario_sistema_plataforma_email_lower
    ON usuario_sistema (LOWER(email)) WHERE email IS NOT NULL AND empresa_id IS NULL;

-- CPF do paciente é único dentro da clínica (o mesmo paciente pode ser atendido em clínicas diferentes).
DROP INDEX IF EXISTS uk_cliente_cpf;
CREATE UNIQUE INDEX uk_cliente_empresa_cpf ON cliente (empresa_id, cpf) WHERE cpf IS NOT NULL;

DROP INDEX IF EXISTS idx_cliente_telefone;
CREATE INDEX idx_cliente_empresa_telefone ON cliente (empresa_id, telefone);

-- Permissões de menu configuradas por clínica.
ALTER TABLE user_role_menu_permission DROP CONSTRAINT IF EXISTS uk_user_role_menu_permission;
ALTER TABLE user_role_menu_permission
    ADD CONSTRAINT uk_user_role_menu_permission UNIQUE (empresa_id, role, menu_key);

-- Um SMTP ativo por clínica e um SMTP global ativo para a plataforma.
DROP INDEX IF EXISTS uk_email_configuracao_global_ativa;
CREATE UNIQUE INDEX uk_email_configuracao_empresa_ativa
    ON email_configuracao (empresa_id) WHERE ativo = TRUE AND empresa_id IS NOT NULL;
CREATE UNIQUE INDEX uk_email_configuracao_global_ativa
    ON email_configuracao ((TRUE)) WHERE ativo = TRUE AND empresa_id IS NULL;

-- ── Integridade entre empresas ─────────────────────────────────────────────────
-- Chaves (id, empresa_id) nos registros referenciados permitem FKs compostas: o banco recusa
-- qualquer vínculo entre registros de empresas diferentes, mesmo que a aplicação falhe.
ALTER TABLE cliente                 ADD CONSTRAINT uk_cliente_id_empresa UNIQUE (id, empresa_id);
ALTER TABLE plano_sessao            ADD CONSTRAINT uk_plano_sessao_id_empresa UNIQUE (id, empresa_id);
ALTER TABLE sessao                  ADD CONSTRAINT uk_sessao_id_empresa UNIQUE (id, empresa_id);
ALTER TABLE solicitacao_agendamento ADD CONSTRAINT uk_solicitacao_agendamento_id_empresa UNIQUE (id, empresa_id);
ALTER TABLE prontuario_soap         ADD CONSTRAINT uk_prontuario_soap_id_empresa UNIQUE (id, empresa_id);

ALTER TABLE sessao
    ADD CONSTRAINT fk_sessao_cliente_mesma_empresa
        FOREIGN KEY (cliente_id, empresa_id) REFERENCES cliente (id, empresa_id),
    ADD CONSTRAINT fk_sessao_plano_mesma_empresa
        FOREIGN KEY (plano_sessao_id, empresa_id) REFERENCES plano_sessao (id, empresa_id);

ALTER TABLE plano_sessao
    ADD CONSTRAINT fk_plano_sessao_cliente_mesma_empresa
        FOREIGN KEY (cliente_id, empresa_id) REFERENCES cliente (id, empresa_id),
    ADD CONSTRAINT fk_plano_sessao_anterior_mesma_empresa
        FOREIGN KEY (plano_anterior_id, empresa_id) REFERENCES plano_sessao (id, empresa_id);

ALTER TABLE solicitacao_agendamento
    ADD CONSTRAINT fk_solicitacao_agendamento_cliente_mesma_empresa
        FOREIGN KEY (cliente_id, empresa_id) REFERENCES cliente (id, empresa_id);

ALTER TABLE horario_bloqueado
    ADD CONSTRAINT fk_horario_bloqueado_solicitacao_mesma_empresa
        FOREIGN KEY (solicitacao_id, empresa_id) REFERENCES solicitacao_agendamento (id, empresa_id);

ALTER TABLE lancamento_financeiro
    ADD CONSTRAINT fk_lancamento_cliente_mesma_empresa
        FOREIGN KEY (cliente_id, empresa_id) REFERENCES cliente (id, empresa_id),
    ADD CONSTRAINT fk_lancamento_plano_mesma_empresa
        FOREIGN KEY (plano_sessao_id, empresa_id) REFERENCES plano_sessao (id, empresa_id);

ALTER TABLE prontuario_soap
    ADD CONSTRAINT fk_prontuario_soap_cliente_mesma_empresa
        FOREIGN KEY (cliente_id, empresa_id) REFERENCES cliente (id, empresa_id),
    ADD CONSTRAINT fk_prontuario_soap_sessao_mesma_empresa
        FOREIGN KEY (sessao_id, empresa_id) REFERENCES sessao (id, empresa_id);

ALTER TABLE prontuario_anexo
    ADD CONSTRAINT fk_prontuario_anexo_cliente_mesma_empresa
        FOREIGN KEY (cliente_id, empresa_id) REFERENCES cliente (id, empresa_id);

ALTER TABLE lista_espera
    ADD CONSTRAINT fk_lista_espera_cliente_mesma_empresa
        FOREIGN KEY (cliente_id, empresa_id) REFERENCES cliente (id, empresa_id);

ALTER TABLE cliente_password_reset_token
    ADD CONSTRAINT fk_cliente_reset_token_cliente_mesma_empresa
        FOREIGN KEY (cliente_id, empresa_id) REFERENCES cliente (id, empresa_id);

-- Vínculos opcionais com ON DELETE SET NULL (recorrência, sessão pai e evolução do anexo) não
-- podem usar FK composta sem anular também empresa_id; para eles, um gatilho garante a mesma empresa.
CREATE OR REPLACE FUNCTION fn_valida_mesma_empresa() RETURNS trigger AS $$
DECLARE
    coluna_ref TEXT := TG_ARGV[0];
    tabela_ref TEXT := TG_ARGV[1];
    valor_ref  TEXT;
    empresa_ref UUID;
BEGIN
    EXECUTE format('SELECT ($1).%I::text', coluna_ref) INTO valor_ref USING NEW;
    IF valor_ref IS NULL THEN
        RETURN NEW;
    END IF;
    EXECUTE format('SELECT empresa_id FROM %I WHERE id::text = $1', tabela_ref) INTO empresa_ref USING valor_ref;
    IF empresa_ref IS NOT NULL AND empresa_ref <> NEW.empresa_id THEN
        RAISE EXCEPTION 'Vínculo entre empresas diferentes não é permitido (%.% -> %)', TG_TABLE_NAME, coluna_ref, tabela_ref
            USING ERRCODE = '23503';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sessao_recorrencia_mesma_empresa
    BEFORE INSERT OR UPDATE OF recorrencia_id ON sessao
    FOR EACH ROW EXECUTE FUNCTION fn_valida_mesma_empresa('recorrencia_id', 'recorrencia');

CREATE TRIGGER trg_sessao_pai_mesma_empresa
    BEFORE INSERT OR UPDATE OF recorrencia_pai_id ON sessao
    FOR EACH ROW EXECUTE FUNCTION fn_valida_mesma_empresa('recorrencia_pai_id', 'sessao');

CREATE TRIGGER trg_prontuario_anexo_soap_mesma_empresa
    BEFORE INSERT OR UPDATE OF prontuario_soap_id ON prontuario_anexo
    FOR EACH ROW EXECUTE FUNCTION fn_valida_mesma_empresa('prontuario_soap_id', 'prontuario_soap');

-- ── View materializada de BI: agora segmentada por empresa ────────────────────
DROP MATERIALIZED VIEW IF EXISTS mv_dashboard_mensal;

CREATE MATERIALIZED VIEW mv_dashboard_mensal AS
SELECT
    empresa_id,
    DATE_TRUNC('month', data_hora_inicio)                                    AS mes,
    COUNT(*) FILTER (WHERE status <> 'CANCELADA')                            AS total_atendimentos,
    COUNT(*) FILTER (WHERE status = 'REALIZADA')                             AS total_realizadas,
    COUNT(*) FILTER (WHERE status = 'FALTA_FATURADA')                        AS total_faltas_faturadas,
    COUNT(*) FILTER (WHERE status = 'FALTA')                                 AS total_faltas,
    COALESCE(
        SUM(EXTRACT(EPOCH FROM (data_hora_fim - data_hora_inicio)) / 3600.0)
            FILTER (WHERE status <> 'CANCELADA'),
        0
    )                                                                        AS horas_agendadas_e_faturadas
FROM sessao
GROUP BY empresa_id, DATE_TRUNC('month', data_hora_inicio);

CREATE UNIQUE INDEX idx_mv_dashboard_mensal_empresa_mes
    ON mv_dashboard_mensal (empresa_id, mes);
