package com.fisiogestao.dashboard;

import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.financeiro.service.FinanceiroService;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.solicitacoes.repository.SolicitacaoAgendamentoRepository;
import com.fisiogestao.solicitacoes.SolicitacaoStatus;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.whatsapp.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PacienteService pacienteService;
    private final AgendaService agendaService;
    private final SolicitacaoAgendamentoRepository solicitacaoAgendamentoRepository;
    private final FinanceiroService financeiroService;
    private final WhatsAppService whatsAppService;

    public DashboardSummary carregarResumo() {
        long pacientesAtivos = pacienteService.listarAtivos().size();
        
        long sessoesHoje = 0;
        try {
            sessoesHoje = agendaService.listarSessoesDoDia(LocalDate.now()).size();
        } catch (BusinessException e) {
        }
        
        long solicitacoesPendentes = solicitacaoAgendamentoRepository.countByStatus(SolicitacaoStatus.PENDENTE);
        long mensagensPendentes = whatsAppService.totalPendentes();
        BigDecimal receitasPendentes = BigDecimal.ZERO;
        BigDecimal despesasPendentes = BigDecimal.ZERO;
        
        try {
            receitasPendentes = financeiroService.totalReceitasPendentes();
            despesasPendentes = financeiroService.totalDespesasPendentes();
        } catch (Exception e) {
        }

        return new DashboardSummary(
                pacientesAtivos,
                sessoesHoje,
                solicitacoesPendentes,
                mensagensPendentes,
                receitasPendentes,
                despesasPendentes,
                receitasPendentes.subtract(despesasPendentes)
        );
    }

    public record DashboardSummary(
            long pacientesAtivos,
            long sessoesHoje,
            long solicitacoesPendentes,
            long mensagensPendentes,
            BigDecimal receitasPendentes,
            BigDecimal despesasPendentes,
            BigDecimal saldoProjetado
    ) {
    }
}
