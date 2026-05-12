CREATE INDEX IF NOT EXISTS idx_sessao_dashboard_ocupacao
    ON sessao (status, data_hora_inicio);

CREATE INDEX IF NOT EXISTS idx_lancamento_financeiro_dashboard_receita
    ON lancamento_financeiro (tipo, status, data_competencia, pacote_sessao_id);

CREATE INDEX IF NOT EXISTS idx_cliente_ativo
    ON cliente (ativo);
