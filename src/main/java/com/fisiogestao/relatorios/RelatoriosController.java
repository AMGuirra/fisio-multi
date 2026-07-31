package com.fisiogestao.relatorios;

import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.financeiro.service.FinanceiroService;
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

import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.util.Objects;

@Controller
@RequestMapping({ "/relatorios", "/relatorio" })
@RequiredArgsConstructor
public class RelatoriosController {

    private static final String ABA_PADRAO = "visao-geral";

    private final RelatoriosService relatoriosService;
    private final AuditService auditService;
    private final FinanceiroService financeiroService;

    @GetMapping
    public String index(
            @RequestParam(name = "dataInicial", required = false) LocalDate dataInicial,
            @RequestParam(name = "dataFinal", required = false) LocalDate dataFinal,
            @RequestParam(name = "incluirPacientes", defaultValue = "true") boolean incluirPacientes,
            @RequestParam(name = "incluirAgenda", defaultValue = "true") boolean incluirAgenda,
            @RequestParam(name = "incluirFinanceiro", defaultValue = "true") boolean incluirFinanceiro,
            @RequestParam(name = "incluirSolicitacoes", defaultValue = "true") boolean incluirSolicitacoes,
            @RequestParam(name = "aba", defaultValue = ABA_PADRAO) String aba,
            Model model) {
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
        prepararPagina(model, filtro, normalizarAba(aba));
        return "relatorios/index";
    }

    @GetMapping("/exportar")
    public ResponseEntity<ByteArrayResource> exportar(
            @ModelAttribute FiltroRelatorioForm filtro,
            @RequestParam FormatoRelatorio formato) {
        RelatoriosService.FaturamentoDetalhado faturamento = relatoriosService.gerarFaturamentoDetalhado(filtro);
        RelatorioDocumento documento = relatoriosService.exportar(
                relatoriosService.gerarRelatorio(filtro), formato, faturamento);
        String rawContentType = documento.contentType();
        String contentType = rawContentType == null || rawContentType.isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : rawContentType;
        byte[] content = Objects.requireNonNullElseGet(documento.content(), () -> new byte[0]);
        String fileName = Objects.requireNonNullElse(documento.fileName(), "relatorio-operacional.bin");
        auditService.recordManual(
                AuditModule.RELATORIOS,
                "relatorio_operacional",
                null,
                "Relatório operacional",
                formato == FormatoRelatorio.PDF ? AuditActionType.EXPORT_PDF : AuditActionType.EXPORT_EXCEL,
                null,
                payloadExportacao(filtro, formato),
                null,
                "Exportação manual de relatório operacional.",
                AuditEventOrigin.MANUAL);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(content.length)
                .body(new ByteArrayResource(content));
    }

