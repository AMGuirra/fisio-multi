-- V20: Token de acesso ao portal do paciente
-- Cada cliente recebe um UUID único que serve como link de autoatendimento.

ALTER TABLE cliente
    ADD COLUMN IF NOT EXISTS portal_token UUID;

-- Gera um token para todos os clientes existentes
UPDATE cliente SET portal_token = gen_random_uuid() WHERE portal_token IS NULL;

-- Garante unicidade e impede nulos em novos registros
ALTER TABLE cliente
    ALTER COLUMN portal_token SET NOT NULL,
    ADD CONSTRAINT uq_cliente_portal_token UNIQUE (portal_token);

COMMENT ON COLUMN cliente.portal_token IS 'Token UUID público para acesso ao portal de autoatendimento do paciente';
