-- V19: Campos específicos de fisioterapia no prontuário SOAP
-- Escala de dor EVA (0-10), região corporal, técnicas utilizadas e observações de evolução

ALTER TABLE prontuario_soap
    ADD COLUMN IF NOT EXISTS escala_eva_dor      INTEGER         CHECK (escala_eva_dor BETWEEN 0 AND 10),
    ADD COLUMN IF NOT EXISTS regiao_corporal      VARCHAR(120),
    ADD COLUMN IF NOT EXISTS tecnicas_utilizadas  TEXT,
    ADD COLUMN IF NOT EXISTS obs_evolucao         TEXT;

COMMENT ON COLUMN prontuario_soap.escala_eva_dor     IS 'Escala Visual Analógica de dor (0 = sem dor, 10 = pior dor possível)';
COMMENT ON COLUMN prontuario_soap.regiao_corporal     IS 'Principal região corporal tratada na sessão (ex.: coluna lombar, joelho D)';
COMMENT ON COLUMN prontuario_soap.tecnicas_utilizadas IS 'Técnicas/recursos utilizados na sessão (ex.: RPG, TENS, crioterapia)';
COMMENT ON COLUMN prontuario_soap.obs_evolucao        IS 'Observações de evolução comparadas com sessões anteriores';
