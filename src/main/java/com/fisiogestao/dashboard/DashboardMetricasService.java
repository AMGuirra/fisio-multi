package com.fisiogestao.dashboard;

import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.config.ClinicTimezoneHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.YearMonth;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Servico de inteligencia de negocio do dashboard.
 *
 * <p>As consultas usam SQL agregado para evitar carregar entidades completas.
 * A camada Java completa os meses sem movimento e calcula indicadores derivados
 * como capacidade e taxa de ocupacao.</p>
 */
@Service
@RequiredArgsConstructor
public class DashboardMetricasService {

    private static final int MESES_HISTORICO = 12;

    private final JdbcTemplate jdbc;
    private final ConfiguracaoClinicaRepository configuracaoClinicaRepository;

    /**
     * {@code true} quando o datasource ativo é H2 (ambiente de testes).
     * Detectado uma única vez na inicialização via URL do datasource —
     * evita abrir uma conexão do pool a cada chamada de métricas.
     */
    @Value("#{T(org.springframework.util.StringUtils).startsWithIgnoreCase('${spring.datasource.url:}', 'jdbc:h2')}")
    private boolean h2Dialect;

    public record OcupacaoMensal(
            String mes,
            double horasAgendadas,
            double capacidadeHoras,
            double taxaOcupacao,
            long totalSessoes,
            long faltasFaturadas
    ) {}

    public record ReceitaMensal(
            String mes,
            BigDecimal receitaAvulso,
            BigDecimal receitaPacote,
            BigDecimal totalReceita
    ) {}

    public record RetencaoPacotes(
            long totalPacientesAtivos,
            long pacientesComPacote,
            long pacientesComRenovacao,
            double taxaConversao,
            double taxaRenovacao
    ) {}

    public record AbsenteismoPaciente(
            String nomePaciente,
            long faltasFaturadas,
            long totalSessoesComputadas,
            double taxaAbsenteismo
    ) {}

    public record MetricasDashboard(
            List<OcupacaoMensal> ocupacaoMensal,
            List<ReceitaMensal> receitaMensal,
            RetencaoPacotes retencaoPacotes,
            List<AbsenteismoPaciente> rankingAbsenteismo
    ) {}

