-- Adiciona suporte ao status FALTA (falta sem cobrança).
-- O status FALTA_FATURADA (falta com cobrança) já existe.
-- A coluna status é VARCHAR, então nenhuma alteração de tipo é necessária.

-- Atualiza a view materializada do dashboard para distinguir faltas faturadas das não faturadas.
-- A view antiga conta apenas FALTA_FATURADA; adicionamos coluna para FALTA.
DROP MATERIALIZED VIEW IF EXISTS mv_dashboard_mensal;

CREATE MATERIALIZED VIEW mv_dashboard_mensal AS
SELECT
    DATE_TRUNC('month', data_hora_inicio)                                    AS mes,
    COUNT(*) FILTER (WHERE status <> 'CANCELADA')                            AS total_atendimentos,
    COUNT(*) FILTER (WHERE status = 'REALIZADA')                             AS total_realizadas,
    COUNT(*) FILTER (WHERE status = 'FALTA_FATURADA')                        AS total_faltas_faturadas,
    COUNT(*) FILTER (WHERE status = 'FALTA')                                 AS total_faltas,
    COALESCE(
        SUM(EXTRACT(EPOCH FROM (data_hora_fim - data_hora_inicio)) / 3600.0)
            FILTER (WHERE status <> 'CANCELADA'),
        0
    )                                                                        AS horas_agendadas_e_faturadas
FROM sessao
GROUP BY DATE_TRUNC('month', data_hora_inicio);

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_dashboard_mensal_mes
    ON mv_dashboard_mensal (mes);
