package com.fisiogestao.listaespera;

import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/lista-espera-publica")
@RequiredArgsConstructor
public class ListaEsperaPublicaController {

    private final ListaEsperaService listaEsperaService;

    @GetMapping
    public String formulario(Model model) {
        model.addAttribute("periodos", PeriodoPreferido.values());
        return "publico/lista-espera-publica";
    }

    @PostMapping
    public String entrar(
            @RequestParam("nome") String nome,
            @RequestParam("telefone") String telefone,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "periodo", required = false) PeriodoPreferido periodo,
            @RequestParam(value = "observacoes", required = false) String observacoes,
            RedirectAttributes ra) {

        if (nome == null || nome.isBlank()) {
            ra.addFlashAttribute("errorMessage", "Informe seu nome completo.");
            return "redirect:/lista-espera-publica";
        }
        if (telefone == null || telefone.replaceAll("\\D", "").length() < 10) {
            ra.addFlashAttribute("errorMessage", "Informe um telefone válido.");
            return "redirect:/lista-espera-publica";
        }

        try {
            listaEsperaService.entrarNaFilaPublico(nome, telefone, email, periodo, observacoes);
        } catch (BusinessException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/lista-espera-publica";
        }
        return "redirect:/lista-espera-publica/sucesso";
    }

    @GetMapping("/sucesso")
    public String sucesso() {
        return "publico/lista-espera-publica-sucesso";
    }
}
