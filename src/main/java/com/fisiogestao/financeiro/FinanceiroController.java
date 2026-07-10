package com.fisiogestao.financeiro;

import com.fisiogestao.config.ClinicTimezoneHolder;
import com.fisiogestao.financeiro.form.LancamentoFinanceiroForm;
import com.fisiogestao.financeiro.service.ContasAPagarExportService;
import com.fisiogestao.financeiro.service.ContasAReceberExportService;
import com.fisiogestao.financeiro.service.FinanceiroService;
import com.fisiogestao.financeiro.service.ReciboPdfService;
import com.fisiogestao.relatorios.form.FormatoRelatorio;
import com.fisiogestao.relatorios.service.RelatorioDocumento;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Controller
@RequestMapping("/financeiro")
@RequiredArgsConstructor
public class FinanceiroController {

    private final FinanceiroService financeiroService;
    private final ReciboPdfService reciboPdfService;
    private final ContasAReceberExportService contasAReceberExportService;
    private final ContasAPagarExportService contasAPagarExportService;

    public record FaturamentoBloco(String titulo, List<FinanceiroService.FaturamentoGrupo> itens) {}

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

    private static final List<String> DESTINOS_PERMITIDOS = List.of(
            "/financeiro", "/financeiro/contas-a-receber", "/financeiro/contas-a-pagar"
    );

    private String redirecionarPara(String redirectTo) {
        String destino = DESTINOS_PERMITIDOS.contains(redirectTo) ? redirectTo : "/financeiro";
        return "redirect:" + destino;
    }

    @GetMapping("/contas-a-receber")
    public String contasAReceber(Model model) {
        model.addAttribute("contasAReceber", financeiroService.listarContasAReceber());
        model.addAttribute("inadimplencia", financeiroService.calcularInadimplencia());
        model.addAttribute("contasAPagarPendentes", financeiroService.contarContasAPagarPendentes());
        model.addAttribute("metodosPagamento", MetodoPagamento.values());
        return "financeiro/contas-a-receber";
    }

