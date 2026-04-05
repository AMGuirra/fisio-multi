package com.fisiogestao.auth;

import com.fisiogestao.auth.form.UsuarioForm;
import com.fisiogestao.auth.service.PasswordResetService;
import com.fisiogestao.email.service.EmailDispatchResult;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
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
    private final PasswordResetService passwordResetService;

    @GetMapping
    public String listar(
            @RequestParam(name = "editar", required = false) UUID editarId,
            Model model
    ) {
        UsuarioForm form = new UsuarioForm();
        form.setRole(UserRole.RECEPCAO);

        if (editarId != null) {
            userCatalog.buscarPorId(editarId).ifPresentOrElse(usuario -> {
                form.setNome(usuario.nome());
                form.setUsername(usuario.username());
                form.setEmail(usuario.email());
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
            HttpServletRequest request,
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
            InMemoryUserCatalog.SystemUserView user = userCatalog.criar(form.getNome(), form.getUsername(), form.getEmail(), form.getPassword(), form.getRole());
            EmailDispatchResult onboardingResult = passwordResetService.enviarEmailBoasVindasParaUsuarioId(
                    user.id(),
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
            );
            redirectAttributes.addFlashAttribute("successMessage", "Usuário cadastrado com sucesso.");
            if (!onboardingResult.sent()) {
                redirectAttributes.addFlashAttribute("warningMessage", "O usuário foi cadastrado, mas o e-mail de acesso não pôde ser enviado agora.");
            }
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
                    form.getNome(),
                    form.getUsername(),
                    form.getEmail(),
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

    @PostMapping("/{id}/reenviar-acesso")
    public String reenviarAcesso(
            @PathVariable UUID id,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        try {
            EmailDispatchResult result = passwordResetService.enviarEmailBoasVindasParaUsuarioId(
                    id,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
            );
            redirectAttributes.addFlashAttribute(result.sent() ? "successMessage" : "errorMessage", result.message());
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
        if (form.getPassword().trim().length() < PasswordPolicy.MIN_LENGTH) {
            bindingResult.rejectValue("password", "password.tooShort", PasswordPolicy.MIN_LENGTH_MESSAGE);
        }
    }
}
