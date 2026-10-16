package com.fisiogestao.agenda;

import com.fisiogestao.agenda.form.ConfiguracaoClinicaForm;
import com.fisiogestao.agenda.service.ConfiguracaoClinicaService;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.util.List;

@Controller
@RequestMapping({"/configuracoes/clinica", "/configuracao/clinica"})
@RequiredArgsConstructor
public class ConfiguracaoClinicaController {

    private final ConfiguracaoClinicaService configuracaoClinicaService;
    private final com.fisiogestao.cancelamento.CancelamentoService cancelamentoService;

    @GetMapping
    public String index(Model model) {
        prepararPagina(model, configuracaoClinicaService.carregarFormulario());
        return "configuracoes/clinica";
    }

    @PostMapping
    public String salvar(
            @Valid @ModelAttribute("configuracaoClinicaForm") ConfiguracaoClinicaForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararPagina(model, form);
            return "configuracoes/clinica";
        }

        try {
            configuracaoClinicaService.salvar(form);
            redirectAttributes.addFlashAttribute("successMessage", "Configuração da clínica atualizada com sucesso.");
            return "redirect:/configuracoes/clinica";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, form);
            return "configuracoes/clinica";
        }
    }

    /** Solicitação de cancelamento da conta: somente ADMIN_CLINICA; não altera status nem apaga dados. */
    @PostMapping("/cancelamento")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN_CLINICA')")
    public String solicitarCancelamento(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String nomeSolicitante,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String emailContato,
            @org.springframework.web.bind.annotation.RequestParam(required = false) com.fisiogestao.cancelamento.MotivoCancelamento motivo,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String descricao,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean confirmacao,
            jakarta.servlet.http.HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        try {
            cancelamentoService.solicitarPelaClinica(nomeSolicitante, emailContato, motivo, descricao, confirmacao,
                    request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Solicitação de cancelamento enviada. A clínica continua com acesso normal até a análise da nossa equipe; nenhum dado foi alterado.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/configuracoes/clinica#cancelamento";
    }

    private void prepararPagina(Model model, ConfiguracaoClinicaForm form) {
        model.addAttribute("configuracaoClinicaForm", form);
        model.addAttribute("diasSemana", List.of(
                new DiaSemanaOption(DayOfWeek.SUNDAY, "Domingo"),
                new DiaSemanaOption(DayOfWeek.MONDAY, "Segunda-feira"),
                new DiaSemanaOption(DayOfWeek.TUESDAY, "Terça-feira"),
                new DiaSemanaOption(DayOfWeek.WEDNESDAY, "Quarta-feira"),
                new DiaSemanaOption(DayOfWeek.THURSDAY, "Quinta-feira"),
                new DiaSemanaOption(DayOfWeek.FRIDAY, "Sexta-feira"),
                new DiaSemanaOption(DayOfWeek.SATURDAY, "Sábado")
        ));
        model.addAttribute("timezonesClinica", configuracaoClinicaService.listarTimezonesSuportados());
        model.addAttribute("motivosCancelamento", com.fisiogestao.cancelamento.MotivoCancelamento.values());
        model.addAttribute("textoConfirmacaoCancelamento", com.fisiogestao.cancelamento.CancelamentoService.TEXTO_CONFIRMACAO);
        model.addAttribute("solicitacoesCancelamento", cancelamentoService.listarDaClinicaAtual());
    }

    public record DiaSemanaOption(DayOfWeek value, String label) {
    }
}
