CREATE TABLE mensagem_whatsapp (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    destinatario VARCHAR(20) NOT NULL,
    conteudo TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    referencia VARCHAR(120),
    agendada_para TIMESTAMPTZ,
    enviada_em TIMESTAMPTZ,
    tentativas INTEGER NOT NULL DEFAULT 0,
    erro TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mensagem_whatsapp_status ON mensagem_whatsapp(status);
