CREATE TABLE prontuario_soap (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id UUID NOT NULL REFERENCES cliente(id),
    sessao_id UUID REFERENCES sessao(id),
    subjetivo TEXT NOT NULL,
    objetivo TEXT NOT NULL,
    avaliacao TEXT NOT NULL,
    plano TEXT NOT NULL,
    criado_por VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_prontuario_cliente ON prontuario_soap(cliente_id);
