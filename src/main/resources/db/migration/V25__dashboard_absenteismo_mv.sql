-- Índice de suporte para queries de absenteísmo por paciente
CREATE INDEX IF NOT EXISTS idx_sessao_absenteismo_cliente
    ON sessao (cliente_id, status, data_hora_inicio);

-- Materialized view para acesso de ferramentas de BI externas (ex.: Metabase, Grafana).
-- A aplicação usa queries diretas com os índices V24 para todas as métricas em tempo real.
-- Refresque manualmente após grandes importações: REFRESH MATERIALIZED VIEW CONCURRENTLY mv_dashboard_mensal;
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_dashboard_mensal AS
SELECT
    DATE_TRUNC('month', data_hora_inicio)                                    AS mes,
    COUNT(*)       FILTER (WHERE status <> 'CANCELADA')                      AS total_atendimentos,
    COUNT(*)       FILTER (WHERE status = 'REALIZADA')                       AS total_realizadas,
    COUNT(*)       FILTER (WHERE status = 'FALTA_FATURADA')                  AS total_faltas_faturadas,
    COALESCE(
        SUM(EXTRACT(EPOCH FROM (data_hora_fim - data_hora_inicio)) / 3600.0)
            FILTER (WHERE status <> 'CANCELADA'),
        0
    )                                                                        AS horas_agendadas_e_faturadas
FROM sessao
GROUP BY DATE_TRUNC('month', data_hora_inicio);

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_dashboard_mensal_mes
    ON mv_dashboard_mensal (mes);
