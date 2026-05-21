-- V34: campo profissional em lancamento_financeiro para relatórios de faturamento por profissional
ALTER TABLE lancamento_financeiro
    ADD COLUMN IF NOT EXISTS profissional VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_lancamento_financeiro_receber
    ON lancamento_financeiro (tipo, status, data_competencia);

CREATE INDEX IF NOT EXISTS idx_lancamento_financeiro_profissional
    ON lancamento_financeiro (profissional);
