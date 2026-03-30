package com.fisiogestao.agenda;

import com.fisiogestao.agenda.form.AgendaManualForm;
import com.fisiogestao.agenda.form.BloqueioAgendaForm;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.pacientes.service.PacienteService;
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

@Controller
@RequestMapping("/agenda")
@RequiredArgsConstructor
public class AgendaController {

    private final AgendaService agendaService;
    private final PacienteService pacienteService;

    @GetMapping
    public String agenda(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            Model model
    ) {
        LocalDate dataSelecionada = data != null ? data : LocalDate.now();
        prepararPaginaComFallback(model, dataSelecionada, new AgendaManualForm(), new BloqueioAgendaForm(), null);
        return "agenda/index";
    }

    @PostMapping("/sessoes")
    public String criarSessao(
            @Valid @ModelAttribute("agendaManualForm") AgendaManualForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        LocalDate dataSelecionada = form.getData() != null ? form.getData() : LocalDate.now();

        if (bindingResult.hasErrors()) {
            prepararPaginaComFallback(model, dataSelecionada, form, new BloqueioAgendaForm(), null);
            return "agenda/index";
        }

        try {
            agendaService.agendarSessaoManual(form);
            redirectAttributes.addFlashAttribute("successMessage", "Sessão agendada com sucesso.");
            return "redirect:/agenda?data=" + form.getData();
        } catch (BusinessException ex) {
            prepararPaginaComFallback(model, dataSelecionada, form, new BloqueioAgendaForm(), ex.getMessage());
            return "agenda/index";
        }
    }

    @PostMapping("/bloqueios")
    public String criarBloqueio(
            @Valid @ModelAttribute("bloqueioAgendaForm") BloqueioAgendaForm form,
            BindingResult bindingResult,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        LocalDate dataSelecionada = form.getData() != null ? form.getData() : LocalDate.now();

        if (bindingResult.hasErrors()) {
            prepararPaginaComFallback(model, dataSelecionada, new AgendaManualForm(), form, null);
            return "agenda/index";
        }

        try {
            agendaService.criarBloqueioAgenda(form, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Bloqueio criado com sucesso.");
            return "redirect:/agenda?data=" + form.getData();
        } catch (BusinessException ex) {
            prepararPaginaComFallback(model, dataSelecionada, new AgendaManualForm(), form, ex.getMessage());
            return "agenda/index";
        }
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
            BloqueioAgendaForm bloqueioForm,
            String errorMessage
    ) {
        try {
            prepararPagina(model, data, agendaForm, bloqueioForm);
            model.addAttribute("agendaConfigurada", true);
            if (errorMessage != null) {
                model.addAttribute("errorMessage", errorMessage);
            }
        } catch (BusinessException ex) {
            if (agendaForm.getData() == null) {
                agendaForm.setData(data);
            }
            if (bloqueioForm.getData() == null) {
                bloqueioForm.setData(data);
            }

            model.addAttribute("agendaConfigurada", false);
            model.addAttribute("dataSelecionada", data);
            model.addAttribute("configuracaoClinica", null);
            model.addAttribute("slots", List.of());
            model.addAttribute("sessoes", List.of());
            model.addAttribute("horariosBloqueados", List.of());
            model.addAttribute("bloqueiosAgenda", List.of());
            model.addAttribute("pacientes", List.of());
            model.addAttribute("agendaManualForm", agendaForm);
            model.addAttribute("bloqueioAgendaForm", bloqueioForm);
            model.addAttribute("errorMessage", errorMessage != null ? errorMessage : ex.getMessage());
        }
    }

    private void prepararPagina(Model model, LocalDate data, AgendaManualForm agendaForm, BloqueioAgendaForm bloqueioForm) {
        if (agendaForm.getData() == null) {
            agendaForm.setData(data);
        }
        if (bloqueioForm.getData() == null) {
            bloqueioForm.setData(data);
        }
        model.addAttribute("dataSelecionada", data);
        model.addAttribute("configuracaoClinica", agendaService.obterConfiguracaoAtiva());
        model.addAttribute("slots", agendaService.gerarSlotsDisponiveis(data));
        model.addAttribute("sessoes", agendaService.listarSessoesDoDia(data));
        model.addAttribute("horariosBloqueados", agendaService.listarHorariosBloqueadosDoDia(data));
        model.addAttribute("bloqueiosAgenda", agendaService.listarBloqueiosAgendaDoDia(data));
        model.addAttribute("pacientes", pacienteService.listarAtivos());
        model.addAttribute("agendaManualForm", agendaForm);
        model.addAttribute("bloqueioAgendaForm", bloqueioForm);
    }
}
