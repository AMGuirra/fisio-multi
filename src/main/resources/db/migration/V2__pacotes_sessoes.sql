CREATE TABLE IF NOT EXISTS pacote_sessao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id UUID NOT NULL REFERENCES cliente(id),
    nome_pacote VARCHAR(120) NOT NULL,
    sessoes_contratadas INTEGER NOT NULL,
    sessoes_consumidas INTEGER NOT NULL DEFAULT 0,
    valor_total NUMERIC(12, 2) NOT NULL,
    data_inicio DATE NOT NULL,
    data_validade DATE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE sessao
    ADD COLUMN IF NOT EXISTS pacote_sessao_id UUID REFERENCES pacote_sessao(id);

CREATE INDEX IF NOT EXISTS idx_pacote_sessao_cliente ON pacote_sessao(cliente_id);
