package com.fisiogestao.calendario;

import com.fisiogestao.agenda.BloqueioAgendaEntity;
import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import com.fisiogestao.solicitacoes.SolicitacaoStatus;
import com.fisiogestao.solicitacoes.form.AprovacaoSolicitacaoForm;
import com.fisiogestao.solicitacoes.form.ReagendarSolicitacaoForm;
import com.fisiogestao.solicitacoes.service.SolicitacaoAgendamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/calendario")
@RequiredArgsConstructor
public class CalendarioController {

    private final AgendaService agendaService;
    private final SolicitacaoAgendamentoService solicitacaoAgendamentoService;

    @GetMapping
    public String index(
            @RequestParam(name = "mes", required = false) String mes,
            Model model
    ) {
        YearMonth mesSelecionado = parseMes(mes);
        carregarPagina(model, mesSelecionado);
        return "calendario/index";
    }

    @PostMapping("/solicitacoes/{id}/aprovar")
    public String aprovarSolicitacao(
            @PathVariable UUID id,
            @RequestParam(name = "mesContexto", required = false) String mesContexto,
            RedirectAttributes redirectAttributes
    ) {
        YearMonth mesSelecionado = parseMes(mesContexto);
        try {
            solicitacaoAgendamentoService.aprovarSolicitacao(id, new AprovacaoSolicitacaoForm());
            redirectAttributes.addFlashAttribute("successMessage", "Solicitação aprovada.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Não foi possível aprovar a solicitação.");
        }
        return redirectMes(mesSelecionado);
    }

    @PostMapping("/solicitacoes/{id}/rejeitar")
    public String rejeitarSolicitacao(
            @PathVariable UUID id,
            @RequestParam(name = "mesContexto", required = false) String mesContexto,
            RedirectAttributes redirectAttributes
    ) {
        YearMonth mesSelecionado = parseMes(mesContexto);
        try {
            solicitacaoAgendamentoService.rejeitarSolicitacao(id);
            redirectAttributes.addFlashAttribute("successMessage", "Solicitação rejeitada.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Não foi possível rejeitar a solicitação.");
        }
        return redirectMes(mesSelecionado);
    }

