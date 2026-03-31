ALTER TABLE bloqueio_agenda
    ADD COLUMN IF NOT EXISTS dia_inteiro BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS observacao TEXT,
    ADD COLUMN IF NOT EXISTS status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS origem VARCHAR(20),
    ADD COLUMN IF NOT EXISTS atualizado_por VARCHAR(80),
    ADD COLUMN IF NOT EXISTS grupo_referencia UUID;

UPDATE bloqueio_agenda
SET status = CASE WHEN ativo THEN 'ATIVO' ELSE 'CANCELADO' END
WHERE status IS NULL;

UPDATE bloqueio_agenda
SET origem = 'MANUAL'
WHERE origem IS NULL;

ALTER TABLE bloqueio_agenda
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN origem SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_bloqueio_agenda_status
    ON bloqueio_agenda (status, data_hora_inicio DESC);
