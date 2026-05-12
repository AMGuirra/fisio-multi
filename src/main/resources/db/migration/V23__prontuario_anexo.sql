-- V23: Anexos do prontuário — laudos, radiografias e documentos clínicos por paciente
--
-- Cada registro armazena o binário do arquivo (BYTEA) diretamente no banco,
-- eliminando dependência de armazenamento externo e garantindo backup junto ao restante dos dados.
-- Tipos aceitos pela aplicação: PDF, JPG, PNG, WebP, DOCX (validado no service).
-- Limite por arquivo: 15 MB (validado no service e no multipart do Spring).

CREATE TABLE prontuario_anexo (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id          UUID        NOT NULL REFERENCES cliente(id),
    prontuario_soap_id  UUID        REFERENCES prontuario_soap(id) ON DELETE SET NULL,
    nome_original       VARCHAR(255) NOT NULL,
    tipo_mime           VARCHAR(100) NOT NULL,
    tamanho_bytes       BIGINT       NOT NULL,
    dados               BYTEA        NOT NULL,
    descricao           VARCHAR(500),
    criado_por          VARCHAR(80)  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_prontuario_anexo_cliente ON prontuario_anexo(cliente_id);
CREATE INDEX idx_prontuario_anexo_soap    ON prontuario_anexo(prontuario_soap_id);

COMMENT ON TABLE  prontuario_anexo                       IS 'Documentos clínicos anexados ao prontuário: laudos, radiografias, avaliações em PDF e imagens';
COMMENT ON COLUMN prontuario_anexo.prontuario_soap_id    IS 'Vínculo opcional com um registro SOAP específico';
COMMENT ON COLUMN prontuario_anexo.dados                 IS 'Conteúdo binário do arquivo armazenado como BYTEA no PostgreSQL';
COMMENT ON COLUMN prontuario_anexo.tamanho_bytes         IS 'Tamanho original do arquivo em bytes (máx. 15 MB por arquivo)';
