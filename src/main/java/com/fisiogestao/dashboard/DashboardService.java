package com.fisiogestao.dashboard;

import com.fisiogestao.agenda.service.AgendaService;
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

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardService.class);

    private final ClienteRepository clienteRepository;
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
                solicitacoesPendentes,
                receitasPendentes,
                despesasPendentes,
                receitasPendentes.subtract(despesasPendentes)
        );
    }

    public record DashboardSummary(
            long pacientesAtivos,
            long sessoesHoje,
            long solicitacoesPendentes,
            BigDecimal receitasPendentes,
            BigDecimal despesasPendentes,
            BigDecimal saldoProjetado
    ) {
    }
}
