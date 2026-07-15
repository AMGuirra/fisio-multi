package com.fisiogestao.calendario;

import com.fisiogestao.agenda.BloqueioAgendaEntity;
import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.pacientes.dto.PlanoDisponibilidadeDTO;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.pacientes.service.PlanoSessaoService;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import com.fisiogestao.solicitacoes.SolicitacaoStatus;
import com.fisiogestao.solicitacoes.form.AprovacaoSolicitacaoForm;
import com.fisiogestao.solicitacoes.form.ReagendarSolicitacaoForm;
import com.fisiogestao.solicitacoes.service.SolicitacaoAgendamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final PacienteService pacienteService;
    private final PlanoSessaoService planoSessaoService;
    private final SolicitacaoAgendamentoService solicitacaoAgendamentoService;

    @GetMapping
    public String index(
            @RequestParam(name = "mes", required = false) String mes,
            @RequestParam(name = "data", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(name = "clienteId", required = false) UUID clienteId,
            @RequestParam(name = "agendar", defaultValue = "false") boolean abrirAgendamento,
            Model model
    ) {
        YearMonth mesSelecionado = resolverMesSelecionado(mes, data);
        LocalDate dataSelecionada = resolverDataSelecionada(mesSelecionado, data);
        carregarPagina(model, mesSelecionado, dataSelecionada);
        model.addAttribute("abrirAgendamentoModal", abrirAgendamento);
        if (clienteId != null) {
            try {
                model.addAttribute("clienteDestaque",
                        pacienteService.buscarPorId(clienteId).getNomeCompleto());
            } catch (Exception ignored) {
            }
        }
        return "calendario/index";
    }

    @PostMapping("/solicitacoes/{id}/aprovar")
    public String aprovarSolicitacao(
            @PathVariable UUID id,
            @RequestParam(name = "mesContexto", required = false) String mesContexto,
            @RequestParam(name = "dataContexto", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataContexto,
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
        return redirectMes(mesSelecionado, dataContexto);
    }

    @PostMapping("/solicitacoes/{id}/rejeitar")
    public String rejeitarSolicitacao(
            @PathVariable UUID id,
            @RequestParam(name = "mesContexto", required = false) String mesContexto,
            @RequestParam(name = "dataContexto", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataContexto,
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
        return redirectMes(mesSelecionado, dataContexto);
    }

    @PostMapping("/solicitacoes/{id}/reagendar")
    public String reagendarSolicitacao(
            @PathVariable UUID id,
            @RequestParam(name = "mesContexto", required = false) String mesContexto,
            @RequestParam(name = "dataContexto", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataContexto,
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
            return redirectMes(mesSelecionado, dataContexto);
        }

        try {
            solicitacaoAgendamentoService.reagendarSolicitacao(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Solicitação reagendada com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Não foi possível reagendar a solicitação.");
        }
        return redirectMes(mesSelecionado, dataContexto);
    }

    @PostMapping("/solicitacoes/{id}/bloquear")
    public String bloquearSolicitacao(
            @PathVariable UUID id,
            @RequestParam(name = "mesContexto", required = false) String mesContexto,
            @RequestParam(name = "dataContexto", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataContexto,
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
        return redirectMes(mesSelecionado, dataContexto);
    }

    private void carregarPagina(Model model, YearMonth mesSelecionado, LocalDate dataSelecionada) {
        LocalDate inicio = mesSelecionado.atDay(1);
        LocalDate fim = mesSelecionado.plusMonths(1).atDay(1);

        List<SessaoEntity> sessoesMes = agendaService.listarSessoesNoPeriodo(inicio, fim);
        List<SessaoEntity> sessoesDia = agendaService.listarSessoesDoDia(dataSelecionada);
        List<SolicitacaoAgendamentoEntity> solicitacoesMes = solicitacaoAgendamentoService.listarNoPeriodo(inicio, fim);
        List<BloqueioAgendaEntity> bloqueiosMes = agendaService.listarBloqueiosAgendaNoPeriodo(inicio, fim);
        List<SolicitacaoAgendamentoEntity> pendentes = solicitacaoAgendamentoService.listarPendentes();

        long totalDia = sessoesDia.size();
        long realizadasDia = sessoesDia.stream().filter(s -> s.getStatus() == SessaoStatus.REALIZADA).count();
        long agendadasDia = sessoesDia.stream().filter(s -> s.getStatus() == SessaoStatus.AGENDADA).count();
        long canceladasDia = sessoesDia.stream().filter(s -> s.getStatus() == SessaoStatus.CANCELADA || s.getStatus() == SessaoStatus.FALTA_FATURADA || s.getStatus() == SessaoStatus.FALTA).count();

        model.addAttribute("resumoDia", new ResumoAgendaDia(totalDia, realizadasDia, agendadasDia, canceladasDia));
        model.addAttribute("mesSelecionado", mesSelecionado);
        model.addAttribute("mesAnterior", mesSelecionado.minusMonths(1));
        model.addAttribute("mesProximo", mesSelecionado.plusMonths(1));
        model.addAttribute("dataSelecionada", dataSelecionada);
        model.addAttribute("dataFimRecorrenciaPadrao", dataSelecionada.withDayOfMonth(dataSelecionada.lengthOfMonth()));
        model.addAttribute("dataMesAnterior", ajustarDataParaMes(dataSelecionada, mesSelecionado.minusMonths(1)));
        model.addAttribute("dataMesProximo", ajustarDataParaMes(dataSelecionada, mesSelecionado.plusMonths(1)));
        model.addAttribute("calendarioSemanas", montarSemanas(mesSelecionado, dataSelecionada, sessoesMes, solicitacoesMes, bloqueiosMes));
        model.addAttribute("sessoes", sessoesDia);
        model.addAttribute("pacientes", pacienteService.listarAtivos());
        model.addAttribute("planosAgenda", planoSessaoService.listarDisponiveisParaAgenda());
        List<com.fisiogestao.pacientes.PlanoSessaoEntity> planosDiagnostico = planoSessaoService.listarTodosParaDiagnosticoAgenda();
        // Para o dia selecionado no passado, o agendamento eh retroativo (registra um fato ja
        // ocorrido) e nao deve ser bloqueado pela vigencia do plano — apenas por saldo e status.
        boolean considerarVigencia = !dataSelecionada.isBefore(LocalDate.now());
        Map<String, PlanoDisponibilidadeDTO> diagnosticosPlanosAgenda = planosDiagnostico.stream()
                .collect(Collectors.toMap(
                        plano -> plano.getId().toString(),
                        plano -> planoSessaoService.diagnosticarPlano(plano.getId(), considerarVigencia)
                ));
        model.addAttribute("planosAgendaDiagnostico", planosDiagnostico);
        model.addAttribute("motivosPlanosAgenda", planosDiagnostico.stream()
                .collect(Collectors.toMap(
                        plano -> plano.getId().toString(),
                        plano -> diagnosticosPlanosAgenda.get(plano.getId().toString()).motivoIndisponibilidade() == null
                                ? ""
                                : diagnosticosPlanosAgenda.get(plano.getId().toString()).motivoIndisponibilidade()
                )));
        model.addAttribute("saldosLivresAgenda", planosDiagnostico.stream()
                .collect(Collectors.toMap(
                        plano -> plano.getId().toString(),
                        plano -> diagnosticosPlanosAgenda.get(plano.getId().toString()).sessoesLivresParaAgenda()
                )));
        model.addAttribute("pendentes", pendentes);
        model.addAttribute("bloqueiosAtivosMes", bloqueiosMes.stream()
                .sorted((a, b) -> a.getDataHoraInicio().compareTo(b.getDataHoraInicio()))
                .toList());
    }

    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_CHIPS_POR_DIA = 3;

    private List<List<CalendarioDiaView>> montarSemanas(
            YearMonth mesSelecionado,
            LocalDate dataSelecionada,
            List<SessaoEntity> sessoesMes,
            List<SolicitacaoAgendamentoEntity> solicitacoesMes,
            List<BloqueioAgendaEntity> bloqueiosMes
    ) {
        Map<LocalDate, Long> sessoesPorDia = countByDate(sessoesMes, sessao -> sessao.getDataHoraInicio().toLocalDate());
        Map<LocalDate, List<SessaoEntity>> sessoesPorDiaLista = sessoesMes.stream()
                .collect(Collectors.groupingBy(s -> s.getDataHoraInicio().toLocalDate()));
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
        int offset = primeiroDia.getDayOfWeek().getValue() % 7;
        for (int i = 0; i < offset; i++) {
            dias.add(CalendarioDiaView.vazio());
        }

        for (int dia = 1; dia <= mesSelecionado.lengthOfMonth(); dia++) {
            LocalDate data = mesSelecionado.atDay(dia);
            List<CalendarioDiaView.SessaoChipView> chips = sessoesPorDiaLista
                    .getOrDefault(data, List.of()).stream()
                    .sorted(Comparator.comparing(s -> s.getDataHoraInicio()))
                    .limit(MAX_CHIPS_POR_DIA)
                    .map(s -> new CalendarioDiaView.SessaoChipView(
                            s.getId(),
                            s.getDataHoraInicio().format(HORA_FORMATTER),
                            s.getCliente().getNomeCompleto(),
                            s.getStatus().name()
                    ))
                    .toList();
            dias.add(new CalendarioDiaView(
                    data,
                    true,
                    sessoesPorDia.getOrDefault(data, 0L),
                    solicitacoesPendentesPorDia.getOrDefault(data, 0L),
                    bloqueiosPorDia.getOrDefault(data, 0L),
                    resumoBloqueios(bloqueiosDetalhadosPorDia.getOrDefault(data, List.of())),
                    chips,
                    data.equals(LocalDate.now()),
                    data.equals(dataSelecionada)
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

    private YearMonth resolverMesSelecionado(String mes, LocalDate data) {
        if (mes != null && !mes.isBlank()) {
            return parseMes(mes);
        }
        if (data != null) {
            return YearMonth.from(data);
        }
        return YearMonth.now();
    }

    private LocalDate resolverDataSelecionada(YearMonth mesSelecionado, LocalDate data) {
        if (data == null) {
            return YearMonth.now().equals(mesSelecionado) ? LocalDate.now() : mesSelecionado.atDay(1);
        }
        if (YearMonth.from(data).equals(mesSelecionado)) {
            return data;
        }
        return ajustarDataParaMes(data, mesSelecionado);
    }

    private LocalDate ajustarDataParaMes(LocalDate base, YearMonth mesDestino) {
        int dia = Math.min(base.getDayOfMonth(), mesDestino.lengthOfMonth());
        return mesDestino.atDay(dia);
    }

    private String redirectMes(YearMonth mesSelecionado, LocalDate dataSelecionada) {
        if (dataSelecionada != null) {
            return "redirect:/calendario?data=" + dataSelecionada;
        }
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

    public record ResumoAgendaDia(long total, long realizadas, long agendadas, long canceladas) {}

    public record CalendarioDiaView(
            LocalDate data,
            boolean pertenceAoMes,
            long sessoes,
            long solicitacoesPendentes,
            long bloqueios,
            List<String> resumosBloqueio,
            List<SessaoChipView> sessaoChips,
            boolean hoje,
            boolean selecionado
    ) {
        public static CalendarioDiaView vazio() {
            return new CalendarioDiaView(null, false, 0, 0, 0, List.of(), List.of(), false, false);
        }

        public record SessaoChipView(UUID id, String horario, String nomePaciente, String status) {}
    }
}
