ALTER TABLE solicitacao_agendamento
    ADD COLUMN IF NOT EXISTS aguardando_confirmacao_paciente BOOLEAN NOT NULL DEFAULT FALSE;
