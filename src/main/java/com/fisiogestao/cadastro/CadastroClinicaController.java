package com.fisiogestao.cadastro;

import com.fisiogestao.empresa.ConfiguracaoPlataformaService;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Cadastro público de nova clínica (rota sem autenticação, protegida por CSRF e rate limiting). */
@Controller
@RequiredArgsConstructor
public class CadastroClinicaController {

    private final CadastroClinicaService cadastroClinicaService;
    private final ConfiguracaoPlataformaService configuracaoPlataformaService;

    @GetMapping("/cadastro")
    public String formulario(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new CadastroClinicaForm());
        }
        model.addAttribute("diasTeste", configuracaoPlataformaService.diasUteisTestePadrao());
        return "publico/cadastro-clinica";
    }

    @PostMapping("/cadastro")
    public String cadastrar(
            @Valid @ModelAttribute("form") CadastroClinicaForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("diasTeste", configuracaoPlataformaService.diasUteisTestePadrao());
            return "publico/cadastro-clinica";
        }
        try {
            CadastroClinicaService.ResultadoCadastro resultado = cadastroClinicaService.cadastrar(form, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("resultado", resultado);
            return "redirect:/cadastro/sucesso";
        } catch (BusinessException ex) {
            form.setSenha(null);
            form.setConfirmacaoSenha(null);
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("diasTeste", configuracaoPlataformaService.diasUteisTestePadrao());
            return "publico/cadastro-clinica";
        }
    }

    @GetMapping("/cadastro/sucesso")
    public String sucesso(Model model) {
        if (!model.containsAttribute("resultado")) {
            return "redirect:/cadastro";
        }
        return "publico/cadastro-clinica-sucesso";
    }
}
