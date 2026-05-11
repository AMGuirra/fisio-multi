-- V21: Flag de confirmação de presença pelo paciente via portal de autoatendimento

ALTER TABLE sessao
    ADD COLUMN IF NOT EXISTS confirmado_pelo_paciente BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN sessao.confirmado_pelo_paciente IS 'True quando o paciente confirmou presença via portal de autoatendimento';
