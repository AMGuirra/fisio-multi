-- V32: EVA pelo portal, lembretes automáticos, lista de espera, forma de pagamento

-- Feature 1 & 3: campos na tabela sessao
ALTER TABLE sessao
    ADD COLUMN IF NOT EXISTS feedback_eva_paciente     SMALLINT,
    ADD COLUMN IF NOT EXISTS feedback_obs_paciente     TEXT,
    ADD COLUMN IF NOT EXISTS lembrete_enviado          BOOLEAN NOT NULL DEFAULT FALSE;

-- Feature 2: lista de espera
CREATE TABLE IF NOT EXISTS lista_espera (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id           UUID NOT NULL REFERENCES cliente(id),
    periodo_preferido    VARCHAR(20) NOT NULL DEFAULT 'QUALQUER',
    dias_semana          VARCHAR(100),
    observacoes          TEXT,
    status               VARCHAR(20) NOT NULL DEFAULT 'AGUARDANDO',
    notificado_em        TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_lista_espera_status  ON lista_espera (status);
CREATE INDEX IF NOT EXISTS idx_lista_espera_cliente ON lista_espera (cliente_id);

-- Feature 4: método de pagamento em lançamento financeiro
ALTER TABLE lancamento_financeiro
    ADD COLUMN IF NOT EXISTS metodo_pagamento VARCHAR(30);
