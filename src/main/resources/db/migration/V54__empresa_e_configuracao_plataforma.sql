-- V54: multiempresa — tabela central de empresas (tenants) e configuração global da plataforma.

CREATE TABLE empresa (
    id                           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome                         VARCHAR(160) NOT NULL,
    slug                         VARCHAR(80)  NOT NULL,
    nome_responsavel             VARCHAR(160),
    cpf                          VARCHAR(11),
    cnpj                         VARCHAR(14),
    email                        VARCHAR(160),
    telefone                     VARCHAR(20),
    status                       VARCHAR(20)  NOT NULL,
    motivo_bloqueio              TEXT,
    data_inicio_teste            TIMESTAMPTZ,
    data_fim_teste               TIMESTAMPTZ,
    quantidade_dias_uteis_teste  INTEGER,
    alerta_vencimento_teste_em   TIMESTAMPTZ,
    alerta_expiracao_teste_em    TIMESTAMPTZ,
    versao                       BIGINT       NOT NULL DEFAULT 0,
    created_at                   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_empresa_slug UNIQUE (slug),
    CONSTRAINT ck_empresa_slug_formato CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT ck_empresa_status CHECK (status IN ('ATIVO', 'TESTE', 'BLOQUEADO', 'INATIVO')),
    CONSTRAINT ck_empresa_cpf CHECK (cpf IS NULL OR cpf ~ '^[0-9]{11}$'),
    CONSTRAINT ck_empresa_cnpj CHECK (cnpj IS NULL OR cnpj ~ '^[0-9]{14}$'),
    CONSTRAINT ck_empresa_teste_completo CHECK (
        status <> 'TESTE'
        OR (data_inicio_teste IS NOT NULL AND data_fim_teste IS NOT NULL AND quantidade_dias_uteis_teste IS NOT NULL)
    ),
    CONSTRAINT ck_empresa_teste_periodo CHECK (
        data_inicio_teste IS NULL OR data_fim_teste IS NULL OR data_fim_teste >= data_inicio_teste
    ),
    CONSTRAINT ck_empresa_dias_teste CHECK (
        quantidade_dias_uteis_teste IS NULL OR quantidade_dias_uteis_teste BETWEEN 1 AND 3650
    )
);

-- Um CNPJ identifica uma única clínica na plataforma; CPF do responsável pode repetir (vários consultórios).
CREATE UNIQUE INDEX uk_empresa_cnpj ON empresa (cnpj) WHERE cnpj IS NOT NULL;
CREATE INDEX idx_empresa_status ON empresa (status);
CREATE INDEX idx_empresa_teste_fim ON empresa (data_fim_teste) WHERE status = 'TESTE';
CREATE INDEX idx_empresa_cpf ON empresa (cpf) WHERE cpf IS NOT NULL;
CREATE INDEX idx_empresa_created_at ON empresa (created_at DESC);

COMMENT ON TABLE empresa IS 'Clínicas/empresas clientes (tenants). Todo dado de negócio referencia empresa_id.';

-- Empresa legada: recebe todos os dados existentes antes do multiempresa.
INSERT INTO empresa (id, nome, slug, email, telefone, status)
SELECT
    '00000000-0000-0000-0000-000000000001'::uuid,
    'Clínica Principal',
    'clinica-principal',
    (SELECT LOWER(c.email_contato) FROM configuracao_clinica c WHERE c.ativo ORDER BY c.created_at LIMIT 1),
    (SELECT LEFT(regexp_replace(COALESCE(c.telefone_whatsapp, ''), '[^0-9]', '', 'g'), 20)
       FROM configuracao_clinica c WHERE c.ativo ORDER BY c.created_at LIMIT 1),
    'ATIVO';

CREATE TABLE configuracao_plataforma (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dias_uteis_teste_padrao     INTEGER     NOT NULL DEFAULT 15,
    dias_uteis_aviso_vencimento INTEGER     NOT NULL DEFAULT 3,
    email_notificacoes          VARCHAR(160),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_configuracao_plataforma_dias CHECK (dias_uteis_teste_padrao BETWEEN 1 AND 3650),
    CONSTRAINT ck_configuracao_plataforma_aviso CHECK (dias_uteis_aviso_vencimento BETWEEN 1 AND 3650)
);

-- Registro único: o índice sobre uma expressão constante impede uma segunda linha.
CREATE UNIQUE INDEX uk_configuracao_plataforma_unica ON configuracao_plataforma ((TRUE));

INSERT INTO configuracao_plataforma (id, dias_uteis_teste_padrao, dias_uteis_aviso_vencimento)
VALUES ('00000000-0000-0000-0000-0000000000a1', 15, 3);
