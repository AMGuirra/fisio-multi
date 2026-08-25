-- V52: Estrutura a avaliação inicial do prontuário em campos clínicos separados.
--
-- Contexto: avaliações criadas antes desta migração permanecem preservadas em
-- `texto_registro`. Os campos abaixo são opcionais para permitir a conversão
-- gradual, sem alterar nem tentar interpretar o conteúdo legado.
--
-- Rollback manual (Flyway Community não reverte migrações automaticamente):
--   ALTER TABLE prontuario_soap DROP COLUMN IF EXISTS queixa_principal;
--   ALTER TABLE prontuario_soap DROP COLUMN IF EXISTS avaliacao_fisica;
--   ALTER TABLE prontuario_soap DROP COLUMN IF EXISTS avaliacao_funcional;
--   ALTER TABLE prontuario_soap DROP COLUMN IF EXISTS objetivos_tratamento;
--   ALTER TABLE prontuario_soap DROP COLUMN IF EXISTS plano_terapeutico;
--   ALTER TABLE prontuario_soap DROP COLUMN IF EXISTS observacoes_avaliacao;

ALTER TABLE prontuario_soap
    ADD COLUMN IF NOT EXISTS queixa_principal       TEXT,
    ADD COLUMN IF NOT EXISTS avaliacao_fisica       TEXT,
    ADD COLUMN IF NOT EXISTS avaliacao_funcional    TEXT,
    ADD COLUMN IF NOT EXISTS objetivos_tratamento   TEXT,
    ADD COLUMN IF NOT EXISTS plano_terapeutico      TEXT,
    ADD COLUMN IF NOT EXISTS observacoes_avaliacao  TEXT;

COMMENT ON COLUMN prontuario_soap.queixa_principal      IS 'Queixa principal relatada na avaliação inicial';
COMMENT ON COLUMN prontuario_soap.avaliacao_fisica      IS 'Achados do exame e da avaliação física inicial';
COMMENT ON COLUMN prontuario_soap.avaliacao_funcional   IS 'Avaliação funcional inicial do paciente';
COMMENT ON COLUMN prontuario_soap.objetivos_tratamento  IS 'Objetivos terapêuticos definidos para o tratamento';
COMMENT ON COLUMN prontuario_soap.plano_terapeutico     IS 'Plano terapêutico inicial';
COMMENT ON COLUMN prontuario_soap.observacoes_avaliacao IS 'Observações complementares da avaliação inicial';
