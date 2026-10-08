package com.fisiogestao.gestor;

import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.form.AuditFilterForm;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.auth.InMemoryUserCatalog;
import com.fisiogestao.cancelamento.CancelamentoService;
import com.fisiogestao.cancelamento.StatusSolicitacaoCancelamento;
import com.fisiogestao.empresa.ConfiguracaoPlataformaEntity;
import com.fisiogestao.empresa.ConfiguracaoPlataformaService;
import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.empresa.EmpresaRepository;
import com.fisiogestao.empresa.EmpresaStatus;
import com.fisiogestao.empresa.PeriodoTesteService;
import com.fisiogestao.gestor.form.EmpresaForm;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.suporte.StatusSolicitacaoSuporte;
import com.fisiogestao.suporte.SuporteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Área Gestor: exclusiva do SUPER_ADMIN (SecurityConfig + @PreAuthorize + services).
 * Mostra apenas dados administrativos das clínicas — nunca dados clínicos ou financeiros.
 */
@Controller
@RequestMapping("/gestor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class GestorController {

    private final GestorDashboardService dashboardService;
    private final EmpresaGestaoService empresaGestaoService;
    private final EmpresaRepository empresaRepository;
    private final PeriodoTesteService periodoTesteService;
    private final ConfiguracaoPlataformaService configuracaoPlataformaService;
    private final SuporteService suporteService;
    private final CancelamentoService cancelamentoService;
    private final InMemoryUserCatalog userCatalog;
    private final AuditService auditService;

    // ── Painel ─────────────────────────────────────────────────────────────────

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("painel", dashboardService.carregar());
        return "gestor/dashboard";
    }

    @PostMapping("/notificacoes/{id}/lida")
    public String marcarLida(@PathVariable UUID id) {
        dashboardService.marcarNotificacaoComoLida(id);
        return "redirect:/gestor";
    }

    @PostMapping("/notificacoes/lidas")
    public String marcarTodasLidas() {
        dashboardService.marcarTodasComoLidas();
        return "redirect:/gestor";
    }

    // ── Empresas ───────────────────────────────────────────────────────────────

    @GetMapping("/empresas")
    public String empresas(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) EmpresaStatus status,
            @RequestParam(defaultValue = "0") int pagina,
            Model model
    ) {
        model.addAttribute("pagina", empresaGestaoService.buscar(termo, status, pagina));
        model.addAttribute("termo", termo);
        model.addAttribute("statusSelecionado", status);
        model.addAttribute("statusOpcoes", EmpresaStatus.values());
        return "gestor/empresas";
    }

    @GetMapping("/empresas/nova")
    public String novaEmpresa(Model model) {
        if (!model.containsAttribute("form")) {
            EmpresaForm form = new EmpresaForm();
            form.setQuantidadeDiasUteisTeste(configuracaoPlataformaService.diasUteisTestePadrao());
            model.addAttribute("form", form);
        }
        model.addAttribute("statusOpcoes", EmpresaStatus.values());
        return "gestor/empresa-form";
    }

    @PostMapping("/empresas/nova")
    public String criarEmpresa(@Valid @ModelAttribute("form") EmpresaForm form, BindingResult bindingResult,
                               Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("statusOpcoes", EmpresaStatus.values());
            return "gestor/empresa-form";
        }
        try {
            EmpresaEntity criada = empresaGestaoService.criar(form);
            redirectAttributes.addFlashAttribute("successMessage", "Empresa cadastrada com sucesso.");
            return "redirect:/gestor/empresas/" + criada.getId();
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("statusOpcoes", EmpresaStatus.values());
            return "gestor/empresa-form";
        }
    }

    @GetMapping("/empresas/{id}")
    public String detalheEmpresa(@PathVariable UUID id, Model model) {
        EmpresaEntity empresa = empresaGestaoService.buscarPorId(id);
        model.addAttribute("empresa", empresa);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", EmpresaForm.de(empresa, PeriodoTesteService.zonaPlataforma()));
        }
        model.addAttribute("diasRestantes", empresa.isEmTeste() ? periodoTesteService.diasUteisRestantes(empresa) : null);
        model.addAttribute("equipe", userCatalog.listarPorEmpresa(id));
        model.addAttribute("diasTestePadrao", configuracaoPlataformaService.diasUteisTestePadrao());
        model.addAttribute("statusOpcoes", EmpresaStatus.values());
        return "gestor/empresa-detalhe";
    }

    @PostMapping("/empresas/{id}")
    public String atualizarEmpresa(@PathVariable UUID id, @Valid @ModelAttribute("form") EmpresaForm form,
                                   BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Revise os dados informados.");
            return "redirect:/gestor/empresas/" + id;
        }
        return executar(id, redirectAttributes, "Dados atualizados.", () -> empresaGestaoService.atualizarDados(id, form));
    }

    @PostMapping("/empresas/{id}/ativar")
    public String ativar(@PathVariable UUID id, @RequestParam(required = false) String observacao, RedirectAttributes ra) {
        return executar(id, ra, "Empresa ativada.", () -> empresaGestaoService.ativar(id, observacao));
    }

    @PostMapping("/empresas/{id}/bloquear")
    public String bloquear(@PathVariable UUID id, @RequestParam(required = false) String motivo, RedirectAttributes ra) {
        return executar(id, ra, "Empresa bloqueada. As sessões ativas serão encerradas na próxima requisição.",
                () -> empresaGestaoService.bloquear(id, motivo));
    }

    @PostMapping("/empresas/{id}/inativar")
    public String inativar(@PathVariable UUID id, @RequestParam(required = false) String motivo, RedirectAttributes ra) {
        return executar(id, ra, "Empresa inativada. As sessões ativas serão encerradas na próxima requisição.",
                () -> empresaGestaoService.inativar(id, motivo));
    }

    @PostMapping("/empresas/{id}/teste/iniciar")
    public String iniciarTeste(@PathVariable UUID id, @RequestParam(required = false) Integer dias, RedirectAttributes ra) {
        return executar(id, ra, "Período de teste iniciado.", () -> empresaGestaoService.iniciarTeste(id, dias));
    }

    @PostMapping("/empresas/{id}/teste/prorrogar")
    public String prorrogarTeste(@PathVariable UUID id, @RequestParam(required = false) Integer dias, RedirectAttributes ra) {
        return executar(id, ra, "Teste prorrogado.", () -> empresaGestaoService.prorrogarTeste(id, dias));
    }

    @PostMapping("/empresas/{id}/teste/dias")
    public String alterarDiasTeste(@PathVariable UUID id, @RequestParam(required = false) Integer dias, RedirectAttributes ra) {
        return executar(id, ra, "Quantidade de dias úteis atualizada.", () -> empresaGestaoService.alterarDiasTeste(id, dias));
    }

    @PostMapping("/empresas/{id}/teste/data-final")
    public String definirDataFinal(@PathVariable UUID id,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
                                   RedirectAttributes ra) {
        return executar(id, ra, "Data final do teste atualizada.", () -> empresaGestaoService.definirDataFinalTeste(id, dataFinal));
    }

    // ── Suporte ────────────────────────────────────────────────────────────────

    @GetMapping("/suporte")
    public String suporte(@RequestParam(required = false) String termo,
                          @RequestParam(required = false) StatusSolicitacaoSuporte status,
                          @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("pagina", suporteService.buscar(termo, status, pagina));
        model.addAttribute("termo", termo);
        model.addAttribute("statusSelecionado", status);
        model.addAttribute("statusOpcoes", StatusSolicitacaoSuporte.values());
        model.addAttribute("empresas", nomesEmpresas());
        return "gestor/suporte";
    }

    @GetMapping("/suporte/{id}")
    public String suporteDetalhe(@PathVariable UUID id, Model model) {
        var solicitacao = suporteService.buscarPorId(id);
        model.addAttribute("solicitacao", solicitacao);
        model.addAttribute("empresa", solicitacao.getEmpresaId() == null ? null
                : empresaRepository.findById(solicitacao.getEmpresaId()).orElse(null));
        model.addAttribute("statusOpcoes", StatusSolicitacaoSuporte.values());
        return "gestor/suporte-detalhe";
    }

    @PostMapping("/suporte/{id}/status")
    public String suporteStatus(@PathVariable UUID id, @RequestParam(required = false) StatusSolicitacaoSuporte status,
                                @RequestParam(required = false) String observacao, RedirectAttributes ra) {
        try {
            suporteService.atualizarStatus(id, status, observacao);
            ra.addFlashAttribute("successMessage", "Status atualizado.");
        } catch (BusinessException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/gestor/suporte/" + id;
    }

    // ── Cancelamentos ──────────────────────────────────────────────────────────

    @GetMapping("/cancelamentos")
    public String cancelamentos(@RequestParam(required = false) StatusSolicitacaoCancelamento status,
                                @RequestParam(required = false) UUID empresaId,
                                @RequestParam(required = false) String responsavel,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                                @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("pagina", cancelamentoService.buscar(status, empresaId, responsavel, de, ate, pagina));
        model.addAttribute("statusSelecionado", status);
        model.addAttribute("empresaSelecionada", empresaId);
        model.addAttribute("responsavel", responsavel);
        model.addAttribute("de", de);
        model.addAttribute("ate", ate);
        model.addAttribute("statusOpcoes", StatusSolicitacaoCancelamento.values());
        model.addAttribute("empresas", nomesEmpresas());
        return "gestor/cancelamentos";
    }

    @GetMapping("/cancelamentos/{id}")
    public String cancelamentoDetalhe(@PathVariable UUID id, Model model) {
        var solicitacao = cancelamentoService.detalhar(id);
        model.addAttribute("solicitacao", solicitacao);
        model.addAttribute("empresa", solicitacao.getEmpresaId() == null ? null
                : empresaRepository.findById(solicitacao.getEmpresaId()).orElse(null));
        model.addAttribute("empresas", nomesEmpresas());
        model.addAttribute("hoje", PeriodoTesteService.hoje());
        return "gestor/cancelamento-detalhe";
    }

    @PostMapping("/cancelamentos/{id}/analise")
    public String cancelamentoAnalise(@PathVariable UUID id, @RequestParam(required = false) String observacao, RedirectAttributes ra) {
        return acaoCancelamento(id, ra, "Solicitação em análise.", () -> cancelamentoService.colocarEmAnalise(id, observacao));
    }

    @PostMapping("/cancelamentos/{id}/recusar")
    public String cancelamentoRecusar(@PathVariable UUID id, @RequestParam(required = false) String observacao, RedirectAttributes ra) {
        return acaoCancelamento(id, ra, "Solicitação recusada.", () -> cancelamentoService.recusar(id, observacao));
    }

    @PostMapping("/cancelamentos/{id}/agendar")
    public String cancelamentoAgendar(@PathVariable UUID id,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                      @RequestParam(required = false) String observacao, RedirectAttributes ra) {
        return acaoCancelamento(id, ra, "Cancelamento agendado.", () -> cancelamentoService.agendar(id, data, observacao));
    }

    @PostMapping("/cancelamentos/{id}/concluir")
    public String cancelamentoConcluir(@PathVariable UUID id, @RequestParam(required = false) String observacao, RedirectAttributes ra) {
        return acaoCancelamento(id, ra, "Cancelamento concluído: a empresa foi inativada e os dados foram preservados.",
                () -> cancelamentoService.concluir(id, observacao));
    }

    @PostMapping("/cancelamentos/{id}/vincular")
    public String cancelamentoVincular(@PathVariable UUID id, @RequestParam(required = false) UUID empresaId, RedirectAttributes ra) {
        if (empresaId == null) {
            ra.addFlashAttribute("errorMessage", "Selecione a empresa.");
            return "redirect:/gestor/cancelamentos/" + id;
        }
        return acaoCancelamento(id, ra, "Solicitação vinculada à empresa.", () -> cancelamentoService.vincularEmpresa(id, empresaId));
    }

    // ── Auditoria global ───────────────────────────────────────────────────────

    @GetMapping("/auditoria")
    public String auditoria(@ModelAttribute("filtro") AuditFilterForm filtro, Model model) {
        // Superadmin não tem empresa no contexto: AuditService lista apenas eventos da plataforma.
        var eventos = auditService.list(filtro);
        model.addAttribute("eventos", eventos.size() > 300 ? eventos.subList(0, 300) : eventos);
        model.addAttribute("total", eventos.size());
        return "gestor/auditoria";
    }

    // ── Configurações globais ──────────────────────────────────────────────────

    @GetMapping("/configuracoes")
    public String configuracoes(Model model) {
        model.addAttribute("config", configuracaoPlataformaService.obter());
        return "gestor/configuracoes";
    }

    @PostMapping("/configuracoes")
    public String salvarConfiguracoes(@RequestParam(required = false) Integer diasUteisTestePadrao,
                                      @RequestParam(required = false) Integer diasUteisAvisoVencimento,
                                      @RequestParam(required = false) String emailNotificacoes,
                                      RedirectAttributes ra) {
        try {
            ConfiguracaoPlataformaEntity antes = configuracaoPlataformaService.obter();
            Map<String, Object> anterior = snapshotConfig(antes);
            ConfiguracaoPlataformaEntity salva = configuracaoPlataformaService.salvar(
                    diasUteisTestePadrao, diasUteisAvisoVencimento, emailNotificacoes);
            auditService.recordManual(AuditModule.GESTOR, "configuracao_plataforma", salva.getId().toString(),
                    "Configurações globais", AuditActionType.UPDATE, anterior, snapshotConfig(salva), null,
                    "Configurações globais da plataforma atualizadas.", AuditEventOrigin.MANUAL);
            ra.addFlashAttribute("successMessage", "Configurações salvas.");
        } catch (BusinessException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/gestor/configuracoes";
    }

    // ── internos ───────────────────────────────────────────────────────────────

    private String executar(UUID id, RedirectAttributes ra, String sucesso, Supplier<?> acao) {
        try {
            acao.get();
            ra.addFlashAttribute("successMessage", sucesso);
        } catch (BusinessException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/gestor/empresas/" + id;
    }

    private String acaoCancelamento(UUID id, RedirectAttributes ra, String sucesso, Supplier<?> acao) {
        try {
            acao.get();
            ra.addFlashAttribute("successMessage", sucesso);
        } catch (BusinessException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/gestor/cancelamentos/" + id;
    }

    private Map<UUID, String> nomesEmpresas() {
        Map<UUID, String> nomes = new LinkedHashMap<>();
        empresaRepository.findAll(org.springframework.data.domain.Sort.by("nome"))
                .forEach(e -> nomes.put(e.getId(), e.getNome()));
        return nomes;
    }

    private static Map<String, Object> snapshotConfig(ConfiguracaoPlataformaEntity config) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("diasUteisTestePadrao", config.getDiasUteisTestePadrao());
        mapa.put("diasUteisAvisoVencimento", config.getDiasUteisAvisoVencimento());
        mapa.put("emailNotificacoes", config.getEmailNotificacoes());
        return mapa;
    }
}
