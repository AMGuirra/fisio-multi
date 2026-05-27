package com.fisiogestao.institucional;

import com.fisiogestao.landing.LandingContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller público para a Landing Page institucional do FisioGestão.
 *
 * <p>Visitantes não autenticados recebem a landing page com todo o conteúdo
 * carregado do banco de dados via {@link LandingContentService}.
 * Usuários autenticados são redirecionados para o dashboard.</p>
 */
@Controller
@RequiredArgsConstructor
public class LandingController {

    private final LandingContentService landingContentService;

    @GetMapping({"/", "/home"})
    public String landing(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        var data = landingContentService.loadLandingData();
        model.addAttribute("hero",       data.hero());
        model.addAttribute("secoes",     data.secoes());
        model.addAttribute("testemunhos",data.testemunhos());
        model.addAttribute("planos",     data.planos());
        model.addAttribute("faqs",       data.faqs());
        model.addAttribute("beneficios", data.beneficios());
        model.addAttribute("steps",      data.steps());
        return "institucional/landing";
    }
}
