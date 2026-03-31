package com.fisiogestao.auth;

import com.fisiogestao.auth.form.UsuarioForm;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UserManagementController {

    private final InMemoryUserCatalog userCatalog;

    @GetMapping
    public String listar(
            @RequestParam(name = "editar", required = false) UUID editarId,
            Model model
    ) {
        UsuarioForm form = new UsuarioForm();
        form.setRole(UserRole.ADMINISTRATIVO);

        if (editarId != null) {
            userCatalog.buscarPorId(editarId).ifPresentOrElse(usuario -> {
                form.setUsername(usuario.username());
                form.setRole(usuario.role());
                model.addAttribute("editingUserId", usuario.id());
                model.addAttribute("editMode", true);
            }, () -> model.addAttribute("errorMessage", "Usuário selecionado para edição não foi encontrado."));
        }

        prepararPagina(model, form);
        return "usuarios/index";
    }

    @PostMapping("/novo")
    public String criar(
            @Valid @ModelAttribute("usuarioForm") UsuarioForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        validarSenhaCadastro(form, bindingResult);

        if (bindingResult.hasErrors()) {
            prepararPagina(model, form);
            model.addAttribute("editMode", false);
            return "usuarios/index";
        }

        try {
            userCatalog.criar(form.getUsername(), form.getPassword(), form.getRole());
            redirectAttributes.addFlashAttribute("successMessage", "Usuário cadastrado com sucesso.");
            return "redirect:/usuarios";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("editMode", false);
            prepararPagina(model, form);
            return "usuarios/index";
        }
    }

    @PostMapping("/{id}/editar")
    public String editar(
            @PathVariable UUID id,
            @Valid @ModelAttribute("usuarioForm") UsuarioForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        validarSenhaOpcional(form, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("editMode", true);
            model.addAttribute("editingUserId", id);
            prepararPagina(model, form);
            return "usuarios/index";
        }

        try {
            userCatalog.atualizar(
                    id,
                    form.getUsername(),
                    form.getPassword(),
                    form.getRole(),
                    authentication != null ? authentication.getName() : null
            );
            redirectAttributes.addFlashAttribute("successMessage", "Usuário atualizado com sucesso.");
            return "redirect:/usuarios";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("editMode", true);
            model.addAttribute("editingUserId", id);
            prepararPagina(model, form);
            return "usuarios/index";
        }
    }

    @PostMapping("/{id}/excluir")
    public String excluir(
            @PathVariable UUID id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userCatalog.excluir(id, authentication != null ? authentication.getName() : null);
            redirectAttributes.addFlashAttribute("successMessage", "Usuário excluído com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/usuarios";
    }

    private void prepararPagina(Model model, UsuarioForm form) {
        model.addAttribute("usuarioForm", form);
        model.addAttribute("tiposUsuario", userCatalog.listarPerfis());
        model.addAttribute("usuarios", userCatalog.listar());
        if (!model.containsAttribute("editMode")) {
            model.addAttribute("editMode", false);
        }
    }

    private void validarSenhaCadastro(UsuarioForm form, BindingResult bindingResult) {
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            bindingResult.rejectValue("password", "password.required", "Informe a senha.");
            return;
        }
        validarSenhaOpcional(form, bindingResult);
    }

    private void validarSenhaOpcional(UsuarioForm form, BindingResult bindingResult) {
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            return;
        }
        if (form.getPassword().trim().length() < 4) {
            bindingResult.rejectValue("password", "password.tooShort", "A senha deve ter pelo menos 4 caracteres.");
        }
    }
}
