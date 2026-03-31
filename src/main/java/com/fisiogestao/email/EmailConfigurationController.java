package com.fisiogestao.email;

import com.fisiogestao.email.form.EmailConfiguracaoForm;
import com.fisiogestao.email.form.TesteEmailForm;
import com.fisiogestao.email.service.EmailConfiguracaoService;
import com.fisiogestao.email.service.EmailDispatchResult;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/configuracoes/email")
@RequiredArgsConstructor
public class EmailConfigurationController {

    private final EmailConfiguracaoService emailConfiguracaoService;

    @GetMapping
    public String index(Model model) {
        EmailConfiguracaoForm form = emailConfiguracaoService.buscarAtiva()
                .map(this::toForm)
                .orElseGet(this::formPadrao);
        prepararPagina(model, form);
        return "email/configuracao";
    }

    @PostMapping
    public String salvar(
            @Valid @ModelAttribute("emailConfiguracaoForm") EmailConfiguracaoForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararPagina(model, form);
            return "email/configuracao";
        }

        try {
            emailConfiguracaoService.salvar(
                    form.getId() == null || form.getId().isBlank() ? null : UUID.fromString(form.getId()),
                    form.getNomeRemetente(),
                    form.getEmailRemetente(),
                    form.getSenhaApp(),
                    form.isManterSenhaAtual(),
                    form.getSmtpHost(),
                    form.getSmtpPort(),
                    form.isUsaAutenticacao(),
                    form.isUsaTls(),
                    form.isUsaSsl(),
                    form.getReplyTo(),
                    form.isAtivo(),
                    form.getObservacoes()
            );
            redirectAttributes.addFlashAttribute("successMessage", "Configuração SMTP salva com sucesso.");
            return "redirect:/configuracoes/email";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, form);
            return "email/configuracao";
        }
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            emailConfiguracaoService.definirAtiva(id);
            redirectAttributes.addFlashAttribute("successMessage", "Configuração SMTP ativada com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/configuracoes/email";
    }

    @PostMapping("/{id}/testar")
    public String testar(
            @PathVariable UUID id,
            @Valid @ModelAttribute("testeEmailForm") TesteEmailForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        EmailConfiguracaoForm formConfiguracao = emailConfiguracaoService.buscarPorId(id) != null
                ? toForm(emailConfiguracaoService.buscarPorId(id))
                : formPadrao();

        if (bindingResult.hasErrors()) {
            prepararPagina(model, formConfiguracao);
            return "email/configuracao";
        }

        try {
            EmailDispatchResult result = emailConfiguracaoService.testarEnvio(
                    emailConfiguracaoService.buscarPorId(id),
                    form.getDestinatario(),
                    authentication != null ? authentication.getName() : "sistema"
            );
            if (result.sent()) {
                redirectAttributes.addFlashAttribute("successMessage", result.message());
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", result.message());
            }
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/configuracoes/email";
    }

    private void prepararPagina(Model model, EmailConfiguracaoForm form) {
        model.addAttribute("emailConfiguracaoForm", form);
        model.addAttribute("testeEmailForm", new TesteEmailForm());
        model.addAttribute("configuracoesEmail", emailConfiguracaoService.listarConfiguracoes());
        model.addAttribute("historicoEmails", emailConfiguracaoService.listarHistorico());
    }

    private EmailConfiguracaoForm toForm(EmailConfiguracaoEntity entity) {
        EmailConfiguracaoForm form = new EmailConfiguracaoForm();
        form.setId(entity.getId().toString());
        form.setNomeRemetente(entity.getNomeRemetente());
        form.setEmailRemetente(entity.getEmailRemetente());
        form.setSmtpHost(entity.getSmtpHost());
        form.setSmtpPort(entity.getSmtpPort());
        form.setUsaAutenticacao(Boolean.TRUE.equals(entity.getUsaAutenticacao()));
        form.setUsaTls(Boolean.TRUE.equals(entity.getUsaTls()));
        form.setUsaSsl(Boolean.TRUE.equals(entity.getUsaSsl()));
        form.setReplyTo(entity.getReplyTo());
        form.setAtivo(Boolean.TRUE.equals(entity.getAtivo()));
        form.setObservacoes(entity.getObservacoes());
        form.setManterSenhaAtual(true);
        return form;
    }

    private EmailConfiguracaoForm formPadrao() {
        EmailConfiguracaoForm form = new EmailConfiguracaoForm();
        form.setNomeRemetente("FisioGestao");
        form.setSmtpHost("smtp.gmail.com");
        form.setSmtpPort(587);
        form.setUsaAutenticacao(true);
        form.setUsaTls(true);
        form.setAtivo(true);
        form.setManterSenhaAtual(false);
        return form;
    }
}
