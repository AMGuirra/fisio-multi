CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS btree_gist;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM pg_proc p
          JOIN pg_namespace n ON n.oid = p.pronamespace
         WHERE p.proname = 'gen_random_uuid'
           AND n.nspname = 'public'
    ) THEN
        EXECUTE 'CREATE FUNCTION public.gen_random_uuid() RETURNS uuid AS ''SELECT uuid_generate_v4();'' LANGUAGE sql VOLATILE';
    END IF;
END $$;

ALTER TABLE usuario_sistema
    ADD COLUMN IF NOT EXISTS email VARCHAR(160);

CREATE UNIQUE INDEX IF NOT EXISTS uk_usuario_sistema_email_lower
    ON usuario_sistema (LOWER(email))
    WHERE email IS NOT NULL;

CREATE TABLE IF NOT EXISTS email_configuracao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope_type VARCHAR(30) NOT NULL DEFAULT 'GLOBAL',
    scope_id UUID,
    nome_remetente VARCHAR(120) NOT NULL,
    email_remetente VARCHAR(160) NOT NULL,
    senha_app_criptografada TEXT NOT NULL,
    smtp_host VARCHAR(180) NOT NULL,
    smtp_port INTEGER NOT NULL,
    usa_autenticacao BOOLEAN NOT NULL DEFAULT TRUE,
    usa_tls BOOLEAN NOT NULL DEFAULT TRUE,
    usa_ssl BOOLEAN NOT NULL DEFAULT FALSE,
    reply_to VARCHAR(160),
    ativo BOOLEAN NOT NULL DEFAULT FALSE,
    observacoes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_email_configuracao_global_ativa
    ON email_configuracao (scope_type)
    WHERE ativo = TRUE AND scope_id IS NULL;