    @PostMapping("/enviar")
    public String enviarRelatorio(
            @Valid @ModelAttribute("envioRelatorioForm") EnvioRelatorioForm form,
            BindingResult bindingResult,
            @ModelAttribute("filtroRelatorioForm") FiltroRelatorioForm filtro,
            @RequestParam(name = "aba", defaultValue = ABA_PADRAO) String aba,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        String abaAtiva = normalizarAba(aba);
        if (bindingResult.hasErrors()) {
            prepararPagina(model, filtro, abaAtiva);
            return "relatorios/index";
        }
        try {
            RelatorioOperacional relatorio = relatoriosService.gerarRelatorio(filtro);
            RelatoriosService.FaturamentoDetalhado faturamento = relatoriosService.gerarFaturamentoDetalhado(filtro);
            var result = relatoriosService.enviarRelatorioPorEmail(
                    relatorio,
                    faturamento,
                    form,
                    authentication != null ? authentication.getName() : "sistema");
            redirectAttributes.addFlashAttribute(result.sent() ? "successMessage" : "errorMessage", result.message());
            return "redirect:/relatorios" + queryString(filtro, abaAtiva);
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, filtro, abaAtiva);
            return "relatorios/index";
        }
    }

    @PostMapping("/lembretes")
    public String enviarLembrete(
            @Valid @ModelAttribute("lembreteEmailForm") LembreteEmailForm form,
            BindingResult bindingResult,
            @ModelAttribute("filtroRelatorioForm") FiltroRelatorioForm filtro,
            @RequestParam(name = "aba", defaultValue = ABA_PADRAO) String aba,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        String abaAtiva = normalizarAba(aba);
        if (bindingResult.hasErrors()) {
            prepararPagina(model, filtro, abaAtiva);
            return "relatorios/index";
        }
        try {
            var result = relatoriosService.enviarLembrete(form,
                    authentication != null ? authentication.getName() : "sistema");
            redirectAttributes.addFlashAttribute(result.sent() ? "successMessage" : "errorMessage", result.message());
            return "redirect:/relatorios" + queryString(filtro, abaAtiva);
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, filtro, abaAtiva);
            return "relatorios/index";
        }
    }

    @PostMapping("/cobrancas")
    public String enviarCobranca(
            @Valid @ModelAttribute("cobrancaEmailForm") CobrancaEmailForm form,
            BindingResult bindingResult,
            @ModelAttribute("filtroRelatorioForm") FiltroRelatorioForm filtro,
            @RequestParam(name = "aba", defaultValue = ABA_PADRAO) String aba,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        String abaAtiva = normalizarAba(aba);
        if (bindingResult.hasErrors()) {
            prepararPagina(model, filtro, abaAtiva);
            return "relatorios/index";
        }
        try {
            var result = relatoriosService.enviarCobranca(form,
                    authentication != null ? authentication.getName() : "sistema");
            redirectAttributes.addFlashAttribute(result.sent() ? "successMessage" : "errorMessage", result.message());
            return "redirect:/relatorios" + queryString(filtro, abaAtiva);
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, filtro, abaAtiva);
            return "relatorios/index";
        }
    }

    private void prepararPagina(Model model, FiltroRelatorioForm filtro, String aba) {
        RelatorioOperacional relatorio = relatoriosService.gerarRelatorio(filtro);
        RelatoriosService.FaturamentoDetalhado faturamento = relatoriosService.gerarFaturamentoDetalhado(filtro);
        String abaAtiva = "faturamento".equals(aba) && faturamento == null ? ABA_PADRAO : aba;

        model.addAttribute("filtroRelatorioForm", filtro);
        model.addAttribute("envioRelatorioForm", envioPadrao());
        model.addAttribute("lembreteEmailForm", lembretePadrao());
        model.addAttribute("cobrancaEmailForm", cobrancaPadrao());
        model.addAttribute("tiposLembrete", TipoLembrete.values());
        model.addAttribute("relatorio", relatorio);
        model.addAttribute("activeTab", abaAtiva);
        model.addAttribute("inadimplencia", financeiroService.calcularInadimplencia());
        model.addAttribute("contasAPagarPendentes", financeiroService.contarContasAPagarPendentes());
        model.addAttribute("faturamentoDetalhado", faturamento);
        model.addAttribute("blocosFaturamento", faturamento == null ? java.util.List.<FaturamentoBloco>of() : java.util.List.of(
                new FaturamentoBloco("Categoria / tipo", faturamento.relatorio().porCategoria()),
                new FaturamentoBloco("Plano", faturamento.relatorio().porPlano()),
                new FaturamentoBloco("Profissional", faturamento.relatorio().porProfissional()),
                new FaturamentoBloco("Método de pagamento", faturamento.relatorio().porMetodoPagamento()),
                new FaturamentoBloco("Tipo de atendimento", faturamento.relatorio().porTipoAtendimento())
        ));
    }

    private String normalizarAba(String aba) {
        if (aba == null) {
            return ABA_PADRAO;
        }
        return switch (aba) {
            case "faturamento", "dados", "exportar" -> aba;
            default -> ABA_PADRAO;
        };
    }

    private record FaturamentoBloco(String titulo, java.util.List<FinanceiroService.FaturamentoGrupo> itens) {}

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

    private String queryString(FiltroRelatorioForm filtro, String aba) {
        return "?dataInicial=" + filtro.getDataInicial()
                + "&dataFinal=" + filtro.getDataFinal()
                + "&incluirPacientes=" + filtro.isIncluirPacientes()
                + "&incluirAgenda=" + filtro.isIncluirAgenda()
                + "&incluirFinanceiro=" + filtro.isIncluirFinanceiro()
                + "&incluirSolicitacoes=" + filtro.isIncluirSolicitacoes()
                + "&aba=" + aba;
    }

    private LinkedHashMap<String, Object> payloadExportacao(FiltroRelatorioForm filtro, FormatoRelatorio formato) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("formato", formato != null ? formato.name() : null);
        payload.put("dataInicial", filtro.getDataInicial());
        payload.put("dataFinal", filtro.getDataFinal());
        payload.put("incluirPacientes", filtro.isIncluirPacientes());
        payload.put("incluirAgenda", filtro.isIncluirAgenda());
        payload.put("incluirFinanceiro", filtro.isIncluirFinanceiro());
        payload.put("incluirSolicitacoes", filtro.isIncluirSolicitacoes());
        return payload;
    }
}
