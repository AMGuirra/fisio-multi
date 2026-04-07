package com.fisiogestao.status;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/system-status")
@RequiredArgsConstructor
public class AdminSystemStatusController {

    private final SystemStatusService systemStatusService;

    @GetMapping
    public String showSystemStatus(Model model) {
        SystemStatusEntity statusAtual = systemStatusService.getStatusAtual();
        model.addAttribute("statusAtual", statusAtual);
        model.addAttribute("statusTypes", SystemStatusType.values());
        return "admin/system-status";
    }

    @PostMapping
    public String atualizarStatus(
            @RequestParam SystemStatusType status,
            @RequestParam(required = false) String mensagem,
            RedirectAttributes redirectAttributes
    ) {
        systemStatusService.atualizarStatus(status, mensagem);
        redirectAttributes.addFlashAttribute("successMessage", "Status do sistema atualizado com sucesso.");
        return "redirect:/admin/system-status";
    }
}