CREATE TABLE IF NOT EXISTS email_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo VARCHAR(80) NOT NULL,
    destinatario_principal VARCHAR(160),
    destinatarios TEXT,
    assunto VARCHAR(220) NOT NULL,
    status VARCHAR(30) NOT NULL,
    mensagem_erro TEXT,
    template_id VARCHAR(120),
    usuario_disparador VARCHAR(80),
    entidade_tipo VARCHAR(80),
    entidade_id UUID,
    observacoes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_email_log_tipo_data
    ON email_log (tipo, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_email_log_entidade
    ON email_log (entidade_tipo, entidade_id);

CREATE TABLE IF NOT EXISTS password_reset_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuario_sistema (id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    requested_ip VARCHAR(64),
    requested_user_agent VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_password_reset_token_hash
    ON password_reset_token (token_hash);

CREATE INDEX IF NOT EXISTS idx_password_reset_token_usuario
    ON password_reset_token (usuario_id, expires_at DESC);

ALTER TABLE bloqueio_agenda
    ADD COLUMN IF NOT EXISTS dia_inteiro BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS observacao TEXT,
    ADD COLUMN IF NOT EXISTS status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS origem VARCHAR(20),
    ADD COLUMN IF NOT EXISTS atualizado_por VARCHAR(80),
    ADD COLUMN IF NOT EXISTS grupo_referencia UUID;

UPDATE bloqueio_agenda
   SET status = CASE WHEN ativo THEN 'ATIVO' ELSE 'CANCELADO' END
 WHERE status IS NULL;

UPDATE bloqueio_agenda
   SET origem = 'MANUAL'
 WHERE origem IS NULL;

ALTER TABLE bloqueio_agenda
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN origem SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_bloqueio_agenda_status
    ON bloqueio_agenda (status, data_hora_inicio DESC);

ALTER TABLE cliente
    ADD COLUMN IF NOT EXISTS cpf VARCHAR(11),
    ADD COLUMN IF NOT EXISTS cep VARCHAR(8),
    ADD COLUMN IF NOT EXISTS rua VARCHAR(160),
    ADD COLUMN IF NOT EXISTS numero VARCHAR(20),
    ADD COLUMN IF NOT EXISTS bairro VARCHAR(120),
    ADD COLUMN IF NOT EXISTS cidade VARCHAR(120),
    ADD COLUMN IF NOT EXISTS estado VARCHAR(2);

ALTER TABLE solicitacao_agendamento
    ADD COLUMN IF NOT EXISTS cpf VARCHAR(11),
    ADD COLUMN IF NOT EXISTS local_atendimento VARCHAR(20),
    ADD COLUMN IF NOT EXISTS cep VARCHAR(8),
    ADD COLUMN IF NOT EXISTS rua VARCHAR(160),
    ADD COLUMN IF NOT EXISTS numero VARCHAR(20),
    ADD COLUMN IF NOT EXISTS bairro VARCHAR(120),
    ADD COLUMN IF NOT EXISTS cidade VARCHAR(120),
    ADD COLUMN IF NOT EXISTS estado VARCHAR(2);

UPDATE solicitacao_agendamento
   SET local_atendimento = 'CLINICA'
 WHERE local_atendimento IS NULL;

ALTER TABLE solicitacao_agendamento
    ALTER COLUMN local_atendimento SET DEFAULT 'CLINICA',
    ALTER COLUMN local_atendimento SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_cliente_cpf
    ON cliente (cpf)
    WHERE cpf IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_solicitacao_agendamento_cpf
    ON solicitacao_agendamento (cpf);

DO $$
DECLARE
    tipo_cliente_id TEXT;
    registros BIGINT;
BEGIN
    SELECT c.data_type
      INTO tipo_cliente_id
      FROM information_schema.columns c
     WHERE c.table_schema = 'public'
       AND c.table_name = 'cliente'
       AND c.column_name = 'id';

    IF tipo_cliente_id IS NULL THEN
        RAISE EXCEPTION 'Tabela public.cliente nao encontrada.';
    END IF;

    IF tipo_cliente_id = 'bigint' THEN
        ALTER TABLE cliente ADD COLUMN IF NOT EXISTS id_uuid UUID;
        UPDATE cliente
           SET id_uuid = COALESCE(id_uuid, gen_random_uuid());
        ALTER TABLE cliente ALTER COLUMN id_uuid SET NOT NULL;

        IF EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = 'public'
               AND table_name = 'sessao'
               AND column_name = 'cliente_id'
               AND data_type = 'bigint'
        ) THEN
            ALTER TABLE sessao ADD COLUMN IF NOT EXISTS cliente_id_uuid UUID;
            UPDATE sessao s
               SET cliente_id_uuid = c.id_uuid
              FROM cliente c
             WHERE s.cliente_id = c.id;
            ALTER TABLE sessao DROP COLUMN cliente_id CASCADE;
            ALTER TABLE sessao RENAME COLUMN cliente_id_uuid TO cliente_id;
            ALTER TABLE sessao ALTER COLUMN cliente_id SET NOT NULL;
        END IF;

        IF EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = 'public'
               AND table_name = 'pacote_sessao'
               AND column_name = 'cliente_id'
               AND data_type = 'bigint'
        ) THEN
            ALTER TABLE pacote_sessao ADD COLUMN IF NOT EXISTS cliente_id_uuid UUID;
            UPDATE pacote_sessao p
               SET cliente_id_uuid = c.id_uuid
              FROM cliente c
             WHERE p.cliente_id = c.id;
            ALTER TABLE pacote_sessao DROP COLUMN cliente_id CASCADE;
            ALTER TABLE pacote_sessao RENAME COLUMN cliente_id_uuid TO cliente_id;
            ALTER TABLE pacote_sessao ALTER COLUMN cliente_id SET NOT NULL;
        END IF;

        IF EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = 'public'
               AND table_name = 'prontuario_soap'
               AND column_name = 'cliente_id'
               AND data_type = 'bigint'
        ) THEN
            ALTER TABLE prontuario_soap ADD COLUMN IF NOT EXISTS cliente_id_uuid UUID;
            UPDATE prontuario_soap p
               SET cliente_id_uuid = c.id_uuid
              FROM cliente c
             WHERE p.cliente_id = c.id;
            ALTER TABLE prontuario_soap DROP COLUMN cliente_id CASCADE;
            ALTER TABLE prontuario_soap RENAME COLUMN cliente_id_uuid TO cliente_id;
            ALTER TABLE prontuario_soap ALTER COLUMN cliente_id SET NOT NULL;
        END IF;

        IF EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = 'public'
               AND table_name = 'solicitacao_agendamento'
               AND column_name = 'cliente_id'
               AND data_type = 'bigint'
        ) THEN
            ALTER TABLE solicitacao_agendamento ADD COLUMN IF NOT EXISTS cliente_id_uuid UUID;
            UPDATE solicitacao_agendamento s
               SET cliente_id_uuid = c.id_uuid
              FROM cliente c
             WHERE s.cliente_id = c.id;
            ALTER TABLE solicitacao_agendamento DROP COLUMN cliente_id CASCADE;
            ALTER TABLE solicitacao_agendamento RENAME COLUMN cliente_id_uuid TO cliente_id;
        END IF;

        IF EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = 'public'
               AND table_name = 'lancamento_financeiro'
               AND column_name = 'cliente_id'
               AND data_type = 'bigint'
        ) THEN
            ALTER TABLE lancamento_financeiro ADD COLUMN IF NOT EXISTS cliente_id_uuid UUID;
            UPDATE lancamento_financeiro l
               SET cliente_id_uuid = c.id_uuid
              FROM cliente c
             WHERE l.cliente_id = c.id;
            ALTER TABLE lancamento_financeiro DROP COLUMN cliente_id CASCADE;
            ALTER TABLE lancamento_financeiro RENAME COLUMN cliente_id_uuid TO cliente_id;
        END IF;

        IF EXISTS (
            SELECT 1
              FROM pg_constraint
             WHERE conname = 'cliente_pkey'
               AND conrelid = 'cliente'::regclass
        ) THEN
            ALTER TABLE cliente DROP CONSTRAINT cliente_pkey;
        END IF;
        ALTER TABLE cliente DROP COLUMN id CASCADE;
        ALTER TABLE cliente RENAME COLUMN id_uuid TO id;
        ALTER TABLE cliente ALTER COLUMN id SET DEFAULT gen_random_uuid();
        ALTER TABLE cliente ADD CONSTRAINT cliente_pkey PRIMARY KEY (id);
    END IF;

    IF tipo_cliente_id <> 'uuid' AND tipo_cliente_id <> 'bigint' THEN
        RAISE EXCEPTION 'Tipo nao suportado para public.cliente.id: %', tipo_cliente_id;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = 'public'
           AND table_name = 'solicitacao_agendamento'
           AND column_name = 'cliente_id'
           AND data_type = 'bigint'
    ) THEN
        EXECUTE 'SELECT count(*) FROM solicitacao_agendamento WHERE cliente_id IS NOT NULL' INTO registros;
        IF registros > 0 THEN
            RAISE EXCEPTION 'Schema legado incompativel: public.solicitacao_agendamento.cliente_id ainda esta como bigint com % registros preenchidos. Execute docs/sql/correcao_cliente_id_uuid.sql.', registros;
        END IF;
        IF EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = 'public'
               AND table_name = 'solicitacao_agendamento'
               AND column_name = 'cliente_id_uuid'
        ) THEN
            ALTER TABLE solicitacao_agendamento DROP COLUMN cliente_id_uuid;
        END IF;
        ALTER TABLE solicitacao_agendamento DROP COLUMN cliente_id CASCADE;
        ALTER TABLE solicitacao_agendamento ADD COLUMN cliente_id UUID REFERENCES cliente(id);
    END IF;

    IF EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = 'public'
           AND table_name = 'sessao'
           AND column_name = 'cliente_id'
           AND data_type = 'uuid'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
              FROM pg_constraint
             WHERE conname = 'sessao_cliente_id_fkey'
               AND conrelid = 'sessao'::regclass
        ) THEN
            ALTER TABLE sessao
                ADD CONSTRAINT sessao_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES cliente(id);
        END IF;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = 'public'
           AND table_name = 'pacote_sessao'
           AND column_name = 'cliente_id'
           AND data_type = 'uuid'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
              FROM pg_constraint
             WHERE conname = 'pacote_sessao_cliente_id_fkey'
               AND conrelid = 'pacote_sessao'::regclass
        ) THEN
            ALTER TABLE pacote_sessao
                ADD CONSTRAINT pacote_sessao_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES cliente(id);
        END IF;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = 'public'
           AND table_name = 'prontuario_soap'
           AND column_name = 'cliente_id'
           AND data_type = 'uuid'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
              FROM pg_constraint
             WHERE conname = 'prontuario_soap_cliente_id_fkey'
               AND conrelid = 'prontuario_soap'::regclass
        ) THEN
            ALTER TABLE prontuario_soap
                ADD CONSTRAINT prontuario_soap_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES cliente(id);
        END IF;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = 'public'
           AND table_name = 'solicitacao_agendamento'
           AND column_name = 'cliente_id'
           AND data_type = 'uuid'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
              FROM pg_constraint
             WHERE conname = 'solicitacao_agendamento_cliente_id_fkey'
               AND conrelid = 'solicitacao_agendamento'::regclass
        ) THEN
            ALTER TABLE solicitacao_agendamento
                ADD CONSTRAINT solicitacao_agendamento_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES cliente(id);
        END IF;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = 'public'
           AND table_name = 'lancamento_financeiro'
           AND column_name = 'cliente_id'
           AND data_type = 'uuid'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
              FROM pg_constraint
             WHERE conname = 'lancamento_financeiro_cliente_id_fkey'
               AND conrelid = 'lancamento_financeiro'::regclass
        ) THEN
            ALTER TABLE lancamento_financeiro
                ADD CONSTRAINT lancamento_financeiro_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES cliente(id);
        END IF;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_pacote_sessao_cliente ON pacote_sessao(cliente_id);
