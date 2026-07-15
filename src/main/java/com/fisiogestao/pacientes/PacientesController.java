package com.fisiogestao.pacientes;

import com.fisiogestao.pacientes.form.PacienteForm;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/pacientes")
@RequiredArgsConstructor
public class PacientesController {

    private final PacienteService pacienteService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String busca,
            @RequestParam(name = "novoPaciente", defaultValue = "false") boolean abrirNovoPacienteModal,
            Model model
    ) {
        Page<ClienteEntity> pacientesPage = pacienteService.listarAtivosPaginado(page, busca);
        prepararPagina(model, pacientesPage, new PacienteForm(), busca);
        model.addAttribute("abrirNovoPacienteModal", abrirNovoPacienteModal);
        return "pacientes/index";
    }

    @PostMapping
    public String criarPaciente(
            @Valid @ModelAttribute("pacienteForm") PacienteForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            Page<ClienteEntity> pacientesPage = pacienteService.listarAtivosPaginado(0, null);
            prepararPagina(model, pacientesPage, form, null);
            return "pacientes/index";
        }

        try {
            pacienteService.cadastrar(form);
            redirectAttributes.addFlashAttribute("successMessage", "Paciente cadastrado com sucesso.");
            return "redirect:/pacientes";
        } catch (BusinessException ex) {
            Page<ClienteEntity> pacientesPage = pacienteService.listarAtivosPaginado(0, null);
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, pacientesPage, form, null);
            return "pacientes/index";
        }
    }

    @PostMapping("/{id}/atualizar")
    public String atualizarPaciente(
            @PathVariable UUID id,
            @Valid @ModelAttribute("pacienteForm") PacienteForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            Page<ClienteEntity> pacientesPage = pacienteService.listarAtivosPaginado(0, null);
            prepararPagina(model, pacientesPage, form, null);
            model.addAttribute("errorMessage", "Corrija os campos inválidos antes de salvar.");
            return "pacientes/index";
        }

        try {
            pacienteService.atualizar(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Paciente atualizado com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/pacientes";
    }

    @PostMapping("/{id}/excluir")
    public String excluirPaciente(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            pacienteService.excluir(id);
            redirectAttributes.addFlashAttribute("successMessage", "Paciente e todos os registros vinculados foram excluídos com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/pacientes";
    }

    /**
     * Gera novo token para o portal do paciente, invalidando o link anterior.
     * Útil quando o paciente reportar que o link foi comprometido.
     */
    @PostMapping("/{id}/rotacionar-token-portal")
    public String rotacionarTokenPortal(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            pacienteService.rotacionarPortalToken(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Link do portal regenerado. O link anterior foi invalidado.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/pacientes";
    }

    private void prepararPagina(
            Model model,
            Page<ClienteEntity> pacientesPage,
            PacienteForm pacienteForm,
            String busca
    ) {
        model.addAttribute("pacientes", pacientesPage.getContent());
        model.addAttribute("pacientesPage", pacientesPage);
        model.addAttribute("busca", busca != null ? busca : "");
        model.addAttribute("pacienteForm", pacienteForm);
        model.addAttribute("abrirNovoPacienteModal", false);
        // Contagens globais (independentes da página/busca)
        model.addAttribute("totalPacientesAtivos", pacienteService.contarAtivos());
        model.addAttribute("totalPacientesComTelefone", pacienteService.contarAtivosComTelefone());
        model.addAttribute("totalPacientesComEmail", pacienteService.contarAtivosComEmail());
    }
}
