package com.fisiogestao.whatsapp;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/whatsapp")
public class WhatsAppController {

    @GetMapping
    public String index(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "A página da fila do WhatsApp está inativa.");
        return "redirect:/dashboard";
    }
}
