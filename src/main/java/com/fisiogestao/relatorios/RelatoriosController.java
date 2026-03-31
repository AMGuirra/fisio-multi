package com.fisiogestao.relatorios;

import com.fisiogestao.financeiro.service.FinanceiroService;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.relatorios.form.CobrancaEmailForm;
import com.fisiogestao.relatorios.form.EnvioRelatorioForm;
import com.fisiogestao.relatorios.form.FiltroRelatorioForm;
import com.fisiogestao.relatorios.form.FormatoRelatorio;
import com.fisiogestao.relatorios.form.LembreteEmailForm;
import com.fisiogestao.relatorios.form.TipoLembrete;
import com.fisiogestao.relatorios.service.RelatorioDocumento;
import com.fisiogestao.relatorios.service.RelatorioOperacional;
import com.fisiogestao.relatorios.service.RelatoriosService;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/relatorios")
@RequiredArgsConstructor
public class RelatoriosController {

    private final RelatoriosService relatoriosService;
    private final PacienteService pacienteService;
    private final FinanceiroService financeiroService;

    @GetMapping
    public String index(
            @RequestParam(name = "dataInicial", required = false) LocalDate dataInicial,
            @RequestParam(name = "dataFinal", required = false) LocalDate dataFinal,
            @RequestParam(name = "incluirPacientes", defaultValue = "true") boolean incluirPacientes,
            @RequestParam(name = "incluirAgenda", defaultValue = "true") boolean incluirAgenda,
            @RequestParam(name = "incluirFinanceiro", defaultValue = "true") boolean incluirFinanceiro,
            @RequestParam(name = "incluirSolicitacoes", defaultValue = "true") boolean incluirSolicitacoes,
            Model model
    ) {
        FiltroRelatorioForm filtro = filtroPadrao();
        if (dataInicial != null) {
            filtro.setDataInicial(dataInicial);
        }
        if (dataFinal != null) {
            filtro.setDataFinal(dataFinal);
        }
        filtro.setIncluirPacientes(incluirPacientes);
        filtro.setIncluirAgenda(incluirAgenda);
        filtro.setIncluirFinanceiro(incluirFinanceiro);
        filtro.setIncluirSolicitacoes(incluirSolicitacoes);
        prepararPagina(model, filtro);
        return "relatorios/index";
    }

    @GetMapping("/exportar")
    public ResponseEntity<ByteArrayResource> exportar(
            @ModelAttribute FiltroRelatorioForm filtro,
            @RequestParam FormatoRelatorio formato
    ) {
        RelatorioDocumento documento = relatoriosService.exportar(relatoriosService.gerarRelatorio(filtro), formato);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + documento.fileName() + "\"")
                .contentType(MediaType.parseMediaType(documento.contentType()))
                .contentLength(documento.content().length)
                .body(new ByteArrayResource(documento.content()));
    }

    @PostMapping("/enviar")
    public String enviarRelatorio(
            @Valid @ModelAttribute("envioRelatorioForm") EnvioRelatorioForm form,
            BindingResult bindingResult,
            @ModelAttribute("filtroRelatorioForm") FiltroRelatorioForm filtro,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararPagina(model, filtro);
            return "relatorios/index";
        }
        try {
            var result = relatoriosService.enviarRelatorioPorEmail(
                    relatoriosService.gerarRelatorio(filtro),
                    form,
                    authentication != null ? authentication.getName() : "sistema"
            );
            redirectAttributes.addFlashAttribute(result.sent() ? "successMessage" : "errorMessage", result.message());
            return "redirect:/relatorios" + queryString(filtro);
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, filtro);
            return "relatorios/index";
        }
    }

    @PostMapping("/lembretes")
    public String enviarLembrete(
            @Valid @ModelAttribute("lembreteEmailForm") LembreteEmailForm form,
            BindingResult bindingResult,
            @ModelAttribute("filtroRelatorioForm") FiltroRelatorioForm filtro,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararPagina(model, filtro);
            return "relatorios/index";
        }
        var result = relatoriosService.enviarLembrete(form, authentication != null ? authentication.getName() : "sistema");
        redirectAttributes.addFlashAttribute(result.sent() ? "successMessage" : "errorMessage", result.message());
        return "redirect:/relatorios" + queryString(filtro);
    }

    @PostMapping("/cobrancas")
    public String enviarCobranca(
            @Valid @ModelAttribute("cobrancaEmailForm") CobrancaEmailForm form,
            BindingResult bindingResult,
            @ModelAttribute("filtroRelatorioForm") FiltroRelatorioForm filtro,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararPagina(model, filtro);
            return "relatorios/index";
        }
        var result = relatoriosService.enviarCobranca(form, authentication != null ? authentication.getName() : "sistema");
        redirectAttributes.addFlashAttribute(result.sent() ? "successMessage" : "errorMessage", result.message());
        return "redirect:/relatorios" + queryString(filtro);
    }

    private void prepararPagina(Model model, FiltroRelatorioForm filtro) {
        RelatorioOperacional relatorio = relatoriosService.gerarRelatorio(filtro);
        model.addAttribute("filtroRelatorioForm", filtro);
        model.addAttribute("envioRelatorioForm", envioPadrao());
        model.addAttribute("lembreteEmailForm", lembretePadrao());
        model.addAttribute("cobrancaEmailForm", cobrancaPadrao());
        model.addAttribute("tiposLembrete", TipoLembrete.values());
        model.addAttribute("relatorio", relatorio);
        model.addAttribute("pacientes", pacienteService.listarAtivos());
        model.addAttribute("lancamentosPendentes", financeiroService.listarTodos());
    }

    private FiltroRelatorioForm filtroPadrao() {
        FiltroRelatorioForm form = new FiltroRelatorioForm();
        form.setDataInicial(LocalDate.now().minusDays(30));
        form.setDataFinal(LocalDate.now());
        return form;
    }

    private EnvioRelatorioForm envioPadrao() {
        EnvioRelatorioForm form = new EnvioRelatorioForm();
        form.setFormato(FormatoRelatorio.PDF);
        form.setAssunto("Relatório operacional - FisioGestao");
        return form;
    }

    private LembreteEmailForm lembretePadrao() {
        LembreteEmailForm form = new LembreteEmailForm();
        form.setTipo(TipoLembrete.AGENDAMENTO);
        form.setAssunto("Lembrete - FisioGestao");
        form.setDataReferencia(LocalDate.now());
        return form;
    }

    private CobrancaEmailForm cobrancaPadrao() {
        CobrancaEmailForm form = new CobrancaEmailForm();
        form.setAssunto("Cobrança / demonstrativo - FisioGestao");
        return form;
    }

    private String queryString(FiltroRelatorioForm filtro) {
        return "?dataInicial=" + filtro.getDataInicial()
                + "&dataFinal=" + filtro.getDataFinal()
                + "&incluirPacientes=" + filtro.isIncluirPacientes()
                + "&incluirAgenda=" + filtro.isIncluirAgenda()
                + "&incluirFinanceiro=" + filtro.isIncluirFinanceiro()
                + "&incluirSolicitacoes=" + filtro.isIncluirSolicitacoes();
    }
}