    @GetMapping("/contas-a-receber/exportar")
    public ResponseEntity<ByteArrayResource> exportarContasAReceber(@RequestParam FormatoRelatorio formato) {
        FinanceiroService.ContasAReceberView view = financeiroService.listarContasAReceber();
        RelatorioDocumento documento = contasAReceberExportService.exportar(view, formato);
        String contentType = Objects.requireNonNullElse(documento.contentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
        byte[] content = Objects.requireNonNullElseGet(documento.content(), () -> new byte[0]);
        String fileName = Objects.requireNonNullElse(documento.fileName(), "contas-a-receber.bin");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(content.length)
                .body(new ByteArrayResource(content));
    }

    @GetMapping("/contas-a-pagar")
    public String contasAPagar(Model model) {
        model.addAttribute("contasAPagar", financeiroService.listarContasAPagar());
        model.addAttribute("inadimplencia", financeiroService.calcularInadimplencia());
        model.addAttribute("contasAPagarPendentes", financeiroService.contarContasAPagarPendentes());
        return "financeiro/contas-a-pagar";
    }

    @GetMapping("/contas-a-pagar/exportar")
    public ResponseEntity<ByteArrayResource> exportarContasAPagar(@RequestParam FormatoRelatorio formato) {
        FinanceiroService.ContasAPagarView view = financeiroService.listarContasAPagar();
        RelatorioDocumento documento = contasAPagarExportService.exportar(view, formato);
        String contentType = Objects.requireNonNullElse(documento.contentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
        byte[] content = Objects.requireNonNullElseGet(documento.content(), () -> new byte[0]);
        String fileName = Objects.requireNonNullElse(documento.fileName(), "contas-a-pagar.bin");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(content.length)
                .body(new ByteArrayResource(content));
    }

    @GetMapping("/relatorio-faturamento")
    public String relatorioFaturamento(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Model model
    ) {
        LocalDate periodoInicio = inicio != null ? inicio : LocalDate.now().withDayOfMonth(1);
        LocalDate periodoFim = fim != null ? fim : LocalDate.now();
        FinanceiroService.RelatorioFaturamento relatorio = financeiroService.gerarRelatorioFaturamento(periodoInicio, periodoFim);
        model.addAttribute("relatorio", relatorio);
        model.addAttribute("blocosFaturamento", List.of(
                new FaturamentoBloco("Categoria / tipo", relatorio.porCategoria()),
                new FaturamentoBloco("Plano", relatorio.porPlano()),
                new FaturamentoBloco("Profissional", relatorio.porProfissional()),
                new FaturamentoBloco("Método de pagamento", relatorio.porMetodoPagamento()),
                new FaturamentoBloco("Tipo de atendimento", relatorio.porTipoAtendimento())
        ));
        model.addAttribute("periodoInicio", periodoInicio);
        model.addAttribute("periodoFim", periodoFim);
        model.addAttribute("inadimplencia", financeiroService.calcularInadimplencia());
        model.addAttribute("contasAPagarPendentes", financeiroService.contarContasAPagarPendentes());
        return "financeiro/relatorio-faturamento";
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
            @RequestParam(value = "redirectTo", required = false, defaultValue = "/financeiro") String redirectTo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            financeiroService.marcarComoPago(id, metodoPagamento);
            redirectAttributes.addFlashAttribute("successMessage", "Pagamento registrado com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return redirecionarPara(redirectTo);
    }

    @PostMapping("/lancamentos/{id}/reabrir")
    public String reabrirLancamento(
            @PathVariable("id") UUID id,
            @RequestParam(value = "redirectTo", required = false, defaultValue = "/financeiro") String redirectTo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            financeiroService.reabrir(id);
            redirectAttributes.addFlashAttribute("successMessage", "Cancelamento revertido. O lançamento voltou para pendente.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return redirecionarPara(redirectTo);
    }

    @PostMapping("/lancamentos/{id}/cancelar-baixa")
    public String cancelarBaixa(
            @PathVariable("id") UUID id,
            @RequestParam(value = "redirectTo", required = false, defaultValue = "/financeiro") String redirectTo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            financeiroService.cancelarBaixa(id);
            redirectAttributes.addFlashAttribute("successMessage", "Baixa cancelada. O lançamento voltou para pendente.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return redirecionarPara(redirectTo);
    }

    @GetMapping("/lancamentos/{id}/recibo.pdf")
    public ResponseEntity<byte[]> baixarRecibo(@PathVariable UUID id) {
        byte[] pdf = reciboPdfService.gerarRecibo(id);
        String idCurto = id.toString().substring(0, 8).toUpperCase();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("recibo-" + idCurto + ".pdf")
                .build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @PostMapping("/lancamentos/{id}/cancelar")
    public String cancelarLancamento(
            @PathVariable("id") UUID id,
            @RequestParam(value = "redirectTo", required = false, defaultValue = "/financeiro") String redirectTo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            financeiroService.cancelar(id);
            redirectAttributes.addFlashAttribute("successMessage", "Lançamento cancelado com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return redirecionarPara(redirectTo);
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
        model.addAttribute("inadimplencia", financeiroService.calcularInadimplencia());
        model.addAttribute("contasAPagarPendentes", financeiroService.contarContasAPagarPendentes());
        model.addAttribute("periodoInicio", periodoInicio);
        model.addAttribute("periodoFim", periodoFim);
        model.addAttribute("editMode", lancamentoEmEdicao != null);
        model.addAttribute("lancamentoEmEdicao", lancamentoEmEdicao);
    }

    private LancamentoFinanceiroForm criarFormPadrao() {
        LancamentoFinanceiroForm form = new LancamentoFinanceiroForm();
        form.setTipo(TipoLancamentoFinanceiro.RECEITA);
        form.setDataRecebimento(LocalDate.now());
        return form;
    }

    private LancamentoFinanceiroForm mapearParaFormulario(LancamentoFinanceiroEntity lancamento) {
        LancamentoFinanceiroForm form = new LancamentoFinanceiroForm();
        form.setTipo(lancamento.getTipo());
        form.setDescricao(lancamento.getDescricao());
        form.setCategoria(lancamento.getCategoria());
        form.setProfissional(lancamento.getProfissional());
        form.setValor(lancamento.getValor());
        form.setDataRecebimento(lancamento.getDataRecebimento()
                .atZoneSameInstant(ClinicTimezoneHolder.zoneId()).toLocalDate());
        return form;
    }
}
