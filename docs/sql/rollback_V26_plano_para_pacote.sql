-- ============================================================
-- ROLLBACK DA MIGRATION V26
-- Reverte: plano → pacote (nomes de tabelas, colunas e dados)
-- ============================================================
-- QUANDO USAR:
--   Se o deploy da V26 causar problema, aplique este script
--   MANUALMENTE no banco (via psql, DBeaver, etc.) E também
--   faça o rollback do código para a versão anterior.
--
-- ATENÇÃO: Após executar este script, remova também o registro
--   da V26 da tabela flyway_schema_history para que o Flyway
--   não tente reaplicá-la:
--
--   DELETE FROM flyway_schema_history WHERE version = '26';
--
-- ============================================================

BEGIN;

-- 1. Remover constraints criadas pela V26
ALTER TABLE sessao DROP CONSTRAINT IF EXISTS chk_sessao_tipo_agendamento;
ALTER TABLE sessao DROP CONSTRAINT IF EXISTS chk_sessao_tipo_vinculo;

-- 2. Reverter valor 'PLANO' → 'PACOTE' em todos os registros
UPDATE sessao SET tipo_agendamento = 'PACOTE' WHERE tipo_agendamento = 'PLANO';

-- 3. Reverter DEFAULT da coluna
ALTER TABLE sessao ALTER COLUMN tipo_agendamento SET DEFAULT 'PACOTE';

-- 4. Reverter rename da FK em lancamento_financeiro
ALTER TABLE lancamento_financeiro RENAME COLUMN plano_sessao_id TO pacote_sessao_id;

-- 5. Reverter rename da flag de consumo em sessao
ALTER TABLE sessao RENAME COLUMN plano_consumo_registrado TO pacote_consumo_registrado;

-- 6. Reverter rename da FK em sessao
ALTER TABLE sessao RENAME COLUMN plano_sessao_id TO pacote_sessao_id;

-- 7. Reverter rename da coluna de nome
ALTER TABLE plano_sessao RENAME COLUMN nome_plano TO nome_pacote;

-- 8. Reverter rename da tabela principal
ALTER TABLE plano_sessao RENAME TO pacote_sessao;

-- 9. Reverter rename do índice
ALTER INDEX IF EXISTS idx_plano_sessao_cliente RENAME TO idx_pacote_sessao_cliente;

-- 10. Recriar constraints originais (conforme V18)
ALTER TABLE sessao
    ADD CONSTRAINT chk_sessao_tipo_agendamento
    CHECK (tipo_agendamento IN ('PACOTE', 'AVULSO'));

ALTER TABLE sessao
    ADD CONSTRAINT chk_sessao_tipo_vinculo
    CHECK (
        (tipo_agendamento = 'PACOTE' AND pacote_sessao_id IS NOT NULL)
        OR (tipo_agendamento = 'AVULSO' AND pacote_sessao_id IS NULL)
    );

COMMIT;

-- Após executar com sucesso, remover o registro do Flyway:
-- DELETE FROM flyway_schema_history WHERE version = '26';
