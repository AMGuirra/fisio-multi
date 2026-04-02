package com.fisiogestao.agenda;

import com.fisiogestao.agenda.form.ConfiguracaoClinicaForm;
import com.fisiogestao.agenda.service.ConfiguracaoClinicaService;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.util.List;

@Controller
@RequestMapping({"/configuracoes/clinica", "/configuracao/clinica"})
@RequiredArgsConstructor
public class ConfiguracaoClinicaController {

    private final ConfiguracaoClinicaService configuracaoClinicaService;

    @GetMapping
    public String index(Model model) {
        prepararPagina(model, configuracaoClinicaService.carregarFormulario());
        return "configuracoes/clinica";
    }

    @PostMapping
    public String salvar(
            @Valid @ModelAttribute("configuracaoClinicaForm") ConfiguracaoClinicaForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararPagina(model, form);
            return "configuracoes/clinica";
        }

        try {
            configuracaoClinicaService.salvar(form);
            redirectAttributes.addFlashAttribute("successMessage", "Configuração da clínica atualizada com sucesso.");
            return "redirect:/configuracoes/clinica";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, form);
            return "configuracoes/clinica";
        }
    }

    private void prepararPagina(Model model, ConfiguracaoClinicaForm form) {
        model.addAttribute("configuracaoClinicaForm", form);
        model.addAttribute("diasSemana", List.of(
                new DiaSemanaOption(DayOfWeek.MONDAY, "Segunda-feira"),
                new DiaSemanaOption(DayOfWeek.TUESDAY, "Terça-feira"),
                new DiaSemanaOption(DayOfWeek.WEDNESDAY, "Quarta-feira"),
                new DiaSemanaOption(DayOfWeek.THURSDAY, "Quinta-feira"),
                new DiaSemanaOption(DayOfWeek.FRIDAY, "Sexta-feira"),
                new DiaSemanaOption(DayOfWeek.SATURDAY, "Sábado"),
                new DiaSemanaOption(DayOfWeek.SUNDAY, "Domingo")
        ));
        model.addAttribute("timezonesClinica", configuracaoClinicaService.listarTimezonesSuportados());
    }

    public record DiaSemanaOption(DayOfWeek value, String label) {
    }
}
