CREATE TABLE cliente_password_reset_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id UUID NOT NULL REFERENCES cliente(id),
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    requested_ip VARCHAR(45),
    requested_user_agent VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_cliente_reset_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_cliente_reset_token_cliente ON cliente_password_reset_token(cliente_id);
