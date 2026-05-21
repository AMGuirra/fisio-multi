package com.fisiogestao.hoje;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.config.ClinicTimezoneHolder;
import com.fisiogestao.financeiro.LancamentoFinanceiroEntity;
import com.fisiogestao.financeiro.StatusLancamentoFinanceiro;
import com.fisiogestao.financeiro.service.FinanceiroService;
import com.fisiogestao.listaespera.ListaEsperaRepository;
import com.fisiogestao.listaespera.StatusListaEspera;
import com.fisiogestao.solicitacoes.SolicitacaoStatus;
import com.fisiogestao.solicitacoes.repository.SolicitacaoAgendamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HojeService {

    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final AgendaService agendaService;
    private final FinanceiroService financeiroService;
    private final SolicitacaoAgendamentoRepository solicitacaoRepository;
    private final ListaEsperaRepository listaEsperaRepository;

    public record SessaoDoDia(
            SessaoEntity sessao,
            String horaInicio,
            String horaFim,
            boolean passada,
            boolean podeMarcarRealizada,
            boolean podeCancelar
    ) {}

    public record LancamentoPendente(
            LancamentoFinanceiroEntity lancamento,
            String valorFormatado
    ) {}

    public record ResumoHoje(
            LocalDate data,
            String dataFormatada,
            String diaSemana,
            List<SessaoDoDia> sessoes,
            long totalSessoes,
            long realizadas,
            long agendadas,
            long canceladas,
            List<LancamentoPendente> lancamentosPendentes,
            long totalPendenteCount,
            long solicitacoesPendentes,
            long listaEsperaAguardando
    ) {}

    @Transactional(readOnly = true)
    public ResumoHoje resumoDeHoje() {
        ZoneId zone = ClinicTimezoneHolder.zoneId();
        LocalDate hoje = LocalDate.now(zone);

        List<SessaoEntity> sessoesBruto = agendaService.listarSessoesDoDia(hoje);
        ZonedDateTime agora = ZonedDateTime.now(zone);

        List<SessaoDoDia> sessoes = sessoesBruto.stream()
                .map(s -> {
                    ZonedDateTime inicio = s.getDataHoraInicio().atZoneSameInstant(zone);
                    ZonedDateTime fim    = s.getDataHoraFim().atZoneSameInstant(zone);
                    boolean passada = fim.isBefore(agora);
                    return new SessaoDoDia(
                            s,
                            inicio.format(FMT_HORA),
                            fim.format(FMT_HORA),
                            passada,
                            s.getStatus() == SessaoStatus.AGENDADA,
                            s.getStatus() == SessaoStatus.AGENDADA
                    );
                })
                .toList();

        long realizadas  = sessoesBruto.stream().filter(s -> s.getStatus() == SessaoStatus.REALIZADA).count();
        long agendadas   = sessoesBruto.stream().filter(s -> s.getStatus() == SessaoStatus.AGENDADA).count();
        long canceladas  = sessoesBruto.stream().filter(s -> s.getStatus() == SessaoStatus.CANCELADA).count();

        List<LancamentoFinanceiroEntity> lancamentosHoje = financeiroService.listarNoPeriodo(hoje, hoje);
        List<LancamentoPendente> pendentes = lancamentosHoje.stream()
                .filter(l -> l.getStatus() == StatusLancamentoFinanceiro.PENDENTE)
                .map(l -> new LancamentoPendente(l, formatarValor(l)))
                .toList();

        long solicitacoesPendentes  = solicitacaoRepository.countByStatus(SolicitacaoStatus.PENDENTE);
        long listaEsperaAguardando  = listaEsperaRepository.countByStatus(StatusListaEspera.AGUARDANDO);

        DateTimeFormatter fmtData    = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", java.util.Locale.forLanguageTag("pt-BR"));
        DateTimeFormatter fmtDiaSem  = DateTimeFormatter.ofPattern("EEEE", java.util.Locale.forLanguageTag("pt-BR"));
        String dataFormatada = hoje.format(fmtData);
        String diaSemana     = capitalize(hoje.format(fmtDiaSem));

        return new ResumoHoje(
                hoje, dataFormatada, diaSemana,
                sessoes, sessoesBruto.size(), realizadas, agendadas, canceladas,
                pendentes, pendentes.size(),
                solicitacoesPendentes, listaEsperaAguardando
        );
    }

    private String formatarValor(LancamentoFinanceiroEntity l) {
        return "R$ " + String.format("%,.2f", l.getValor())
                .replace(',', 'X').replace('.', ',').replace('X', '.');
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
