package com.fisiogestao.listaespera;

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
@RequestMapping("/lista-espera")
@RequiredArgsConstructor
public class ListaEsperaController {

    private final ListaEsperaService listaEsperaService;

    @GetMapping
    public String index(Model model) {
        var entradas = listaEsperaService.listarAguardando();
        long totalComCadastro = entradas.stream()
                .filter(entrada -> entrada.getCliente() != null)
                .count();

        model.addAttribute("entradas", entradas);
        model.addAttribute("totalAguardando", entradas.size());
        model.addAttribute("totalComCadastro", totalComCadastro);
        model.addAttribute("totalPublico", entradas.size() - totalComCadastro);
        model.addAttribute("periodos", PeriodoPreferido.values());
        return "lista-espera/index";
    }

    @PostMapping("/{id}/notificar")
    public String notificar(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            listaEsperaService.notificar(id);
            ra.addFlashAttribute("successMessage", "Paciente marcado como notificado.");
        } catch (BusinessException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/lista-espera";
    }

    @PostMapping("/{id}/converter")
    public String converter(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            ListaEsperaEntity entrada = listaEsperaService.converter(id);
            ra.addFlashAttribute("successMessage",
                    "✓ " + entrada.getNomeExibicao() + " convertido. Use o botão + para criar a sessão.");
            if (entrada.getCliente() != null) {
                return "redirect:/calendario?clienteId=" + entrada.getCliente().getId();
            }
        } catch (BusinessException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/lista-espera";
        }
        return "redirect:/calendario";
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            listaEsperaService.cancelar(id);
            ra.addFlashAttribute("successMessage", "Entrada removida da lista de espera.");
        } catch (BusinessException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/lista-espera";
    }
}