    /**
     * Carrega métricas históricas dos últimos 12 meses.
     *
     * <p>A anotação {@code @PreAuthorize} é a segunda linha de defesa:
     * garante que, mesmo que este método seja chamado fora do contexto HTTP
     * (ex.: via injeção direta por outro serviço), a verificação de role
     * ainda é enforced pelo Spring Security AOP.
     * A primeira linha de defesa é o {@code requestMatchers} em {@code SecurityConfig}.</p>
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCEIRO')")
    @Transactional(readOnly = true)
    public MetricasDashboard carregarMetricas() {
        List<YearMonth> meses = ultimosDozeMeses();
        OffsetDateTime inicioJanela = inicioDaJanela(meses.getFirst());

        return new MetricasDashboard(
                consultarOcupacaoMensal(meses, inicioJanela),
                consultarReceitaMensal(meses, inicioJanela),
                consultarRetencaoPacotes(),
                consultarRankingAbsenteismo(inicioJanela)
        );
    }

    private List<OcupacaoMensal> consultarOcupacaoMensal(List<YearMonth> meses, OffsetDateTime inicioJanela) {
        String duracaoHoras = h2Dialect
                ? "DATEDIFF('SECOND', data_hora_inicio, data_hora_fim) / 3600.0"
                : "EXTRACT(EPOCH FROM (data_hora_fim - data_hora_inicio)) / 3600.0";

        String sql = ("""
                SELECT
                    EXTRACT(YEAR FROM data_hora_inicio) AS ano,
                    EXTRACT(MONTH FROM data_hora_inicio) AS mes_num,
                    SUM(%s) AS horas_agendadas,
                    COUNT(*) AS total_sessoes,
                    COUNT(*) FILTER (WHERE status = 'FALTA_FATURADA') AS total_faltas_faturadas
                FROM sessao
                WHERE status <> 'CANCELADA'
                  AND data_hora_inicio >= ?
                GROUP BY ano, mes_num
                ORDER BY ano, mes_num
                """).formatted(duracaoHoras);

        Map<YearMonth, OcupacaoAgregada> agregados = new LinkedHashMap<>();
        jdbc.query(sql, (rs) -> {
            YearMonth mes = YearMonth.of(rs.getInt("ano"), rs.getInt("mes_num"));
            agregados.put(
                    mes,
                    new OcupacaoAgregada(
                            arredondar(rs.getDouble("horas_agendadas")),
                            rs.getLong("total_sessoes"),
                            rs.getLong("total_faltas_faturadas")
                    )
            );
        }, inicioJanela);

        ConfiguracaoClinicaEntity configuracao = configuracaoClinicaRepository
                .findFirstByAtivoTrueOrderByCreatedAtAsc()
                .orElse(null);

        return meses.stream()
                .map(mes -> {
                    OcupacaoAgregada agregado = agregados.getOrDefault(mes, new OcupacaoAgregada(0.0, 0L, 0L));
                    double capacidadeHoras = capacidadeHorasNoMes(mes, configuracao);
                    double taxaOcupacao = capacidadeHoras > 0
                            ? arredondar(agregado.horasAgendadas() / capacidadeHoras * 100.0)
                            : 0.0;

                    return new OcupacaoMensal(
                            mes.toString(),
                            agregado.horasAgendadas(),
                            capacidadeHoras,
                            taxaOcupacao,
                            agregado.totalSessoes(),
                            agregado.faltasFaturadas()
                    );
                })
                .toList();
    }

    private List<ReceitaMensal> consultarReceitaMensal(List<YearMonth> meses, OffsetDateTime inicioJanela) {
        String sql = """
                SELECT
                    EXTRACT(YEAR FROM data_competencia) AS ano,
                    EXTRACT(MONTH FROM data_competencia) AS mes_num,
                    COALESCE(SUM(CASE WHEN pacote_sessao_id IS NULL THEN valor ELSE 0 END), 0) AS receita_avulso,
                    COALESCE(SUM(CASE WHEN pacote_sessao_id IS NOT NULL THEN valor ELSE 0 END), 0) AS receita_pacote,
                    COALESCE(SUM(valor), 0) AS total_receita
                FROM lancamento_financeiro
                WHERE tipo = 'RECEITA'
                  AND status = 'PAGO'
                  AND data_competencia >= ?
                GROUP BY ano, mes_num
                ORDER BY ano, mes_num
                """;

        Map<YearMonth, ReceitaMensal> agregados = new LinkedHashMap<>();
        jdbc.query(sql, (rs) -> {
            YearMonth mes = YearMonth.of(rs.getInt("ano"), rs.getInt("mes_num"));
            agregados.put(mes, new ReceitaMensal(
                    mes.toString(),
                    rs.getBigDecimal("receita_avulso"),
                    rs.getBigDecimal("receita_pacote"),
                    rs.getBigDecimal("total_receita")
            ));
        }, inicioJanela);

        return meses.stream()
                .map(mes -> agregados.getOrDefault(
                        mes,
                        new ReceitaMensal(mes.toString(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
                ))
                .toList();
    }

    private RetencaoPacotes consultarRetencaoPacotes() {
        Long totalAtivos = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cliente WHERE ativo = true",
                Long.class
        );

        Long comPacote = jdbc.queryForObject(
                """
                SELECT COUNT(DISTINCT p.cliente_id)
                FROM pacote_sessao p
                JOIN cliente c ON c.id = p.cliente_id
                WHERE c.ativo = true
                """,
                Long.class
        );

        Long comRenovacao = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM (
                    SELECT p.cliente_id
                    FROM pacote_sessao p
                    JOIN cliente c ON c.id = p.cliente_id
                    WHERE c.ativo = true
                    GROUP BY p.cliente_id
                    HAVING COUNT(*) > 1
                ) sub
                """,
                Long.class
        );

        long ativos = valorOuZero(totalAtivos);
        long pacote = valorOuZero(comPacote);
        long renovacao = valorOuZero(comRenovacao);

        return new RetencaoPacotes(
                ativos,
                pacote,
                renovacao,
                ativos > 0 ? arredondar((double) pacote / ativos * 100.0) : 0.0,
                pacote > 0 ? arredondar((double) renovacao / pacote * 100.0) : 0.0
        );
    }

    private List<AbsenteismoPaciente> consultarRankingAbsenteismo(OffsetDateTime inicioJanela) {
        String sql = """
                SELECT
                    c.nome_completo,
                    COUNT(*) FILTER (WHERE s.status = 'FALTA_FATURADA')                             AS faltas_faturadas,
                    COUNT(*) FILTER (WHERE s.status IN ('REALIZADA', 'FALTA_FATURADA'))             AS total_computadas,
                    ROUND(
                        1.0 * COUNT(*) FILTER (WHERE s.status = 'FALTA_FATURADA') * 100 /
                        NULLIF(COUNT(*) FILTER (WHERE s.status IN ('REALIZADA', 'FALTA_FATURADA')), 0),
                        1
                    )                                                                               AS taxa_absenteismo
                FROM sessao s
                JOIN cliente c ON c.id = s.cliente_id
                WHERE s.data_hora_inicio >= ?
                  AND s.status <> 'CANCELADA'
                GROUP BY c.id, c.nome_completo
                HAVING COUNT(*) FILTER (WHERE s.status = 'FALTA_FATURADA') > 0
                ORDER BY faltas_faturadas DESC, taxa_absenteismo DESC
                LIMIT 10
                """;

        List<AbsenteismoPaciente> resultado = new java.util.ArrayList<>();
        jdbc.query(sql, (rs) -> {
            resultado.add(new AbsenteismoPaciente(
                    rs.getString("nome_completo"),
                    rs.getLong("faltas_faturadas"),
                    rs.getLong("total_computadas"),
                    rs.getDouble("taxa_absenteismo")
            ));
        }, inicioJanela);
        return resultado;
    }

    private List<YearMonth> ultimosDozeMeses() {
        YearMonth atual = YearMonth.from(ClinicTimezoneHolder.today());
        return IntStream.range(0, MESES_HISTORICO)
                .mapToObj(index -> atual.minusMonths(MESES_HISTORICO - 1L - index))
                .toList();
    }

    private OffsetDateTime inicioDaJanela(YearMonth primeiroMes) {
        return primeiroMes.atDay(1).atStartOfDay(ClinicTimezoneHolder.zoneId()).toOffsetDateTime();
    }

    private double capacidadeHorasNoMes(YearMonth mes, ConfiguracaoClinicaEntity configuracao) {
        if (configuracao == null) {
            return 0.0;
        }

        double horasPorDia = horasOperacionaisPorDia(configuracao);
        if (horasPorDia <= 0) {
            return 0.0;
        }

        Set<DayOfWeek> diasFuncionamento = configuracao.diasFuncionamentoSet();
        long diasUteisClinica = mes.atDay(1)
                .datesUntil(mes.plusMonths(1).atDay(1))
                .filter(dia -> diasFuncionamento.contains(dia.getDayOfWeek()))
                .count();

        return arredondar(diasUteisClinica * horasPorDia);
    }

    private double horasOperacionaisPorDia(ConfiguracaoClinicaEntity configuracao) {
        Duration expediente = Duration.between(configuracao.getHorarioAbertura(), configuracao.getHorarioFechamento());
        Duration almoco = Duration.ZERO;
        if (configuracao.getHorarioInicioAlmoco() != null && configuracao.getHorarioFimAlmoco() != null) {
            almoco = Duration.between(configuracao.getHorarioInicioAlmoco(), configuracao.getHorarioFimAlmoco());
        }
        return Math.max(0.0, (expediente.minus(almoco).toMinutes()) / 60.0);
    }

    private long valorOuZero(Long value) {
        return value == null ? 0L : value;
    }

    private double arredondar(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private record OcupacaoAgregada(double horasAgendadas, long totalSessoes, long faltasFaturadas) {}
}
