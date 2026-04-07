package com.fisiogestao.agenda;

import com.fisiogestao.agenda.dto.SessaoEdicaoDto;
import com.fisiogestao.agenda.form.AgendaManualForm;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.pacientes.PacoteSessaoEntity;
import com.fisiogestao.pacientes.PacoteSessaoStatus;
import com.fisiogestao.pacientes.form.PacoteConsumoForm;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.pacientes.service.PacoteSessaoService;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/agenda")
@RequiredArgsConstructor
public class AgendaController {

    private final AgendaService agendaService;
    private final PacienteService pacienteService;
    private final PacoteSessaoService pacoteSessaoService;

    @GetMapping
    public String agenda(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) UUID paciente,
            @RequestParam(required = false) UUID editar,
            Model model
    ) {
        LocalDate dataSelecionada = data != null ? data : LocalDate.now();
        AgendaManualForm agendaForm = resolverAgendaManualForm(model, dataSelecionada, paciente);
        PacoteConsumoForm pacoteAgendaForm = resolverPacoteAgendaForm(model, agendaForm.getClienteId(), dataSelecionada);
        boolean pacoteModalOpen = Boolean.TRUE.equals(model.asMap().get("pacoteModalOpen"));
        prepararPaginaComFallback(model, dataSelecionada, agendaForm, editar, null, pacoteAgendaForm, pacoteModalOpen);
        return "agenda/index";
    }

    @PostMapping("/sessoes")
    public String criarSessao(
            @Valid @ModelAttribute("agendaManualForm") AgendaManualForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        LocalDate dataSelecionada = form.getData() != null ? form.getData() : LocalDate.now();
        PacoteConsumoForm pacoteAgendaForm = criarPacoteAgendaFormPadrao(form.getClienteId(), dataSelecionada);

        if (bindingResult.hasErrors()) {
            prepararPaginaComFallback(model, dataSelecionada, form, null, null, pacoteAgendaForm, false);
            return "agenda/index";
        }

        try {
            agendaService.agendarSessaoManual(form, authentication != null ? authentication.getName() : "Equipe da clínica");
            redirectAttributes.addFlashAttribute("successMessage", "Sessão agendada com sucesso.");
            return "redirect:/agenda?data=" + form.getData();
        } catch (BusinessException ex) {
            prepararPaginaComFallback(model, dataSelecionada, form, null, ex.getMessage(), pacoteAgendaForm, false);
            return "agenda/index";
        }
    }

    @PostMapping("/pacotes")
    public String criarPacotePelaAgenda(
            @Valid @ModelAttribute("pacoteAgendaForm") PacoteConsumoForm form,
            BindingResult bindingResult,
            @RequestParam(required = false, name = "agendaPacoteSessaoId") UUID agendaPacoteSessaoId,
            @RequestParam(required = false, name = "editar") UUID sessaoEdicaoId,
            @RequestParam(required = false, name = "agendaData") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate agendaData,
            @RequestParam(required = false, name = "agendaHorario") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime agendaHorario,
            @RequestParam(required = false, name = "agendaTipoAtendimento") String agendaTipoAtendimento,
            @RequestParam(required = false, name = "agendaObservacoes") String agendaObservacoes,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        LocalDate dataSelecionada = agendaData != null ? agendaData : LocalDate.now();
        AgendaManualForm agendaForm = reconstruirAgendaManualForm(
                form.getClienteId(),
                agendaPacoteSessaoId,
                dataSelecionada,
                agendaHorario,
                agendaTipoAtendimento,
                agendaObservacoes
        );

        if (bindingResult.hasErrors()) {
            prepararPaginaComFallback(model, dataSelecionada, agendaForm, sessaoEdicaoId, null, form, true);
            return "agenda/index";
        }

        try {
            PacoteSessaoEntity salvo = pacoteSessaoService.criarViaCrud(form);
            agendaForm.setPacoteSessaoId(salvo.getId());
            redirectAttributes.addFlashAttribute("agendaManualForm", agendaForm);
        redirectAttributes.addFlashAttribute("successMessage", "Plano de tratamento criado e selecionado para o agendamento.");
            return construirDestinoAgenda(dataSelecionada, form.getClienteId(), sessaoEdicaoId);
        } catch (BusinessException ex) {
            prepararPaginaComFallback(model, dataSelecionada, agendaForm, sessaoEdicaoId, ex.getMessage(), form, true);
            return "agenda/index";
        }
    }

    @PostMapping("/sessoes/{id}")
    public String editarSessao(
            @PathVariable UUID id,
            @Valid @ModelAttribute("sessaoEdicaoForm") AgendaManualForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        LocalDate retorno = form.getData() != null ? form.getData() : LocalDate.now();
        PacoteConsumoForm pacoteAgendaForm = criarPacoteAgendaFormPadrao(form.getClienteId(), retorno);
        if (bindingResult.hasErrors()) {
            prepararPaginaComFallback(model, retorno, new AgendaManualForm(), id, "Revise os dados da sessão antes de salvar.", pacoteAgendaForm, false);
            model.addAttribute("sessaoEdicaoForm", form);
            return "agenda/index";
        }

        try {
            agendaService.atualizarSessaoManual(id, form, authentication != null ? authentication.getName() : "Equipe da clínica");
            redirectAttributes.addFlashAttribute("successMessage", "Sessão atualizada com sucesso.");
        } catch (BusinessException ex) {
            prepararPaginaComFallback(model, retorno, new AgendaManualForm(), id, ex.getMessage(), pacoteAgendaForm, false);
            model.addAttribute("sessaoEdicaoForm", form);
            return "agenda/index";
        }
        return "redirect:/agenda?data=" + retorno;
    }

    @PostMapping("/sessoes/{id}/cancelar")
    public String cancelarSessao(
            @PathVariable UUID id,
            @RequestParam(required = false) String observacoes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            RedirectAttributes redirectAttributes
    ) {
        LocalDate retorno = data != null ? data : LocalDate.now();
        try {
            agendaService.cancelarSessao(id, observacoes);
            redirectAttributes.addFlashAttribute("successMessage", "Sessão cancelada com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/agenda?data=" + retorno;
    }

    @PostMapping("/sessoes/{id}/reabrir")
    public String reverterRealizacao(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            RedirectAttributes redirectAttributes
    ) {
        LocalDate retorno = data != null ? data : LocalDate.now();
        try {
            agendaService.reverterRealizacao(id);
            redirectAttributes.addFlashAttribute("successMessage", "Sessão reaberta como agendada.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/agenda?data=" + retorno;
    }

    @PostMapping("/sessoes/{id}/realizar")
    public String concluirSessao(
            @PathVariable java.util.UUID id,
            @RequestParam(required = false) String observacoes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            RedirectAttributes redirectAttributes
    ) {
        LocalDate retorno = data != null ? data : LocalDate.now();
        try {
            agendaService.marcarComoRealizada(id, observacoes);
            redirectAttributes.addFlashAttribute("successMessage", "Sessão marcada como realizada.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/agenda?data=" + retorno;
    }

    @GetMapping("/sessoes/{id}/json")
    @ResponseBody
    public ResponseEntity<?> buscarSessaoJson(@PathVariable UUID id) {
        try {
            SessaoEntity sessao = agendaService.buscarSessao(id);
            ZoneId zoneId = ZoneId.of(agendaService.obterConfiguracaoAtiva().getTimezoneId());
            ZonedDateTime inicio = sessao.getDataHoraInicio().atZoneSameInstant(zoneId);
            SessaoEdicaoDto dto = SessaoEdicaoDto.of(
                    sessao.getId(),
                    sessao.getCliente().getId(),
                    sessao.getCliente().getNomeCompleto(),
                    sessao.getPacoteSessao() != null ? sessao.getPacoteSessao().getId() : null,
                    sessao.getPacoteSessao() != null ? sessao.getPacoteSessao().getNomePacote() : null,
                    inicio.toLocalDate(),
                    inicio.toLocalTime(),
                    sessao.getTipoAtendimento(),
                    sessao.getObservacoes(),
                    sessao.getStatus().name()
            );
            return ResponseEntity.ok(dto);
        } catch (BusinessException ex) {
            Map<String, String> error = new HashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @PostMapping("/sessoes/{id}/json")
    @ResponseBody
    public ResponseEntity<?> atualizarSessaoJson(
            @PathVariable UUID id,
            @RequestParam UUID clienteId,
            @RequestParam(required = false) UUID pacoteSessaoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horario,
            @RequestParam(required = false) String tipoAtendimento,
            @RequestParam(required = false) String observacoes,
            Authentication authentication
    ) {
        try {
            AgendaManualForm form = new AgendaManualForm();
            form.setClienteId(clienteId);
            form.setPacoteSessaoId(pacoteSessaoId);
            form.setData(data);
            form.setHorario(horario);
            form.setTipoAtendimento(tipoAtendimento);
            form.setObservacoes(observacoes);
            agendaService.atualizarSessaoManual(id, form, authentication != null ? authentication.getName() : "Equipe da clínica");
            Map<String, Object> success = new HashMap<>();
            success.put("success", true);
            success.put("message", "Sessão atualizada com sucesso.");
            return ResponseEntity.ok(success);
        } catch (BusinessException ex) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    private void prepararPaginaComFallback(
            Model model,
            LocalDate data,
            AgendaManualForm agendaForm,
            UUID sessaoEdicaoId,
            String errorMessage,
            PacoteConsumoForm pacoteAgendaForm,
            boolean pacoteModalOpen
    ) {
        try {
            prepararPagina(model, data, agendaForm, sessaoEdicaoId);
            model.addAttribute("agendaConfigurada", true);
            if (errorMessage != null) {
                model.addAttribute("errorMessage", errorMessage);
            }
        } catch (BusinessException ex) {
            if (agendaForm.getData() == null) {
                agendaForm.setData(data);
            }

            model.addAttribute("agendaConfigurada", false);
            model.addAttribute("dataSelecionada", data);
            model.addAttribute("configuracaoClinica", null);
            model.addAttribute("slots", List.of());
            model.addAttribute("sessoes", List.of());
            model.addAttribute("horariosBloqueados", List.of());
            model.addAttribute("bloqueiosAgenda", List.of());
            model.addAttribute("pacientes", List.of());
            model.addAttribute("pacotesAgenda", List.of());
            model.addAttribute("agendaManualForm", agendaForm);
            model.addAttribute("editMode", false);
            model.addAttribute("errorMessage", errorMessage != null ? errorMessage : ex.getMessage());
        }
        prepararContextoPacoteAgenda(model, data, agendaForm, pacoteAgendaForm, pacoteModalOpen);
    }

    private void prepararPagina(Model model, LocalDate data, AgendaManualForm agendaForm, UUID sessaoEdicaoId) {
        if (agendaForm.getData() == null) {
            agendaForm.setData(data);
        }
        AgendaManualForm sessaoEdicaoForm = null;
        SessaoEntity sessaoEmEdicao = null;
        if (sessaoEdicaoId != null) {
            sessaoEmEdicao = agendaService.buscarSessao(sessaoEdicaoId);
            sessaoEdicaoForm = mapearSessaoParaFormulario(sessaoEmEdicao);
        }

        List<PacoteSessaoEntity> pacotesAgenda = listarPacotesAgenda(sessaoEmEdicao);
        model.addAttribute("dataSelecionada", data);
        model.addAttribute("configuracaoClinica", agendaService.obterConfiguracaoAtiva());
        model.addAttribute("slots", agendaService.gerarSlotsDisponiveis(data, false));
        model.addAttribute("sessoes", agendaService.listarSessoesDoDia(data));
        model.addAttribute("horariosBloqueados", agendaService.listarHorariosBloqueadosDoDia(data));
        model.addAttribute("bloqueiosAgenda", agendaService.listarBloqueiosAgendaDoDia(data));
        model.addAttribute("pacientes", pacienteService.listarAtivos());
        model.addAttribute("pacotesAgenda", pacotesAgenda);
        model.addAttribute("agendaManualForm", agendaForm);
        model.addAttribute("editMode", sessaoEmEdicao != null);
        model.addAttribute("sessaoEmEdicao", sessaoEmEdicao);
        if (!model.containsAttribute("sessaoEdicaoForm")) {
            model.addAttribute("sessaoEdicaoForm", sessaoEdicaoForm != null ? sessaoEdicaoForm : new AgendaManualForm());
        }
    }

    private AgendaManualForm mapearSessaoParaFormulario(SessaoEntity sessao) {
        AgendaManualForm form = new AgendaManualForm();
        form.setClienteId(sessao.getCliente().getId());
        form.setPacoteSessaoId(sessao.getPacoteSessao() != null ? sessao.getPacoteSessao().getId() : null);
        form.setData(sessao.getDataHoraInicio().toLocalDate());
        form.setHorario(sessao.getDataHoraInicio().toLocalTime());
        form.setTipoAtendimento(sessao.getTipoAtendimento());
        form.setObservacoes(sessao.getObservacoes());
        return form;
    }

    private List<PacoteSessaoEntity> listarPacotesAgenda(SessaoEntity sessaoEmEdicao) {
        List<PacoteSessaoEntity> pacotes = new java.util.ArrayList<>(pacoteSessaoService.listarDisponiveisParaAgenda());
        if (sessaoEmEdicao != null && sessaoEmEdicao.getPacoteSessao() != null) {
            boolean presente = pacotes.stream().anyMatch(item -> item.getId().equals(sessaoEmEdicao.getPacoteSessao().getId()));
            if (!presente) {
                pacotes.add(sessaoEmEdicao.getPacoteSessao());
            }
        }
        return pacotes;
    }

    private AgendaManualForm criarFormularioPadrao(LocalDate dataSelecionada, UUID pacienteId) {
        AgendaManualForm form = new AgendaManualForm();
        form.setData(dataSelecionada);
        if (pacienteId != null) {
            form.setClienteId(pacienteId);
            pacoteSessaoService.buscarPrimeiroDisponivel(pacienteId)
                    .ifPresent(pacote -> form.setPacoteSessaoId(pacote.getId()));
        }
        return form;
    }

    private AgendaManualForm resolverAgendaManualForm(Model model, LocalDate dataSelecionada, UUID pacienteId) {
        Object existente = model.asMap().get("agendaManualForm");
        if (existente instanceof AgendaManualForm agendaForm) {
            if (agendaForm.getData() == null) {
                agendaForm.setData(dataSelecionada);
            }
            if (agendaForm.getClienteId() == null && pacienteId != null) {
                agendaForm.setClienteId(pacienteId);
                pacoteSessaoService.buscarPrimeiroDisponivel(pacienteId)
                        .ifPresent(pacote -> agendaForm.setPacoteSessaoId(pacote.getId()));
            }
            return agendaForm;
        }
        return criarFormularioPadrao(dataSelecionada, pacienteId);
    }

    private PacoteConsumoForm resolverPacoteAgendaForm(Model model, UUID pacienteId, LocalDate dataSelecionada) {
        Object existente = model.asMap().get("pacoteAgendaForm");
        if (existente instanceof PacoteConsumoForm pacoteAgendaForm) {
            preencherDefaultsPacoteAgenda(pacoteAgendaForm, pacienteId, dataSelecionada);
            return pacoteAgendaForm;
        }
        return criarPacoteAgendaFormPadrao(pacienteId, dataSelecionada);
    }

    private void prepararContextoPacoteAgenda(
            Model model,
            LocalDate dataSelecionada,
            AgendaManualForm agendaForm,
            PacoteConsumoForm pacoteAgendaForm,
            boolean pacoteModalOpen
    ) {
        PacoteConsumoForm form = pacoteAgendaForm != null
                ? pacoteAgendaForm
                : criarPacoteAgendaFormPadrao(agendaForm.getClienteId(), dataSelecionada);
        preencherDefaultsPacoteAgenda(form, agendaForm.getClienteId(), dataSelecionada);
        model.addAttribute("pacoteAgendaForm", form);
        model.addAttribute("pacoteModalOpen", pacoteModalOpen);
    }

    private void preencherDefaultsPacoteAgenda(PacoteConsumoForm form, UUID pacienteId, LocalDate dataSelecionada) {
        if (form.getClienteId() == null) {
            form.setClienteId(pacienteId);
        }
        if (form.getSessoesContratadas() == null) {
            form.setSessoesContratadas(10);
        }
        if (form.getSessoesConsumidas() == null) {
            form.setSessoesConsumidas(0);
        }
        if (form.getDataInicio() == null) {
            form.setDataInicio(dataSelecionada != null ? dataSelecionada : LocalDate.now());
        }
        if (form.getStatus() == null) {
            form.setStatus(PacoteSessaoStatus.ATIVO);
        }
    }

    private PacoteConsumoForm criarPacoteAgendaFormPadrao(UUID pacienteId, LocalDate dataSelecionada) {
        PacoteConsumoForm form = new PacoteConsumoForm();
        preencherDefaultsPacoteAgenda(form, pacienteId, dataSelecionada);
        return form;
    }

    private AgendaManualForm reconstruirAgendaManualForm(
            UUID clienteId,
            UUID pacoteSessaoId,
            LocalDate data,
            LocalTime horario,
            String tipoAtendimento,
            String observacoes
    ) {
        AgendaManualForm form = new AgendaManualForm();
        form.setClienteId(clienteId);
        form.setPacoteSessaoId(pacoteSessaoId);
        form.setData(data);
        form.setHorario(horario);
        form.setTipoAtendimento(tipoAtendimento);
        form.setObservacoes(observacoes);
        return form;
    }

    private String construirDestinoAgenda(LocalDate data, UUID pacienteId, UUID sessaoEdicaoId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/agenda");
        if (data != null) {
            builder.queryParam("data", data);
        }
        if (pacienteId != null) {
            builder.queryParam("paciente", pacienteId);
        }
        if (sessaoEdicaoId != null) {
            builder.queryParam("editar", sessaoEdicaoId);
        }
        return "redirect:" + builder.build().encode().toUriString();
    }
}
