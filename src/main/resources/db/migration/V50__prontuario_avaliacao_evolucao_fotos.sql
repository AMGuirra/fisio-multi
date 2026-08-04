-- V50: Suporte a Avaliação/Evolução simplificadas e acompanhamento fotográfico clínico.
--
-- Contexto: a tela de Prontuário foi refatorada para separar duas experiências:
--   - Avaliação: um registro clínico principal por paciente, editável em uma única caixa
--     de texto livre, com histórico de alterações preservado via auditoria (antes/depois).
--   - Evolução: um diário clínico contínuo — cada salvamento cria um novo registro
--     independente, nunca sobrescrevendo o anterior.
--
-- Estratégia (não destrutiva): reaproveita a tabela prontuario_soap existente. Os campos
-- S/O/A/P legados (subjetivo, objetivo, avaliacao, plano) continuam intactos e legíveis
-- para registros antigos — apenas deixam de ser obrigatórios para o novo fluxo simplificado,
-- que passa a gravar o texto livre em `texto_registro`. Um discriminador `tipo_registro`
-- marca a natureza do registro; linhas legadas recebem 'EVOLUCAO' por padrão (continuam
-- sendo exibidas no histórico com a grade S/O/A/P completa).
--
-- Para fotos clínicas, reaproveita-se prontuario_anexo (já preparado para BYTEA + vínculo
-- opcional com prontuario_soap_id), adicionando um discriminador de tipo de anexo e os
-- metadados clínicos da fotografia (região, observação, consentimento).
--
-- Rollback manual (Flyway Community não reverte migrações automaticamente):
--   ALTER TABLE prontuario_soap DROP COLUMN IF EXISTS tipo_registro;
--   ALTER TABLE prontuario_soap DROP COLUMN IF EXISTS texto_registro;
--   ALTER TABLE prontuario_soap ALTER COLUMN subjetivo SET NOT NULL; -- só após backfill
--   ALTER TABLE prontuario_soap ALTER COLUMN objetivo SET NOT NULL;  -- só após backfill
--   ALTER TABLE prontuario_soap ALTER COLUMN plano SET NOT NULL;     -- só após backfill
--   ALTER TABLE prontuario_anexo DROP COLUMN IF EXISTS tipo_anexo;
--   ALTER TABLE prontuario_anexo DROP COLUMN IF EXISTS regiao_corporal;
--   ALTER TABLE prontuario_anexo DROP COLUMN IF EXISTS consentimento_confirmado_em;
--   ALTER TABLE prontuario_anexo DROP COLUMN IF EXISTS hash_sha256;

-- --- prontuario_soap -------------------------------------------------------

ALTER TABLE prontuario_soap
    ADD COLUMN IF NOT EXISTS tipo_registro  VARCHAR(20) NOT NULL DEFAULT 'EVOLUCAO',
    ADD COLUMN IF NOT EXISTS texto_registro TEXT;

ALTER TABLE prontuario_soap
    DROP CONSTRAINT IF EXISTS ck_prontuario_soap_tipo_registro;
ALTER TABLE prontuario_soap
    ADD CONSTRAINT ck_prontuario_soap_tipo_registro
        CHECK (tipo_registro IN ('AVALIACAO', 'EVOLUCAO'));

-- Campos S/O/A/P deixam de ser obrigatórios: o fluxo simplificado (avaliação/evolução em
-- texto livre) não os preenche. Registros legados mantêm os valores já gravados.
ALTER TABLE prontuario_soap ALTER COLUMN subjetivo DROP NOT NULL;
ALTER TABLE prontuario_soap ALTER COLUMN objetivo  DROP NOT NULL;
ALTER TABLE prontuario_soap ALTER COLUMN plano      DROP NOT NULL;

-- Uma única avaliação principal (tipo_registro = 'AVALIACAO') por paciente.
CREATE UNIQUE INDEX IF NOT EXISTS uq_prontuario_soap_avaliacao_por_cliente
    ON prontuario_soap (cliente_id)
    WHERE tipo_registro = 'AVALIACAO';

CREATE INDEX IF NOT EXISTS idx_prontuario_soap_cliente_tipo
    ON prontuario_soap (cliente_id, tipo_registro, created_at DESC);

COMMENT ON COLUMN prontuario_soap.tipo_registro  IS 'AVALIACAO (registro único e editável por paciente) ou EVOLUCAO (diário clínico, um registro por salvamento)';
COMMENT ON COLUMN prontuario_soap.texto_registro IS 'Texto livre do fluxo simplificado de Avaliação/Evolução; registros SOAP legados usam subjetivo/objetivo/avaliacao/plano';

-- --- prontuario_anexo --------------------------------------------------------

ALTER TABLE prontuario_anexo
    ADD COLUMN IF NOT EXISTS tipo_anexo                   VARCHAR(20) NOT NULL DEFAULT 'DOCUMENTO',
    ADD COLUMN IF NOT EXISTS regiao_corporal               VARCHAR(120),
    ADD COLUMN IF NOT EXISTS consentimento_confirmado_em    TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS hash_sha256                    VARCHAR(64);

ALTER TABLE prontuario_anexo
    DROP CONSTRAINT IF EXISTS ck_prontuario_anexo_tipo_anexo;
ALTER TABLE prontuario_anexo
    ADD CONSTRAINT ck_prontuario_anexo_tipo_anexo
        CHECK (tipo_anexo IN ('DOCUMENTO', 'FOTO_CLINICA'));

CREATE INDEX IF NOT EXISTS idx_prontuario_anexo_cliente_tipo
    ON prontuario_anexo (cliente_id, tipo_anexo, created_at DESC);

COMMENT ON COLUMN prontuario_anexo.tipo_anexo                 IS 'DOCUMENTO (laudo/PDF/DOCX genérico) ou FOTO_CLINICA (fotografia de acompanhamento vinculada a uma evolução)';
COMMENT ON COLUMN prontuario_anexo.regiao_corporal             IS 'Região corporal fotografada (apenas para FOTO_CLINICA)';
COMMENT ON COLUMN prontuario_anexo.consentimento_confirmado_em IS 'Data/hora em que o profissional confirmou a autorização do paciente para o registro fotográfico (apenas para FOTO_CLINICA)';
COMMENT ON COLUMN prontuario_anexo.hash_sha256                 IS 'Hash SHA-256 do conteúdo, para verificação de integridade';
