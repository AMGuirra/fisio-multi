-- V55: solicitações públicas de suporte, solicitações de cancelamento e alertas da área Gestor.

CREATE TABLE solicitacao_suporte (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id         UUID REFERENCES empresa (id) ON DELETE SET NULL,
    nome               VARCHAR(160) NOT NULL,
    cpf                VARCHAR(11),
    cnpj               VARCHAR(14),
    email              VARCHAR(160) NOT NULL,
    descricao          VARCHAR(500) NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    observacao_gestor  TEXT,
    ip_origem          VARCHAR(64),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_solicitacao_suporte_status CHECK (status IN ('PENDENTE', 'EM_ANALISE', 'RESPONDIDA', 'ENCERRADA')),
    CONSTRAINT ck_solicitacao_suporte_documento CHECK (cpf IS NOT NULL OR cnpj IS NOT NULL),
    CONSTRAINT ck_solicitacao_suporte_cpf CHECK (cpf IS NULL OR cpf ~ '^[0-9]{11}$'),
    CONSTRAINT ck_solicitacao_suporte_cnpj CHECK (cnpj IS NULL OR cnpj ~ '^[0-9]{14}$'),
    CONSTRAINT ck_solicitacao_suporte_descricao CHECK (char_length(descricao) BETWEEN 1 AND 500)
);

CREATE INDEX idx_solicitacao_suporte_status ON solicitacao_suporte (status, created_at DESC);
CREATE INDEX idx_solicitacao_suporte_empresa ON solicitacao_suporte (empresa_id);

CREATE TABLE solicitacao_cancelamento (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id              UUID REFERENCES empresa (id) ON DELETE SET NULL,
    origem                  VARCHAR(20)  NOT NULL,
    solicitante_usuario_id  UUID REFERENCES usuario_sistema (id) ON DELETE SET NULL,
    nome_solicitante        VARCHAR(160) NOT NULL,
    email_contato           VARCHAR(160) NOT NULL,
    cpf                     VARCHAR(11),
    cnpj                    VARCHAR(14),
    motivo                  VARCHAR(40)  NOT NULL,
    descricao               VARCHAR(500),
    status                  VARCHAR(30)  NOT NULL DEFAULT 'PENDENTE',
    data_cancelamento_agendada DATE,
    observacao_gestor       TEXT,
    concluida_em            TIMESTAMPTZ,
    ip_origem               VARCHAR(64),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_solicitacao_cancelamento_status CHECK (
        status IN ('PENDENTE', 'EM_ANALISE', 'CANCELAMENTO_AGENDADO', 'CANCELADA', 'RECUSADA')
    ),
    CONSTRAINT ck_solicitacao_cancelamento_origem CHECK (origem IN ('SISTEMA', 'PUBLICA')),
    CONSTRAINT ck_solicitacao_cancelamento_descricao CHECK (descricao IS NULL OR char_length(descricao) <= 500),
    CONSTRAINT ck_solicitacao_cancelamento_cpf CHECK (cpf IS NULL OR cpf ~ '^[0-9]{11}$'),
    CONSTRAINT ck_solicitacao_cancelamento_cnpj CHECK (cnpj IS NULL OR cnpj ~ '^[0-9]{14}$')
);

CREATE INDEX idx_solicitacao_cancelamento_status ON solicitacao_cancelamento (status, created_at DESC);
CREATE INDEX idx_solicitacao_cancelamento_empresa ON solicitacao_cancelamento (empresa_id);

-- Histórico de cada mudança de status de uma solicitação de cancelamento.
CREATE TABLE solicitacao_cancelamento_historico (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    solicitacao_id   UUID        NOT NULL REFERENCES solicitacao_cancelamento (id) ON DELETE CASCADE,
    status_anterior  VARCHAR(30),
    status_novo      VARCHAR(30) NOT NULL,
    observacao       TEXT,
    usuario_nome     VARCHAR(160),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_solicitacao_cancelamento_historico ON solicitacao_cancelamento_historico (solicitacao_id, created_at);

-- Alertas exibidos no painel do Gestor (novos cadastros, suporte, testes vencendo etc.).
CREATE TABLE notificacao_gestor (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo               VARCHAR(40)  NOT NULL,
    titulo             VARCHAR(200) NOT NULL,
    mensagem           TEXT,
    empresa_id         UUID REFERENCES empresa (id) ON DELETE SET NULL,
    referencia_id      UUID,
    link               VARCHAR(255),
    lida               BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notificacao_gestor_lida ON notificacao_gestor (lida, created_at DESC);
CREATE INDEX idx_notificacao_gestor_empresa ON notificacao_gestor (empresa_id);
