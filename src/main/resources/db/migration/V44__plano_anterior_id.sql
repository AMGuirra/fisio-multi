ALTER TABLE plano_sessao
    ADD COLUMN plano_anterior_id UUID REFERENCES plano_sessao(id);
