CREATE TABLE lancamento_financeiro (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    descricao VARCHAR(180) NOT NULL,
    categoria VARCHAR(80) NOT NULL,
    valor NUMERIC(12, 2) NOT NULL,
    data_competencia TIMESTAMPTZ NOT NULL,
    data_pagamento TIMESTAMPTZ,
    cliente_id UUID REFERENCES cliente(id),
    pacote_sessao_id UUID REFERENCES pacote_sessao(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lancamento_financeiro_data_competencia ON lancamento_financeiro(data_competencia);
