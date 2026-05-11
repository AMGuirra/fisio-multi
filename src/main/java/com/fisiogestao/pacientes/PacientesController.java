package com.fisiogestao.pacientes;

import com.fisiogestao.pacientes.form.PacienteForm;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/pacientes")
@RequiredArgsConstructor
public class PacientesController {

    private final PacienteService pacienteService;

    @GetMapping
    public String index(Model model) {
        List<ClienteEntity> pacientes = pacienteService.listarAtivos();
        prepararPagina(model, pacientes, new PacienteForm());
        return "pacientes/index";
    }

    @PostMapping
    public String criarPaciente(
            @Valid @ModelAttribute("pacienteForm") PacienteForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        List<ClienteEntity> pacientes = pacienteService.listarAtivos();

        if (bindingResult.hasErrors()) {
            prepararPagina(model, pacientes, form);
            return "pacientes/index";
        }

        try {
            pacienteService.cadastrar(form);
            redirectAttributes.addFlashAttribute("successMessage", "Paciente cadastrado com sucesso.");
            return "redirect:/pacientes";
        }
        catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, pacientes, form);
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
            List<ClienteEntity> pacientes = pacienteService.listarAtivos();
            prepararPagina(model, pacientes, form);
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
            redirectAttributes.addFlashAttribute("successMessage", "Paciente excluído com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/pacientes";
    }

    private void prepararPagina(Model model, List<ClienteEntity> pacientes, PacienteForm pacienteForm) {
        long pacientesComTelefone = pacientes.stream()
                .filter(paciente -> paciente.getTelefone() != null && !paciente.getTelefone().isBlank())
                .count();
        long pacientesComEmail = pacientes.stream()
                .filter(paciente -> paciente.getEmail() != null && !paciente.getEmail().isBlank())
                .count();

        model.addAttribute("pacientes", pacientes);
        model.addAttribute("pacienteForm", pacienteForm);
        model.addAttribute("totalPacientesAtivos", pacientes.size());
        model.addAttribute("totalPacientesComTelefone", pacientesComTelefone);
        model.addAttribute("totalPacientesComEmail", pacientesComEmail);
    }
}
