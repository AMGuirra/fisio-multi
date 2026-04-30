-- Corrige bases legadas onde cliente.id ficou como bigint em vez de UUID.
-- Execute este script no PostgreSQL da aplicação antes de aprovar solicitações.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
    tipo_cliente_id TEXT;
BEGIN
    SELECT c.data_type
      INTO tipo_cliente_id
      FROM information_schema.columns c
     WHERE c.table_schema = 'public'
       AND c.table_name = 'cliente'
       AND c.column_name = 'id';

    IF tipo_cliente_id IS NULL THEN
        RAISE EXCEPTION 'Tabela public.cliente não encontrada.';
    END IF;

    IF tipo_cliente_id = 'uuid' THEN
        RAISE NOTICE 'Schema já compatível: public.cliente.id está em UUID.';
        RETURN;
    END IF;

    IF tipo_cliente_id <> 'bigint' THEN
        RAISE EXCEPTION 'Tipo não suportado para public.cliente.id: %', tipo_cliente_id;
    END IF;

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

    ALTER TABLE cliente DROP CONSTRAINT IF EXISTS cliente_pkey;
    ALTER TABLE cliente DROP COLUMN id CASCADE;
    ALTER TABLE cliente RENAME COLUMN id_uuid TO id;
    ALTER TABLE cliente ALTER COLUMN id SET DEFAULT gen_random_uuid();
    ALTER TABLE cliente ADD CONSTRAINT cliente_pkey PRIMARY KEY (id);

    IF EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = 'public'
           AND table_name = 'sessao'
           AND column_name = 'cliente_id'
           AND data_type = 'uuid'
    ) THEN
        ALTER TABLE sessao DROP CONSTRAINT IF EXISTS sessao_cliente_id_fkey;
        ALTER TABLE sessao
            ADD CONSTRAINT sessao_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES cliente(id);
    END IF;

    IF EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = 'public'
           AND table_name = 'pacote_sessao'
           AND column_name = 'cliente_id'
           AND data_type = 'uuid'
    ) THEN
        ALTER TABLE pacote_sessao DROP CONSTRAINT IF EXISTS pacote_sessao_cliente_id_fkey;
        ALTER TABLE pacote_sessao
            ADD CONSTRAINT pacote_sessao_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES cliente(id);
    END IF;

    IF EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = 'public'
           AND table_name = 'prontuario_soap'
           AND column_name = 'cliente_id'
           AND data_type = 'uuid'
    ) THEN
        ALTER TABLE prontuario_soap DROP CONSTRAINT IF EXISTS prontuario_soap_cliente_id_fkey;
        ALTER TABLE prontuario_soap
            ADD CONSTRAINT prontuario_soap_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES cliente(id);
    END IF;

    IF EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = 'public'
           AND table_name = 'solicitacao_agendamento'
           AND column_name = 'cliente_id'
           AND data_type = 'uuid'
    ) THEN
        ALTER TABLE solicitacao_agendamento DROP CONSTRAINT IF EXISTS solicitacao_agendamento_cliente_id_fkey;
        ALTER TABLE solicitacao_agendamento
            ADD CONSTRAINT solicitacao_agendamento_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES cliente(id);
    END IF;

    IF EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = 'public'
           AND table_name = 'lancamento_financeiro'
           AND column_name = 'cliente_id'
           AND data_type = 'uuid'
    ) THEN
        ALTER TABLE lancamento_financeiro DROP CONSTRAINT IF EXISTS lancamento_financeiro_cliente_id_fkey;
        ALTER TABLE lancamento_financeiro
            ADD CONSTRAINT lancamento_financeiro_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES cliente(id);
    END IF;

    CREATE INDEX IF NOT EXISTS idx_pacote_sessao_cliente ON pacote_sessao(cliente_id);

    RAISE NOTICE 'Correção concluída: cliente.id migrado para UUID.';
END $$;
