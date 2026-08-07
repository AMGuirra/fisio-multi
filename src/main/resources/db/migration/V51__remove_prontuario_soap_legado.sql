-- V51: Remove o modelo SOAP legado (subjetivo/objetivo/avaliacao/plano) do prontuário.
--
-- Contexto: desde a V50 o fluxo simplificado grava tudo em `texto_registro`. As colunas
-- S/O/A/P só existiam para exibir registros antigos, criados antes da V50, que nunca
-- receberam `texto_registro`. Por decisão do usuário, esses registros legados (e apenas
-- eles) são descartados junto com as colunas — não há mais fallback de exibição em grade.
--
-- Rollback manual (Flyway Community não reverte migrações automaticamente):
--   ALTER TABLE prontuario_soap ADD COLUMN subjetivo TEXT;
--   ALTER TABLE prontuario_soap ADD COLUMN objetivo TEXT;
--   ALTER TABLE prontuario_soap ADD COLUMN avaliacao TEXT;
--   ALTER TABLE prontuario_soap ADD COLUMN plano TEXT;
--   -- (dados legados descartados nesta migração não são recuperáveis)

DELETE FROM prontuario_soap
WHERE texto_registro IS NULL
  AND (subjetivo IS NOT NULL OR objetivo IS NOT NULL OR avaliacao IS NOT NULL OR plano IS NOT NULL);

ALTER TABLE prontuario_soap
    DROP COLUMN IF EXISTS subjetivo,
    DROP COLUMN IF EXISTS objetivo,
    DROP COLUMN IF EXISTS avaliacao,
    DROP COLUMN IF EXISTS plano;
