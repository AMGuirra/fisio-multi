package com.fisiogestao.agenda;

import com.fisiogestao.agenda.form.BloqueioAgendaCadastroForm;
import com.fisiogestao.agenda.form.BloqueioAgendaFiltroForm;
import com.fisiogestao.agenda.form.ConfiguracaoClinicaForm;
import com.fisiogestao.agenda.service.BloqueioAgendaService;
import com.fisiogestao.agenda.service.ConfiguracaoClinicaService;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Mantém as ações de cadastro, edição e cancelamento dos bloqueios de agenda,
 * agora servidas como POSTs sob /configuracoes/clinica/bloqueios. O GET legado
 * em /bloqueios redireciona para a página unificada de Configuração Operacional.
 */
@Controller
@RequiredArgsConstructor
public class BloqueiosController {

    private final BloqueioAgendaService bloqueioAgendaService;
    private final ConfiguracaoClinicaService configuracaoClinicaService;

    @GetMapping("/bloqueios")
    public String redirecionarPaginaLegada() {
        return "redirect:/configuracoes/clinica";
    }

    @PostMapping("/configuracoes/clinica/bloqueios")
    public String criar(
            @Valid @ModelAttribute("bloqueioForm") BloqueioAgendaCadastroForm form,
            BindingResult bindingResult,
            @ModelAttribute("filtroForm") BloqueioAgendaFiltroForm filtroForm,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararPaginaConfig(model, filtroPadrao(filtroForm), form, null);
            return "configuracoes/clinica";
        }
        try {
            bloqueioAgendaService.criar(form, usuario(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Bloqueio cadastrado com sucesso.");
            return "redirect:/configuracoes/clinica";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPaginaConfig(model, filtroPadrao(filtroForm), form, null);
            return "configuracoes/clinica";
        }
    }

    @PostMapping("/configuracoes/clinica/bloqueios/{id}/editar")
    public String editar(
            @PathVariable UUID id,
            @Valid @ModelAttribute("bloqueioForm") BloqueioAgendaCadastroForm form,
            BindingResult bindingResult,
            @ModelAttribute("filtroForm") BloqueioAgendaFiltroForm filtroForm,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararPaginaConfig(model, filtroPadrao(filtroForm), form, id);
            return "configuracoes/clinica";
        }
        try {
            bloqueioAgendaService.atualizar(id, form, usuario(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Bloqueio atualizado com sucesso.");
            return "redirect:/configuracoes/clinica";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPaginaConfig(model, filtroPadrao(filtroForm), form, id);
            return "configuracoes/clinica";
        }
    }

    @PostMapping("/configuracoes/clinica/bloqueios/{id}/cancelar")
    public String cancelar(
            @PathVariable UUID id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            bloqueioAgendaService.cancelar(id, usuario(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Bloqueio cancelado com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/configuracoes/clinica";
    }

    private void prepararPaginaConfig(Model model, BloqueioAgendaFiltroForm filtroForm, BloqueioAgendaCadastroForm form, UUID editarId) {
        ConfiguracaoClinicaForm configuracaoForm = configuracaoClinicaService.carregarFormulario();
        model.addAttribute("configuracaoClinicaForm", configuracaoForm);
        model.addAttribute("diasSemana", List.of(
                new ConfiguracaoClinicaController.DiaSemanaOption(DayOfWeek.MONDAY, "Segunda-feira"),
                new ConfiguracaoClinicaController.DiaSemanaOption(DayOfWeek.TUESDAY, "Terça-feira"),
                new ConfiguracaoClinicaController.DiaSemanaOption(DayOfWeek.WEDNESDAY, "Quarta-feira"),
                new ConfiguracaoClinicaController.DiaSemanaOption(DayOfWeek.THURSDAY, "Quinta-feira"),
                new ConfiguracaoClinicaController.DiaSemanaOption(DayOfWeek.FRIDAY, "Sexta-feira"),
                new ConfiguracaoClinicaController.DiaSemanaOption(DayOfWeek.SATURDAY, "Sábado"),
                new ConfiguracaoClinicaController.DiaSemanaOption(DayOfWeek.SUNDAY, "Domingo")
        ));
        model.addAttribute("timezonesClinica", configuracaoClinicaService.listarTimezonesSuportados());

        model.addAttribute("filtroForm", filtroForm);
        model.addAttribute("bloqueioForm", form);
        model.addAttribute("bloqueios", bloqueioAgendaService.listarPorFiltro(filtroForm));
        model.addAttribute("resumoBloqueios", bloqueioAgendaService.resumo(filtroForm));
        if (!model.containsAttribute("editMode")) {
            model.addAttribute("editMode", editarId != null);
        }
        if (editarId != null) {
            model.addAttribute("editingId", editarId);
            if (!model.containsAttribute("bloqueiosRelacionados")) {
                model.addAttribute("bloqueiosRelacionados", bloqueioAgendaService.carregarParaEdicao(editarId).grupo());
            }
        } else if (!model.containsAttribute("bloqueiosRelacionados")) {
            model.addAttribute("bloqueiosRelacionados", java.util.List.of());
        }
    }

    private BloqueioAgendaFiltroForm filtroPadrao(BloqueioAgendaFiltroForm filtroForm) {
        if (filtroForm == null) {
            filtroForm = new BloqueioAgendaFiltroForm();
        }
        if (filtroForm.getDataInicial() == null) {
            filtroForm.setDataInicial(LocalDate.now().minusDays(7));
        }
        if (filtroForm.getDataFinal() == null) {
            filtroForm.setDataFinal(LocalDate.now().plusDays(45));
        }
        return filtroForm;
    }

    private String usuario(Authentication authentication) {
        return authentication != null ? authentication.getName() : "sistema";
    }
}
