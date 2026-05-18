package com.fisiogestao.financeiro;

import com.fisiogestao.financeiro.form.LancamentoFinanceiroForm;
import com.fisiogestao.financeiro.service.FinanceiroService;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/financeiro")
@RequiredArgsConstructor
public class FinanceiroController {

    private final FinanceiroService financeiroService;

    @GetMapping
    public String index(
            @RequestParam(required = false) UUID editar,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Model model
    ) {
        LocalDate periodoInicio = inicio != null ? inicio : LocalDate.now().withDayOfMonth(1);
        LocalDate periodoFim   = fim   != null ? fim   : LocalDate.now();

        LancamentoFinanceiroForm form = model.asMap().get("lancamentoForm") instanceof LancamentoFinanceiroForm existente
                ? existente
                : null;
        prepararPagina(model, form, editar, periodoInicio, periodoFim);
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
            prepararPagina(model, form, null, LocalDate.now().withDayOfMonth(1), LocalDate.now());
            return "financeiro/index";
        }

        try {
            financeiroService.criarManual(form);
            redirectAttributes.addFlashAttribute("successMessage", "Lançamento financeiro cadastrado com sucesso.");
            return "redirect:/financeiro";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, form, null, LocalDate.now().withDayOfMonth(1), LocalDate.now());
            return "financeiro/index";
        }
    }

    @PostMapping("/lancamentos/{id}")
    public String editarLancamento(
            @PathVariable("id") UUID id,
            @Valid @ModelAttribute("lancamentoForm") LancamentoFinanceiroForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararPagina(model, form, id, LocalDate.now().withDayOfMonth(1), LocalDate.now());
            return "financeiro/index";
        }

        try {
            financeiroService.atualizarManual(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Lançamento financeiro atualizado com sucesso.");
            return "redirect:/financeiro";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, form, id, LocalDate.now().withDayOfMonth(1), LocalDate.now());
            return "financeiro/index";
        }
    }

    @PostMapping("/lancamentos/{id}/pagar")
    public String marcarComoPago(
            @PathVariable("id") UUID id,
            @RequestParam(value = "metodoPagamento", required = false) MetodoPagamento metodoPagamento,
            RedirectAttributes redirectAttributes
    ) {
        try {
            financeiroService.marcarComoPago(id, metodoPagamento);
            redirectAttributes.addFlashAttribute("successMessage", "Lançamento marcado como pago.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/financeiro";
    }

    @PostMapping("/lancamentos/{id}/cancelar-baixa")
    public String cancelarBaixa(
            @PathVariable("id") UUID id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            financeiroService.cancelarBaixa(id);
            redirectAttributes.addFlashAttribute("successMessage", "Baixa cancelada. O lançamento voltou para pendente.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/financeiro";
    }

    @PostMapping("/lancamentos/{id}/cancelar")
    public String cancelarLancamento(
            @PathVariable("id") UUID id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            financeiroService.cancelar(id);
            redirectAttributes.addFlashAttribute("successMessage", "Lançamento cancelado com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/financeiro";
    }

    private void prepararPagina(Model model, LancamentoFinanceiroForm form, UUID lancamentoEdicaoId,
                                LocalDate periodoInicio, LocalDate periodoFim) {
        LancamentoFinanceiroEntity lancamentoEmEdicao = null;
        LancamentoFinanceiroForm formResolvido = form;
        if (lancamentoEdicaoId != null) {
            lancamentoEmEdicao = financeiroService.buscarPorId(lancamentoEdicaoId);
            if (formResolvido == null) {
                formResolvido = mapearParaFormulario(lancamentoEmEdicao);
            }
        }
        if (formResolvido == null) {
            formResolvido = criarFormPadrao();
        }

        FinanceiroService.ResumoFinanceiro resumo = financeiroService.calcularResumo(periodoInicio, periodoFim);

        model.addAttribute("lancamentoForm", formResolvido);
        model.addAttribute("tiposLancamento", TipoLancamentoFinanceiro.values());
        model.addAttribute("metodosPagamento", MetodoPagamento.values());
        model.addAttribute("lancamentos", financeiroService.listarNoPeriodo(periodoInicio, periodoFim));
        model.addAttribute("resumo", resumo);
        model.addAttribute("periodoInicio", periodoInicio);
        model.addAttribute("periodoFim", periodoFim);
        model.addAttribute("editMode", lancamentoEmEdicao != null);
        model.addAttribute("lancamentoEmEdicao", lancamentoEmEdicao);
    }

    private LancamentoFinanceiroForm criarFormPadrao() {
        LancamentoFinanceiroForm form = new LancamentoFinanceiroForm();
        form.setTipo(TipoLancamentoFinanceiro.RECEITA);
        form.setDataCompetencia(LocalDate.now());
        return form;
    }

    private LancamentoFinanceiroForm mapearParaFormulario(LancamentoFinanceiroEntity lancamento) {
        LancamentoFinanceiroForm form = new LancamentoFinanceiroForm();
        form.setTipo(lancamento.getTipo());
        form.setDescricao(lancamento.getDescricao());
        form.setCategoria(lancamento.getCategoria());
        form.setValor(lancamento.getValor());
        form.setDataCompetencia(lancamento.getDataCompetencia().toLocalDate());
        return form;
    }
}
