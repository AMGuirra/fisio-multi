-- ============================================================
-- V26: Renomeia "pacote" → "plano" em toda a estrutura do banco
-- ============================================================
-- SEGURANÇA DOS DADOS:
--   RENAME TABLE e RENAME COLUMN são operações de metadados em
--   PostgreSQL — instantâneas, sem reescrita de dados e TRANSACIONAIS.
--   Se qualquer passo falhar, o banco reverte automaticamente.
--   O UPDATE na linha 7 converte os valores 'PACOTE' → 'PLANO' sem
--   perder nenhuma linha.
--
-- ROLLBACK MANUAL:
--   Caso seja necessário reverter, aplicar o script:
--   docs/sql/rollback_V26_plano_para_pacote.sql
--   (ou criar nova migration V27 com o conteúdo desse arquivo)
-- ============================================================

-- 1. Remover constraints que referenciam o valor 'PACOTE' e a coluna
--    pacote_sessao_id (serão recriadas com os novos nomes/valores)
ALTER TABLE sessao DROP CONSTRAINT IF EXISTS chk_sessao_tipo_agendamento;
ALTER TABLE sessao DROP CONSTRAINT IF EXISTS chk_sessao_tipo_vinculo;
ALTER TABLE sessao DROP CONSTRAINT IF EXISTS sessao_tipo_agendamento_check;

-- 2. Renomear tabela principal
ALTER TABLE pacote_sessao RENAME TO plano_sessao;

-- 3. Renomear coluna de nome do plano
ALTER TABLE plano_sessao RENAME COLUMN nome_pacote TO nome_plano;

-- 4. Renomear FK de sessao → plano_sessao
ALTER TABLE sessao RENAME COLUMN pacote_sessao_id TO plano_sessao_id;

-- 5. Renomear flag de consumo registrado em sessao
ALTER TABLE sessao RENAME COLUMN pacote_consumo_registrado TO plano_consumo_registrado;

-- 6. Renomear FK de lancamento_financeiro → plano_sessao
ALTER TABLE lancamento_financeiro RENAME COLUMN pacote_sessao_id TO plano_sessao_id;

-- 7. Converter valor 'PACOTE' → 'PLANO' em todos os registros existentes
UPDATE sessao SET tipo_agendamento = 'PLANO' WHERE tipo_agendamento = 'PACOTE';

-- 8. Atualizar o DEFAULT da coluna (era 'PACOTE', agora é 'PLANO')
ALTER TABLE sessao ALTER COLUMN tipo_agendamento SET DEFAULT 'PLANO';

-- 9. Recriar constraint de valores válidos com o novo valor
ALTER TABLE sessao
    ADD CONSTRAINT chk_sessao_tipo_agendamento
    CHECK (tipo_agendamento IN ('PLANO', 'AVULSO'));

-- 10. Recriar constraint de vínculo com a nova coluna e o novo valor
ALTER TABLE sessao
    ADD CONSTRAINT chk_sessao_tipo_vinculo
    CHECK (
        (tipo_agendamento = 'PLANO'   AND plano_sessao_id IS NOT NULL)
        OR (tipo_agendamento = 'AVULSO' AND plano_sessao_id IS NULL)
    );

-- 11. Renomear índice (se existir)
ALTER INDEX IF EXISTS idx_pacote_sessao_cliente RENAME TO idx_plano_sessao_cliente;