    @PostMapping("/solicitacoes/{id}/reagendar")
    public String reagendarSolicitacao(
            @PathVariable UUID id,
            @RequestParam(name = "mesContexto", required = false) String mesContexto,
            @RequestParam("data") String data,
            @RequestParam("horario") String horario,
            @RequestParam(name = "motivo", required = false) String motivo,
            RedirectAttributes redirectAttributes
    ) {
        YearMonth mesSelecionado = parseMes(mesContexto);

        ReagendarSolicitacaoForm form = new ReagendarSolicitacaoForm();
        try {
            form.setData(LocalDate.parse(data));
            form.setHorario(LocalTime.parse(horario));
            form.setMotivo(motivo);
        } catch (DateTimeParseException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Data ou horário inválidos para reagendamento.");
            return redirectMes(mesSelecionado);
        }

        try {
            solicitacaoAgendamentoService.reagendarSolicitacao(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Solicitação reagendada com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Não foi possível reagendar a solicitação.");
        }
        return redirectMes(mesSelecionado);
    }

    @PostMapping("/solicitacoes/{id}/bloquear")
    public String bloquearSolicitacao(
            @PathVariable UUID id,
            @RequestParam(name = "mesContexto", required = false) String mesContexto,
            @RequestParam(name = "motivo", required = false) String motivo,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        YearMonth mesSelecionado = parseMes(mesContexto);
        try {
            String usuario = authentication != null ? authentication.getName() : "sistema";
            solicitacaoAgendamentoService.bloquearPeriodoDaSolicitacao(id, motivo, usuario);
            redirectAttributes.addFlashAttribute("successMessage", "Período da solicitação bloqueado e solicitação cancelada.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Não foi possível bloquear a solicitação.");
        }
        return redirectMes(mesSelecionado);
    }

    private void carregarPagina(Model model, YearMonth mesSelecionado) {
        LocalDate inicio = mesSelecionado.atDay(1);
        LocalDate fim = mesSelecionado.plusMonths(1).atDay(1);

        List<SessaoEntity> sessoesMes = agendaService.listarSessoesNoPeriodo(inicio, fim);
        List<SolicitacaoAgendamentoEntity> solicitacoesMes = solicitacaoAgendamentoService.listarNoPeriodo(inicio, fim);
        List<BloqueioAgendaEntity> bloqueiosMes = agendaService.listarBloqueiosAgendaNoPeriodo(inicio, fim);
        List<SolicitacaoAgendamentoEntity> pendentes = solicitacaoAgendamentoService.listarPendentes();

        model.addAttribute("mesSelecionado", mesSelecionado);
        model.addAttribute("mesAnterior", mesSelecionado.minusMonths(1));
        model.addAttribute("mesProximo", mesSelecionado.plusMonths(1));
        model.addAttribute("calendarioSemanas", montarSemanas(mesSelecionado, sessoesMes, solicitacoesMes, bloqueiosMes));
        model.addAttribute("pendentes", pendentes);
        model.addAttribute("bloqueiosAtivosMes", bloqueiosMes.stream()
                .sorted((a, b) -> a.getDataHoraInicio().compareTo(b.getDataHoraInicio()))
                .toList());
    }

    private List<List<CalendarioDiaView>> montarSemanas(
            YearMonth mesSelecionado,
            List<SessaoEntity> sessoesMes,
            List<SolicitacaoAgendamentoEntity> solicitacoesMes,
            List<BloqueioAgendaEntity> bloqueiosMes
    ) {
        Map<LocalDate, Long> sessoesPorDia = countByDate(sessoesMes, sessao -> sessao.getDataHoraInicio().toLocalDate());
        Map<LocalDate, Long> solicitacoesPendentesPorDia = solicitacoesMes.stream()
                .filter(sol -> sol.getStatus() == SolicitacaoStatus.PENDENTE)
                .collect(Collectors.groupingBy(sol -> sol.getDataHoraInicio().toLocalDate(), Collectors.counting()));
        Map<LocalDate, Long> bloqueiosPorDia = countByDate(bloqueiosMes, bloqueio -> bloqueio.getDataHoraInicio().toLocalDate());
        Map<LocalDate, List<BloqueioAgendaEntity>> bloqueiosDetalhadosPorDia = new HashMap<>();
        bloqueiosMes.forEach(bloqueio -> bloqueiosDetalhadosPorDia
                .computeIfAbsent(bloqueio.getDataHoraInicio().toLocalDate(), ignored -> new ArrayList<>())
                .add(bloqueio));

        ArrayList<CalendarioDiaView> dias = new ArrayList<>();
        LocalDate primeiroDia = mesSelecionado.atDay(1);
        int offset = primeiroDia.getDayOfWeek().getValue() - 1;
        for (int i = 0; i < offset; i++) {
            dias.add(CalendarioDiaView.vazio());
        }

        for (int dia = 1; dia <= mesSelecionado.lengthOfMonth(); dia++) {
            LocalDate data = mesSelecionado.atDay(dia);
            dias.add(new CalendarioDiaView(
                    data,
                    true,
                    sessoesPorDia.getOrDefault(data, 0L),
                    solicitacoesPendentesPorDia.getOrDefault(data, 0L),
                    bloqueiosPorDia.getOrDefault(data, 0L),
                    resumoBloqueios(bloqueiosDetalhadosPorDia.getOrDefault(data, List.of())),
                    data.equals(LocalDate.now())
            ));
        }

        while (dias.size() % 7 != 0) {
            dias.add(CalendarioDiaView.vazio());
        }

        ArrayList<List<CalendarioDiaView>> semanas = new ArrayList<>();
        for (int i = 0; i < dias.size(); i += 7) {
            semanas.add(dias.subList(i, i + 7));
        }
        return semanas;
    }

    private <T> Map<LocalDate, Long> countByDate(List<T> items, Function<T, LocalDate> dateExtractor) {
        return items.stream().collect(Collectors.groupingBy(dateExtractor, Collectors.counting()));
    }

    private YearMonth parseMes(String mes) {
        try {
            return mes == null || mes.isBlank() ? YearMonth.now() : YearMonth.parse(mes);
        } catch (DateTimeParseException ex) {
            return YearMonth.now();
        }
    }

    private String redirectMes(YearMonth mesSelecionado) {
        return "redirect:/calendario?mes=" + mesSelecionado;
    }

    private List<String> resumoBloqueios(List<BloqueioAgendaEntity> bloqueios) {
        return bloqueios.stream()
                .sorted((a, b) -> a.getDataHoraInicio().compareTo(b.getDataHoraInicio()))
                .limit(2)
                .map(bloqueio -> {
                    if (Boolean.TRUE.equals(bloqueio.getDiaInteiro())) {
                        return "Dia inteiro - " + bloqueio.getMotivo();
                    }
                    return bloqueio.getHoraInicio() + " às " + bloqueio.getHoraFim() + " - " + bloqueio.getMotivo();
                })
                .toList();
    }

    public record CalendarioDiaView(
            LocalDate data,
            boolean pertenceAoMes,
            long sessoes,
            long solicitacoesPendentes,
            long bloqueios,
            List<String> resumosBloqueio,
            boolean hoje
    ) {
        public static CalendarioDiaView vazio() {
            return new CalendarioDiaView(null, false, 0, 0, 0, List.of(), false);
        }
    }
}
