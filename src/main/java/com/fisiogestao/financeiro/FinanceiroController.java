package com.fisiogestao.financeiro;

import com.fisiogestao.financeiro.form.LancamentoFinanceiroForm;
import com.fisiogestao.financeiro.service.FinanceiroService;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/financeiro")
@RequiredArgsConstructor
public class FinanceiroController {

    private final FinanceiroService financeiroService;

    @GetMapping
    public String index(Model model) {
        prepararPagina(model, criarFormPadrao());
        return "financeiro/index";
    }

    @PostMapping("/lancamentos")
    public String criarLancamento(
            @Valid @ModelAttribute("lancamentoForm") LancamentoFinanceiroForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararPagina(model, form);
            return "financeiro/index";
        }

        try {
            financeiroService.criarManual(form);
            redirectAttributes.addFlashAttribute("successMessage", "Lançamento financeiro cadastrado com sucesso.");
            return "redirect:/financeiro";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, form);
            return "financeiro/index";
        }
    }

    @PostMapping("/lancamentos/{id}/pagar")
    public String marcarComoPago(
            @PathVariable("id") UUID id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            financeiroService.marcarComoPago(id);
            redirectAttributes.addFlashAttribute("successMessage", "Lançamento marcado como pago.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/financeiro";
    }

    private void prepararPagina(Model model, LancamentoFinanceiroForm form) {
        model.addAttribute("lancamentoForm", form);
        model.addAttribute("tiposLancamento", TipoLancamentoFinanceiro.values());
        model.addAttribute("lancamentos", financeiroService.listarTodos());
        model.addAttribute("totalReceitasPendentes", financeiroService.totalReceitasPendentes());
        model.addAttribute("totalDespesasPendentes", financeiroService.totalDespesasPendentes());
        model.addAttribute("saldoProjetado", financeiroService.saldoProjetado());
    }

    private LancamentoFinanceiroForm criarFormPadrao() {
        LancamentoFinanceiroForm form = new LancamentoFinanceiroForm();
        form.setTipo(TipoLancamentoFinanceiro.RECEITA);
        form.setDataCompetencia(LocalDate.now());
        return form;
    }
}
