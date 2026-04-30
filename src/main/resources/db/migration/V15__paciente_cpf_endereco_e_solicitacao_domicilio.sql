ALTER TABLE cliente
    ADD COLUMN cpf VARCHAR(11),
    ADD COLUMN cep VARCHAR(8),
    ADD COLUMN rua VARCHAR(160),
    ADD COLUMN numero VARCHAR(20),
    ADD COLUMN bairro VARCHAR(120),
    ADD COLUMN cidade VARCHAR(120),
    ADD COLUMN estado VARCHAR(2);

ALTER TABLE solicitacao_agendamento
    ADD COLUMN cpf VARCHAR(11),
    ADD COLUMN local_atendimento VARCHAR(20) NOT NULL DEFAULT 'CLINICA',
    ADD COLUMN cep VARCHAR(8),
    ADD COLUMN rua VARCHAR(160),
    ADD COLUMN numero VARCHAR(20),
    ADD COLUMN bairro VARCHAR(120),
    ADD COLUMN cidade VARCHAR(120),
    ADD COLUMN estado VARCHAR(2);

CREATE UNIQUE INDEX uk_cliente_cpf
    ON cliente (cpf)
    WHERE cpf IS NOT NULL;

CREATE INDEX idx_solicitacao_agendamento_cpf
    ON solicitacao_agendamento (cpf);
