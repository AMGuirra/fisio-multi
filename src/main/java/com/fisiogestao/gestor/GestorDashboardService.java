package com.fisiogestao.gestor;

import com.fisiogestao.cancelamento.SolicitacaoCancelamentoRepository;
import com.fisiogestao.cancelamento.StatusSolicitacaoCancelamento;
import com.fisiogestao.empresa.ConfiguracaoPlataformaService;
import com.fisiogestao.empresa.DiasUteisService;
import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.empresa.EmpresaRepository;
import com.fisiogestao.empresa.EmpresaStatus;
import com.fisiogestao.empresa.PeriodoTesteService;
import com.fisiogestao.suporte.SolicitacaoSuporteRepository;
import com.fisiogestao.suporte.StatusSolicitacaoSuporte;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/** Indicadores globais da plataforma (somente metadados de empresas; nenhum dado clínico). */
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class GestorDashboardService {

    private final EmpresaRepository empresaRepository;
    private final SolicitacaoSuporteRepository suporteRepository;
    private final SolicitacaoCancelamentoRepository cancelamentoRepository;
    private final NotificacaoGestorRepository notificacaoRepository;
    private final ConfiguracaoPlataformaService configuracaoPlataformaService;
    private final DiasUteisService diasUteisService;

    @Transactional(readOnly = true)
    public Painel carregar() {
        OffsetDateTime agora = OffsetDateTime.now();
        LocalDate hoje = PeriodoTesteService.hoje();
        int diasAviso = configuracaoPlataformaService.diasUteisAvisoVencimento();
        LocalDate limiteAviso = diasUteisService.calcularDataFinal(hoje.plusDays(1), diasAviso);
        OffsetDateTime fimJanelaAviso = limiteAviso.plusDays(1).atStartOfDay(PeriodoTesteService.zonaPlataforma()).toOffsetDateTime();
        OffsetDateTime inicioMes = hoje.withDayOfMonth(1).atStartOfDay(PeriodoTesteService.zonaPlataforma()).toOffsetDateTime();

        List<EmpresaEntity> vencendo = empresaRepository
                .findByStatusAndDataFimTesteBetweenOrderByDataFimTesteAsc(EmpresaStatus.TESTE, agora, fimJanelaAviso);

        return new Painel(
                empresaRepository.count(),
                empresaRepository.countByStatus(EmpresaStatus.ATIVO),
                empresaRepository.countByStatus(EmpresaStatus.TESTE),
                empresaRepository.countByStatus(EmpresaStatus.BLOQUEADO),
                empresaRepository.countByStatus(EmpresaStatus.INATIVO),
                empresaRepository.countByCreatedAtGreaterThanEqual(inicioMes),
                vencendo.size(),
                empresaRepository.countByStatusAndDataFimTesteBefore(EmpresaStatus.TESTE, agora),
                suporteRepository.countByStatus(StatusSolicitacaoSuporte.PENDENTE),
                cancelamentoRepository.countByStatusIn(EnumSet.of(
                        StatusSolicitacaoCancelamento.PENDENTE, StatusSolicitacaoCancelamento.EM_ANALISE)),
                diasAviso,
                vencendo,
                empresaRepository.findTop8ByOrderByCreatedAtDesc(),
                notificacaoRepository.findTop20ByOrderByCreatedAtDesc(),
                notificacaoRepository.countByLidaFalse()
        );
    }

    @Transactional
    public void marcarNotificacaoComoLida(UUID id) {
        notificacaoRepository.findById(id).ifPresent(n -> {
            n.setLida(true);
            notificacaoRepository.save(n);
        });
    }

    @Transactional
    public void marcarTodasComoLidas() {
        notificacaoRepository.findAll().stream()
                .filter(n -> !Boolean.TRUE.equals(n.getLida()))
                .forEach(n -> n.setLida(true));
    }

    public record Painel(
            long totalEmpresas,
            long ativas,
            long emTeste,
            long bloqueadas,
            long inativas,
            long novosCadastrosMes,
            long testesVencendo,
            long testesExpirados,
            long suportePendente,
            long cancelamentosPendentes,
            int diasAvisoVencimento,
            List<EmpresaEntity> empresasVencendo,
            List<EmpresaEntity> cadastrosRecentes,
            List<NotificacaoGestorEntity> notificacoes,
            long notificacoesNaoLidas
    ) {
    }
}
