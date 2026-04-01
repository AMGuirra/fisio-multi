package com.fisiogestao.agenda;

import com.fisiogestao.agenda.form.AgendaManualForm;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.pacientes.PacoteSessaoEntity;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.pacientes.service.PacoteSessaoService;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
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
        prepararPaginaComFallback(model, dataSelecionada, criarFormularioPadrao(dataSelecionada, paciente), editar, null);
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

        if (bindingResult.hasErrors()) {
            prepararPaginaComFallback(model, dataSelecionada, form, null, null);
            return "agenda/index";
        }

        try {
            agendaService.agendarSessaoManual(form, authentication != null ? authentication.getName() : "Equipe da clínica");
            redirectAttributes.addFlashAttribute("successMessage", "Sessão agendada com sucesso.");
            return "redirect:/agenda?data=" + form.getData();
        } catch (BusinessException ex) {
            prepararPaginaComFallback(model, dataSelecionada, form, null, ex.getMessage());
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
        if (bindingResult.hasErrors()) {
            prepararPaginaComFallback(model, retorno, new AgendaManualForm(), id, "Revise os dados da sessão antes de salvar.");
            model.addAttribute("sessaoEdicaoForm", form);
            return "agenda/index";
        }

        try {
            agendaService.atualizarSessaoManual(id, form, authentication != null ? authentication.getName() : "Equipe da clínica");
            redirectAttributes.addFlashAttribute("successMessage", "Sessão atualizada com sucesso.");
        } catch (BusinessException ex) {
            prepararPaginaComFallback(model, retorno, new AgendaManualForm(), id, ex.getMessage());
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

    @PostMapping("/sessoes/{id}/realizar")
    public String concluirSessao(
            @PathVariable java.util.UUID id,
            @RequestParam(required = false) String observacoes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            RedirectAttributes redirectAttributes
    ) {
        agendaService.marcarComoRealizada(id, observacoes);
        redirectAttributes.addFlashAttribute("successMessage", "Sessão marcada como realizada.");
        LocalDate retorno = data != null ? data : LocalDate.now();
        return "redirect:/agenda?data=" + retorno;
    }

    private void prepararPaginaComFallback(
            Model model,
            LocalDate data,
            AgendaManualForm agendaForm,
            UUID sessaoEdicaoId,
            String errorMessage
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
}
