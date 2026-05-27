package com.fisiogestao.dashboard;

import com.fisiogestao.config.ClinicTimezoneHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(name = "acessoNegado", required = false) boolean acessoNegado, Model model) {
        model.addAttribute("summary", dashboardService.carregarResumo());
        model.addAttribute("dashboardToday", ClinicTimezoneHolder.today());
        if (acessoNegado) {
            model.addAttribute("errorMessage", "Seu perfil não possui acesso a esse menu.");
        }
        return "dashboard/index";
    }
}
