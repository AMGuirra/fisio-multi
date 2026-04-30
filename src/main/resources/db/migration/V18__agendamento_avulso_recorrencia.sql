CREATE TABLE IF NOT EXISTS recorrencia (
    id BIGSERIAL PRIMARY KEY,
    frequencia VARCHAR(20) NOT NULL CHECK (frequencia IN ('SEMANAL', 'MENSAL', 'ANUAL')),
    dias_semana VARCHAR(50),
    intervalo INT NOT NULL DEFAULT 1,
    data_fim DATE,
    max_ocorrencias INT DEFAULT 52,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE sessao
    ADD COLUMN tipo_agendamento VARCHAR(20) NOT NULL DEFAULT 'PACOTE',
    ADD COLUMN valor_avulso NUMERIC(10,2),
    ADD COLUMN recorrencia_id BIGINT REFERENCES recorrencia(id) ON DELETE SET NULL,
    ADD COLUMN recorrencia_pai_id UUID REFERENCES sessao(id) ON DELETE SET NULL;

UPDATE sessao
SET tipo_agendamento = CASE WHEN pacote_sessao_id IS NULL THEN 'AVULSO' ELSE 'PACOTE' END
WHERE tipo_agendamento IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_sessao_tipo_agendamento'
          AND conrelid = 'sessao'::regclass
    ) THEN
        ALTER TABLE sessao
            ADD CONSTRAINT chk_sessao_tipo_agendamento CHECK (tipo_agendamento IN ('PACOTE', 'AVULSO'));
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_sessao_tipo_vinculo'
          AND conrelid = 'sessao'::regclass
    ) THEN
        ALTER TABLE sessao
            ADD CONSTRAINT chk_sessao_tipo_vinculo CHECK (
                (tipo_agendamento = 'PACOTE' AND pacote_sessao_id IS NOT NULL)
                OR (tipo_agendamento = 'AVULSO' AND pacote_sessao_id IS NULL)
            );
    END IF;
END $$;

CREATE INDEX idx_sessao_tipo_agendamento ON sessao(tipo_agendamento);
CREATE INDEX idx_sessao_recorrencia_pai ON sessao(recorrencia_pai_id);
