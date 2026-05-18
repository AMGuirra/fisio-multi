-- V33: suporte a entradas anônimas na lista de espera (formulário público)
-- e campo de observação para o link de rejeição→espera

ALTER TABLE lista_espera
    ALTER COLUMN cliente_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS nome_publico    VARCHAR(200),
    ADD COLUMN IF NOT EXISTS telefone_publico VARCHAR(20),
    ADD COLUMN IF NOT EXISTS email_publico   VARCHAR(150);
