-- Migration: V17__system_status.sql
-- Sistema de Status Global do FisioGestao

CREATE TABLE system_status (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status VARCHAR(20) NOT NULL,
    mensagem TEXT,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Inserir registro padrão inicial
INSERT INTO system_status (status, mensagem, atualizado_em)
VALUES ('ONLINE', 'Sistema operando normalmente', CURRENT_TIMESTAMP);