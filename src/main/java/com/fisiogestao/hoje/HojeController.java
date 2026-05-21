package com.fisiogestao.hoje;

import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/hoje")
@RequiredArgsConstructor
public class HojeController {

    private final HojeService hojeService;
    private final AgendaService agendaService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("resumo", hojeService.resumoDeHoje());
        return "hoje/index";
    }

    @PostMapping("/sessoes/{id}/realizar")
    public String marcarRealizada(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            agendaService.marcarComoRealizada(id, null);
            redirectAttributes.addFlashAttribute("successMessage", "Sessão marcada como realizada.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/hoje";
    }

    @PostMapping("/sessoes/{id}/cancelar")
    public String cancelarSessao(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            agendaService.cancelarSessao(id, null);
            redirectAttributes.addFlashAttribute("successMessage", "Sessão cancelada.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/hoje";
    }
}
