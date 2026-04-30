CREATE EXTENSION IF NOT EXISTS pgcrypto;

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
        RAISE EXCEPTION 'Tabela public.cliente não encontrada.';
    END IF;

    IF tipo_cliente_id = 'bigint' THEN
        ALTER TABLE cliente ADD COLUMN id_uuid UUID;
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
            ALTER TABLE sessao ADD COLUMN cliente_id_uuid UUID;
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
            ALTER TABLE pacote_sessao ADD COLUMN cliente_id_uuid UUID;
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
            ALTER TABLE prontuario_soap ADD COLUMN cliente_id_uuid UUID;
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
            ALTER TABLE solicitacao_agendamento ADD COLUMN cliente_id_uuid UUID;
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
            ALTER TABLE lancamento_financeiro ADD COLUMN cliente_id_uuid UUID;
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

        IF EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = 'public'
               AND table_name = 'sessao'
               AND column_name = 'cliente_id'
               AND data_type = 'uuid'
        ) THEN
            IF EXISTS (
                SELECT 1
                  FROM pg_constraint
                 WHERE conname = 'sessao_cliente_id_fkey'
                   AND conrelid = 'sessao'::regclass
            ) THEN
                ALTER TABLE sessao DROP CONSTRAINT sessao_cliente_id_fkey;
            END IF;
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
            IF EXISTS (
                SELECT 1
                  FROM pg_constraint
                 WHERE conname = 'pacote_sessao_cliente_id_fkey'
                   AND conrelid = 'pacote_sessao'::regclass
            ) THEN
                ALTER TABLE pacote_sessao DROP CONSTRAINT pacote_sessao_cliente_id_fkey;
            END IF;
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
            IF EXISTS (
                SELECT 1
                  FROM pg_constraint
                 WHERE conname = 'prontuario_soap_cliente_id_fkey'
                   AND conrelid = 'prontuario_soap'::regclass
            ) THEN
                ALTER TABLE prontuario_soap DROP CONSTRAINT prontuario_soap_cliente_id_fkey;
            END IF;
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
            IF EXISTS (
                SELECT 1
                  FROM pg_constraint
                 WHERE conname = 'solicitacao_agendamento_cliente_id_fkey'
                   AND conrelid = 'solicitacao_agendamento'::regclass
            ) THEN
                ALTER TABLE solicitacao_agendamento DROP CONSTRAINT solicitacao_agendamento_cliente_id_fkey;
            END IF;
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
            IF EXISTS (
                SELECT 1
                  FROM pg_constraint
                 WHERE conname = 'lancamento_financeiro_cliente_id_fkey'
                   AND conrelid = 'lancamento_financeiro'::regclass
            ) THEN
                ALTER TABLE lancamento_financeiro DROP CONSTRAINT lancamento_financeiro_cliente_id_fkey;
            END IF;
            ALTER TABLE lancamento_financeiro
                ADD CONSTRAINT lancamento_financeiro_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES cliente(id);
        END IF;

        CREATE INDEX idx_pacote_sessao_cliente ON pacote_sessao(cliente_id);
        RAISE NOTICE 'Correção concluída: cliente.id migrado para UUID.';
        RETURN;
    END IF;

    IF tipo_cliente_id <> 'uuid' THEN
        RAISE EXCEPTION 'Tipo não suportado para public.cliente.id: %', tipo_cliente_id;
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
            RAISE EXCEPTION 'Schema legado incompatível: public.solicitacao_agendamento.cliente_id ainda está como bigint com % registros preenchidos. Execute docs/sql/correcao_cliente_id_uuid.sql.', registros;
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

END $$;
