CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE configuracao_clinica (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome_clinica VARCHAR(150) NOT NULL,
    dias_funcionamento VARCHAR(120) NOT NULL,
    horario_abertura TIME NOT NULL,
    horario_fechamento TIME NOT NULL,
    horario_inicio_almoco TIME,
    horario_fim_almoco TIME,
    duracao_sessao_minutos INTEGER NOT NULL,
    timezone_id VARCHAR(60) NOT NULL,
    telefone_whatsapp VARCHAR(20),
    email_contato VARCHAR(120),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cliente (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome_completo VARCHAR(160) NOT NULL,
    telefone VARCHAR(20),
    email VARCHAR(120),
    data_nascimento DATE,
    observacoes TEXT,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE solicitacao_agendamento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome_paciente VARCHAR(160) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    email VARCHAR(120),
    data_hora_inicio TIMESTAMPTZ NOT NULL,
    data_hora_fim TIMESTAMPTZ NOT NULL,
    observacoes TEXT,
    status VARCHAR(20) NOT NULL,
    cliente_id UUID REFERENCES cliente(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE horario_bloqueado (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    data_hora_inicio TIMESTAMPTZ NOT NULL,
    data_hora_fim TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    origem VARCHAR(50) NOT NULL,
    motivo VARCHAR(255),
    solicitacao_id UUID UNIQUE REFERENCES solicitacao_agendamento(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bloqueio_agenda (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    data_hora_inicio TIMESTAMPTZ NOT NULL,
    data_hora_fim TIMESTAMPTZ NOT NULL,
    motivo VARCHAR(255) NOT NULL,
    criado_por VARCHAR(80),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sessao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id UUID NOT NULL REFERENCES cliente(id),
    data_hora_inicio TIMESTAMPTZ NOT NULL,
    data_hora_fim TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    tipo_atendimento VARCHAR(80),
    observacoes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sessao_data_hora_inicio ON sessao(data_hora_inicio);
CREATE INDEX idx_sessao_data_hora_fim ON sessao(data_hora_fim);
CREATE INDEX idx_solicitacao_data_hora_inicio ON solicitacao_agendamento(data_hora_inicio);
CREATE INDEX idx_solicitacao_data_hora_fim ON solicitacao_agendamento(data_hora_fim);
CREATE INDEX idx_horario_bloqueado_data_hora_inicio ON horario_bloqueado(data_hora_inicio);
CREATE INDEX idx_horario_bloqueado_data_hora_fim ON horario_bloqueado(data_hora_fim);
CREATE INDEX idx_bloqueio_agenda_data_hora_inicio ON bloqueio_agenda(data_hora_inicio);
CREATE INDEX idx_bloqueio_agenda_data_hora_fim ON bloqueio_agenda(data_hora_fim);

INSERT INTO configuracao_clinica (
    id,
    nome_clinica,
    dias_funcionamento,
    horario_abertura,
    horario_fechamento,
    horario_inicio_almoco,
    horario_fim_almoco,
    duracao_sessao_minutos,
    timezone_id,
    telefone_whatsapp,
    email_contato,
    ativo
) VALUES (
    '4eb2ab50-fbc2-4ef0-a4f8-f42d5dc4c2fe',
    'FisioGestão Clínica',
    'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY',
    '08:00',
    '18:00',
    '12:00',
    '13:00',
    50,
    'America/Campo_Grande',
    '67999999999',
    'contato@fisiogestao.local',
    TRUE
);
