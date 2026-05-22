package com.fisiogestao.dashboard;

import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.config.ClinicTimezoneHolder;
import com.fisiogestao.financeiro.service.FinanceiroService;
import com.fisiogestao.pacientes.repository.ClienteRepository;
import com.fisiogestao.solicitacoes.repository.SolicitacaoAgendamentoRepository;
import com.fisiogestao.solicitacoes.SolicitacaoStatus;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardService.class);

    private final ClienteRepository clienteRepository;
    private final SessaoRepository sessaoRepository;
    private final AgendaService agendaService;
    private final SolicitacaoAgendamentoRepository solicitacaoAgendamentoRepository;
    private final FinanceiroService financeiroService;

    public DashboardSummary carregarResumo() {
        long pacientesAtivos = clienteRepository.countByAtivoTrue();

        long sessoesHoje = 0;
        try {
            sessoesHoje = agendaService.contarSessoesDoDia(LocalDate.now());
        } catch (BusinessException e) {
            LOGGER.warn("Nao foi possivel carregar a agenda do dia no dashboard: {}", e.getMessage());
        }

        YearMonth mesAtual = YearMonth.from(ClinicTimezoneHolder.today());
        OffsetDateTime inicioMes = mesAtual.atDay(1).atStartOfDay(ClinicTimezoneHolder.zoneId()).toOffsetDateTime();
        OffsetDateTime inicioProximoMes = mesAtual.plusMonths(1).atDay(1).atStartOfDay(ClinicTimezoneHolder.zoneId()).toOffsetDateTime();

        long sessoesDoMes = 0;
        try {
            sessoesDoMes = sessaoRepository.countNaoCanceladasNoPeriodo(inicioMes, inicioProximoMes);
        } catch (Exception e) {
            LOGGER.warn("Nao foi possivel contar sessoes do mes no dashboard", e);
        }

        long novosPacientesMes = 0;
        try {
            novosPacientesMes = clienteRepository
                    .countByAtivoTrueAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(inicioMes, inicioProximoMes);
        } catch (Exception e) {
            LOGGER.warn("Nao foi possivel contar novos pacientes do mes no dashboard", e);
        }

        long solicitacoesPendentes = solicitacaoAgendamentoRepository.countByStatus(SolicitacaoStatus.PENDENTE);
        BigDecimal receitasPendentes = BigDecimal.ZERO;
        BigDecimal despesasPendentes = BigDecimal.ZERO;

        try {
            receitasPendentes = financeiroService.totalReceitasPendentes();
            despesasPendentes = financeiroService.totalDespesasPendentes();
        } catch (Exception e) {
            LOGGER.warn("Nao foi possivel carregar os totais financeiros do dashboard", e);
        }

        return new DashboardSummary(
                pacientesAtivos,
                sessoesHoje,
                sessoesDoMes,
                novosPacientesMes,
                solicitacoesPendentes,
                receitasPendentes,
                despesasPendentes,
                receitasPendentes.subtract(despesasPendentes)
        );
    }

    public record DashboardSummary(
            long pacientesAtivos,
            long sessoesHoje,
            long sessoesDoMes,
            long novosPacientesMes,
            long solicitacoesPendentes,
            BigDecimal receitasPendentes,
            BigDecimal despesasPendentes,
            BigDecimal saldoProjetado
    ) {
    }
}
