package com.fisiogestao.institucional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller público para a Landing Page institucional do FisioGestão.
 *
 * <p>Visitantes não autenticados recebem a landing page de apresentação.
 * Usuários já autenticados são redirecionados diretamente para o dashboard,
 * sem passar pela tela de marketing.</p>
 *
 * <p>Rotas mapeadas: {@code /} e {@code /home}</p>
 */
@Controller
public class LandingController {

    @GetMapping({"/", "/home"})
    public String landing(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "institucional/landing";
    }
}
