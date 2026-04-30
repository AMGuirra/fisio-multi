ALTER TABLE usuario_sistema
    ADD COLUMN email VARCHAR(160);

CREATE UNIQUE INDEX uk_usuario_sistema_email_lower
    ON usuario_sistema (LOWER(email))
    WHERE email IS NOT NULL;

CREATE TABLE IF NOT EXISTS email_configuracao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope_type VARCHAR(30) NOT NULL DEFAULT 'GLOBAL',
    scope_id UUID,
    nome_remetente VARCHAR(120) NOT NULL,
    email_remetente VARCHAR(160) NOT NULL,
    senha_app_criptografada TEXT NOT NULL,
    smtp_host VARCHAR(180) NOT NULL,
    smtp_port INTEGER NOT NULL,
    usa_autenticacao BOOLEAN NOT NULL DEFAULT TRUE,
    usa_tls BOOLEAN NOT NULL DEFAULT TRUE,
    usa_ssl BOOLEAN NOT NULL DEFAULT FALSE,
    reply_to VARCHAR(160),
    ativo BOOLEAN NOT NULL DEFAULT FALSE,
    observacoes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_email_configuracao_global_ativa
    ON email_configuracao (scope_type)
    WHERE ativo = TRUE AND scope_id IS NULL;

CREATE TABLE IF NOT EXISTS email_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo VARCHAR(80) NOT NULL,
    destinatario_principal VARCHAR(160),
    destinatarios TEXT,
    assunto VARCHAR(220) NOT NULL,
    status VARCHAR(30) NOT NULL,
    mensagem_erro TEXT,
    template_id VARCHAR(120),
    usuario_disparador VARCHAR(80),
    entidade_tipo VARCHAR(80),
    entidade_id UUID,
    observacoes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_log_tipo_data
    ON email_log (tipo, created_at DESC);

CREATE INDEX idx_email_log_entidade
    ON email_log (entidade_tipo, entidade_id);

CREATE TABLE IF NOT EXISTS password_reset_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuario_sistema (id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    requested_ip VARCHAR(64),
    requested_user_agent VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_password_reset_token_hash
    ON password_reset_token (token_hash);

CREATE INDEX idx_password_reset_token_usuario
    ON password_reset_token (usuario_id, expires_at DESC);
