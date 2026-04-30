CREATE TABLE auditoria_sistema (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    modulo VARCHAR(60) NOT NULL,
    entidade VARCHAR(80) NOT NULL,
    entidade_id VARCHAR(80),
    descricao_registro VARCHAR(255),
    tipo_acao VARCHAR(40) NOT NULL,
    campo_alterado VARCHAR(255),
    valor_anterior TEXT,
    valor_novo TEXT,
    payload_anterior_json TEXT,
    payload_novo_json TEXT,
    motivo TEXT,
    observacao TEXT,
    usuario_id UUID REFERENCES usuario_sistema(id),
    usuario_nome VARCHAR(160),
    perfil_usuario VARCHAR(60),
    data_hora_evento TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    origem_evento VARCHAR(40) NOT NULL,
    ip_origem VARCHAR(64),
    user_agent VARCHAR(500),
    unidade_id VARCHAR(80),
    sucesso_operacao BOOLEAN NOT NULL DEFAULT TRUE,
    alteracao_critica BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE auditoria_sistema_campo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auditoria_id UUID NOT NULL REFERENCES auditoria_sistema(id) ON DELETE CASCADE,
    campo_alterado VARCHAR(120) NOT NULL,
    valor_anterior TEXT,
    valor_novo TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auditoria_sistema_data_hora_evento ON auditoria_sistema(data_hora_evento DESC);
CREATE INDEX idx_auditoria_sistema_modulo ON auditoria_sistema(modulo);
CREATE INDEX idx_auditoria_sistema_entidade ON auditoria_sistema(entidade);
CREATE INDEX idx_auditoria_sistema_entidade_id ON auditoria_sistema(entidade_id);
CREATE INDEX idx_auditoria_sistema_usuario_id ON auditoria_sistema(usuario_id);
CREATE INDEX idx_auditoria_sistema_tipo_acao ON auditoria_sistema(tipo_acao);
CREATE INDEX idx_auditoria_sistema_origem_evento ON auditoria_sistema(origem_evento);
CREATE INDEX idx_auditoria_sistema_alteracao_critica ON auditoria_sistema(alteracao_critica);
CREATE INDEX idx_auditoria_sistema_campo_auditoria_id ON auditoria_sistema_campo(auditoria_id);
