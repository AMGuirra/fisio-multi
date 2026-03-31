package com.fisiogestao.auth;

import com.fisiogestao.auth.form.ForgotPasswordForm;
import com.fisiogestao.auth.form.ResetPasswordForm;
import com.fisiogestao.auth.service.PasswordResetService;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final PasswordResetService passwordResetService;

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }
        return "auth/login";
    }

    @GetMapping("/esqueci-minha-senha")
    public String forgotPassword(Model model) {
        model.addAttribute("forgotPasswordForm", new ForgotPasswordForm());
        return "auth/forgot-password";
    }

    @PostMapping("/esqueci-minha-senha")
    public String handleForgotPassword(
            @Valid @ModelAttribute("forgotPasswordForm") ForgotPasswordForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }

        passwordResetService.solicitarReset(
                form.getEmail(),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Se o e-mail estiver cadastrado, você receberá as instruções para redefinir a senha."
        );
        return "redirect:/esqueci-minha-senha";
    }

    @GetMapping("/resetar-senha")
    public String resetPassword(@RequestParam(name = "token", required = false) String token, Model model) {
        ResetPasswordForm form = new ResetPasswordForm();
        form.setToken(token);
        model.addAttribute("resetPasswordForm", form);
        model.addAttribute("tokenValido", passwordResetService.tokenValido(token));
        return "auth/reset-password";
    }

    @PostMapping("/resetar-senha")
    public String handleResetPassword(
            @Valid @ModelAttribute("resetPasswordForm") ResetPasswordForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("tokenValido", passwordResetService.tokenValido(form.getToken()));
            return "auth/reset-password";
        }

        try {
            passwordResetService.redefinirSenha(form.getToken(), form.getPassword(), form.getConfirmPassword());
            redirectAttributes.addFlashAttribute("successMessage", "Senha redefinida com sucesso. Faça login para continuar.");
            return "redirect:/login";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("tokenValido", passwordResetService.tokenValido(form.getToken()));
            return "auth/reset-password";
        }
    }
}
