package com.fisiogestao.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Serviço de inteligência de negócio do dashboard.
 *
 * <p>Todas as queries são nativas (SQL direto) sobre o PostgreSQL para:
 * <ul>
 *   <li>evitar o carregamento de entidades JPA completas em memória;</li>
 *   <li>delegar os agregados para o banco, que os executa com índices;</li>
 *   <li>retornar apenas os campos necessários para os gráficos.</li>
 * </ul>
 * </p>
 *
 * <p>Os dados cobrem sempre os últimos 12 meses completos para viabilizar
 * gráficos de tendência com granularidade mensal.</p>
 */
@Service
@RequiredArgsConstructor
public class DashboardMetricasService {

    private final JdbcTemplate jdbc;

    // -------------------------------------------------------------------------
    // Records (DTOs imutáveis, Java 16+)
    // -------------------------------------------------------------------------

    /** Ocupação mensal: horas efetivamente agendadas por mês. */
    public record OcupacaoMensal(
            String mes,           // formato "YYYY-MM"
            double horasAgendadas,
            long totalSessoes
    ) {}

    /** Receita mensal segregada por tipo de atendimento. */
    public record ReceitaMensal(
            String mes,
            BigDecimal receitaAvulso,
            BigDecimal receitaPacote,
            BigDecimal totalReceita
    ) {}

    /** Índices de conversão e renovação de pacotes. */
    public record RetencaoPacotes(
            long totalPacientesAtivos,
            long pacientesComPacote,
            long pacientesComRenovacao,
            double taxaConversao,     // % de ativos que têm ao menos 1 pacote
            double taxaRenovacao      // % dos que têm pacote e fizeram mais de 1
    ) {}

    /** Agregado completo retornado para o endpoint de métricas. */
    public record MetricasDashboard(
            List<OcupacaoMensal> ocupacaoMensal,
            List<ReceitaMensal> receitaMensal,
            RetencaoPacotes retencaoPacotes
    ) {}

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public MetricasDashboard carregarMetricas() {
        return new MetricasDashboard(
                consultarOcupacaoMensal(),
                consultarReceitaMensal(),
                consultarRetencaoPacotes()
        );
    }

    // -------------------------------------------------------------------------
    // Queries nativas
    // -------------------------------------------------------------------------

    /**
     * Evolução mensal de horas agendadas nos últimos 12 meses.
     *
     * <p>Sessões canceladas são excluídas; apenas AGENDADA, REALIZADA e
     * AGUARDANDO_CONFIRMACAO entram no cálculo.</p>
     */
    private List<OcupacaoMensal> consultarOcupacaoMensal() {
        String sql = """
                SELECT
                    TO_CHAR(data_hora_inicio, 'YYYY-MM')                          AS mes,
                    ROUND(
                        SUM(EXTRACT(EPOCH FROM (data_hora_fim - data_hora_inicio)) / 3600.0)::numeric,
                        1
                    )                                                              AS horas_agendadas,
                    COUNT(*)                                                       AS total_sessoes
                FROM sessao
                WHERE status NOT IN ('CANCELADA')
                  AND data_hora_inicio >= NOW() - INTERVAL '12 months'
                GROUP BY mes
                ORDER BY mes
                """;

        return jdbc.query(sql, (rs, rowNum) -> new OcupacaoMensal(
                rs.getString("mes"),
                rs.getDouble("horas_agendadas"),
                rs.getLong("total_sessoes")
        ));
    }

    /**
     * Receita mensal recebida (status PAGO) segregada entre sessões avulsas
     * (sem pacote_sessao_id) e receita de pacotes (com pacote_sessao_id).
     *
     * <p>O padrão de agendamento avulso vs. pacote é determinado pela presença
     * do FK {@code pacote_sessao_id} no lançamento financeiro, que reflete
     * exatamente a origem da receita.</p>
     */
    private List<ReceitaMensal> consultarReceitaMensal() {
        String sql = """
                SELECT
                    TO_CHAR(data_competencia, 'YYYY-MM')                             AS mes,
                    COALESCE(SUM(valor) FILTER (WHERE pacote_sessao_id IS NULL), 0)  AS receita_avulso,
                    COALESCE(SUM(valor) FILTER (WHERE pacote_sessao_id IS NOT NULL), 0) AS receita_pacote,
                    COALESCE(SUM(valor), 0)                                          AS total_receita
                FROM lancamento_financeiro
                WHERE tipo    = 'RECEITA'
                  AND status  = 'PAGO'
                  AND data_competencia >= NOW() - INTERVAL '12 months'
                GROUP BY mes
                ORDER BY mes
                """;

        return jdbc.query(sql, (rs, rowNum) -> new ReceitaMensal(
                rs.getString("mes"),
                rs.getBigDecimal("receita_avulso"),
                rs.getBigDecimal("receita_pacote"),
                rs.getBigDecimal("total_receita")
        ));
    }

    /**
     * Índices de conversão e renovação de pacotes.
     *
     * <p>Conversão: porcentagem de pacientes ativos que já adquiriram ao menos um pacote.</p>
     * <p>Renovação: porcentagem dos pacientes com pacote que renovaram (mais de um pacote).</p>
     */
    private RetencaoPacotes consultarRetencaoPacotes() {
        long totalAtivos = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cliente WHERE ativo = true",
                Long.class
        );

        long comPacote = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT cliente_id) FROM pacote_sessao",
                Long.class
        );

        long comRenovacao = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM (
                    SELECT cliente_id
                    FROM pacote_sessao
                    GROUP BY cliente_id
                    HAVING COUNT(*) > 1
                ) sub
                """,
                Long.class
        );

        double taxaConversao = totalAtivos > 0
                ? (double) comPacote / totalAtivos * 100
                : 0.0;

        double taxaRenovacao = comPacote > 0
                ? (double) comRenovacao / comPacote * 100
                : 0.0;

        return new RetencaoPacotes(
                totalAtivos,
                comPacote,
                comRenovacao,
                Math.round(taxaConversao * 10.0) / 10.0,
                Math.round(taxaRenovacao * 10.0) / 10.0
        );
    }
}
