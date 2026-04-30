ALTER TABLE usuario_sistema
    ADD COLUMN nome VARCHAR(160);

UPDATE usuario_sistema
SET nome = username
WHERE nome IS NULL OR TRIM(nome) = '';

ALTER TABLE usuario_sistema
    ALTER COLUMN nome SET NOT NULL;
