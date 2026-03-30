-- Seed opcional para ambiente de homologacao (HML)
-- Nao e migration Flyway automatica. Execute manualmente apos subir a base.
-- Data de criacao: 17/02/2026

BEGIN;

-- 1) Ocupacoes padrao para todas empresas
INSERT INTO ocupacao_responsavel (codigo_cnpj, codigo, descricao, ativo)
SELECT e.codigo_cnpj, x.codigo, x.descricao, TRUE
FROM empresa e
CROSS JOIN (
    VALUES
    ('ENGENHEIRO', 'Engenheiro'),
    ('MESTRE_OBRA', 'Mestre de Obra'),
    ('PEDREIRO', 'Pedreiro'),
    ('ASSISTENTE', 'Assistente')
) AS x(codigo, descricao)
ON CONFLICT (codigo_cnpj, codigo) DO NOTHING;

-- 2) Cliente exemplo por empresa (PJ)
INSERT INTO cliente (
    codigo_cnpj,
    codigo_cliente,
    tipo,
    cpf_cnpj,
    razao_social,
    nome,
    email,
    telefone,
    responsavel_nome,
    responsavel_cpf,
    ocupacao_responsavel_id,
    inativo_em
)
SELECT
    e.codigo_cnpj,
    '000998',
    'PESSOA_JURIDICA',
    '22333444000171',
    'Cliente Homologacao Obras Ltda',
    'Cliente Homologacao',
    'cliente.hml@demo.local',
    '11999999999',
    'Mariana Responsavel',
    '12345678901',
    (
        SELECT o.id
        FROM ocupacao_responsavel o
        WHERE o.codigo_cnpj = e.codigo_cnpj
          AND o.codigo = 'ENGENHEIRO'
        LIMIT 1
    ),
    NULL
FROM empresa e
WHERE NOT EXISTS (
    SELECT 1 FROM cliente c
    WHERE c.codigo_cnpj = e.codigo_cnpj
      AND c.codigo_cliente = '000998'
);

-- 3) Enderecos obrigatorios do cliente exemplo
INSERT INTO endereco (
    codigo_cnpj,
    codigo_endereco,
    codigo_cliente,
    tipo,
    cep,
    logradouro,
    numero,
    complemento,
    bairro,
    cidade,
    uf,
    principal
)
SELECT
    c.codigo_cnpj,
    'END890001',
    c.codigo_cliente,
    'CLIENTE',
    '01001000',
    'Rua Cliente Homologacao',
    '100',
    'Sala 1',
    'Centro',
    'Sao Paulo',
    'SP',
    TRUE
FROM cliente c
WHERE c.codigo_cliente = '000998'
  AND NOT EXISTS (
    SELECT 1 FROM endereco e
    WHERE e.codigo_cnpj = c.codigo_cnpj
      AND e.codigo_cliente = c.codigo_cliente
      AND e.tipo = 'CLIENTE'
  );

INSERT INTO endereco (
    codigo_cnpj,
    codigo_endereco,
    codigo_cliente,
    tipo,
    cep,
    logradouro,
    numero,
    complemento,
    bairro,
    cidade,
    uf,
    principal
)
SELECT
    c.codigo_cnpj,
    'END890002',
    c.codigo_cliente,
    'COBRANCA',
    '04567000',
    'Avenida Cobranca Exemplo',
    '200',
    NULL,
    'Vila Financeira',
    'Sao Paulo',
    'SP',
    FALSE
FROM cliente c
WHERE c.codigo_cliente = '000998'
  AND NOT EXISTS (
    SELECT 1 FROM endereco e
    WHERE e.codigo_cnpj = c.codigo_cnpj
      AND e.codigo_cliente = c.codigo_cliente
      AND e.tipo = 'COBRANCA'
  );

INSERT INTO endereco (
    codigo_cnpj,
    codigo_endereco,
    codigo_cliente,
    tipo,
    cep,
    logradouro,
    numero,
    complemento,
    bairro,
    cidade,
    uf,
    principal
)
SELECT
    c.codigo_cnpj,
    'END890003',
    c.codigo_cliente,
    'OBRA',
    '13010000',
    'Rua Obra Exemplo',
    '300',
    'Condominio A',
    'Distrito Industrial',
    'Campinas',
    'SP',
    FALSE
FROM cliente c
WHERE c.codigo_cliente = '000998'
  AND NOT EXISTS (
    SELECT 1 FROM endereco e
    WHERE e.codigo_cnpj = c.codigo_cnpj
      AND e.codigo_cliente = c.codigo_cliente
      AND e.tipo = 'OBRA'
  );

-- 4) Telefones adicionais
INSERT INTO cliente_telefone (
    codigo_cnpj,
    codigo_cliente,
    telefone,
    whatsapp,
    principal,
    observacao
)
SELECT c.codigo_cnpj, c.codigo_cliente, '1132121212', TRUE, TRUE, 'Comercial'
FROM cliente c
WHERE c.codigo_cliente = '000998'
  AND NOT EXISTS (
    SELECT 1 FROM cliente_telefone t
    WHERE t.codigo_cnpj = c.codigo_cnpj
      AND t.codigo_cliente = c.codigo_cliente
      AND t.telefone = '1132121212'
  );

INSERT INTO cliente_telefone (
    codigo_cnpj,
    codigo_cliente,
    telefone,
    whatsapp,
    principal,
    observacao
)
SELECT c.codigo_cnpj, c.codigo_cliente, '11977776666', TRUE, FALSE, 'Financeiro'
FROM cliente c
WHERE c.codigo_cliente = '000998'
  AND NOT EXISTS (
    SELECT 1 FROM cliente_telefone t
    WHERE t.codigo_cnpj = c.codigo_cnpj
      AND t.codigo_cliente = c.codigo_cliente
      AND t.telefone = '11977776666'
  );

-- 5) E-mails adicionais
INSERT INTO cliente_email (
    codigo_cnpj,
    codigo_cliente,
    email,
    principal,
    observacao
)
SELECT c.codigo_cnpj, c.codigo_cliente, 'contato@cliente-hml.local', TRUE, 'Contato geral'
FROM cliente c
WHERE c.codigo_cliente = '000998'
  AND NOT EXISTS (
    SELECT 1 FROM cliente_email e
    WHERE e.codigo_cnpj = c.codigo_cnpj
      AND e.codigo_cliente = c.codigo_cliente
      AND lower(e.email) = lower('contato@cliente-hml.local')
  );

INSERT INTO cliente_email (
    codigo_cnpj,
    codigo_cliente,
    email,
    principal,
    observacao
)
SELECT c.codigo_cnpj, c.codigo_cliente, 'financeiro@cliente-hml.local', FALSE, 'Financeiro'
FROM cliente c
WHERE c.codigo_cliente = '000998'
  AND NOT EXISTS (
    SELECT 1 FROM cliente_email e
    WHERE e.codigo_cnpj = c.codigo_cnpj
      AND e.codigo_cliente = c.codigo_cliente
      AND lower(e.email) = lower('financeiro@cliente-hml.local')
  );

COMMIT;

