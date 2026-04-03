package com.fisiogestao.auth;

import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequestMapping("/funcoes-usuarios")
@RequiredArgsConstructor
public class UserRolePermissionsController {

    private final UserMenuAccessService userMenuAccessService;

    @GetMapping
    public String index(Model model) {
        prepararPagina(model);
        return "usuarios/funcoes";
    }

    @GetMapping("/{role}")
    public String detalharFuncao(
            @PathVariable UserRole role,
            Model model
    ) {
        model.addAttribute("editingRole", role.name());
        prepararPagina(model);
        return "usuarios/funcoes";
    }

    @PostMapping("/{role}")
    public String salvar(
            @PathVariable UserRole role,
            @RequestParam(name = "menus", required = false) Set<AppMenuItem> menus,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userMenuAccessService.salvarPermissoes(role, menus);
            redirectAttributes.addFlashAttribute("successMessage", "Permissões atualizadas para " + role.getLabel() + ".");
            return "redirect:/funcoes-usuarios";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("editingRole", role.name());
            prepararPagina(model);
            return "usuarios/funcoes";
        }
    }

    private void prepararPagina(Model model) {
        model.addAttribute("menusConfiguraveis", userMenuAccessService.listarMenusConfiguraveis());
        model.addAttribute("funcoesUsuario", userMenuAccessService.listarPermissoesPorFuncao());
    }
}
