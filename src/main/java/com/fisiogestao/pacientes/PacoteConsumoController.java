package com.fisiogestao.pacientes;

import com.fisiogestao.agenda.form.AgendaManualForm;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.pacientes.dto.AtendimentoAvulsoFormDTO;
import com.fisiogestao.pacientes.dto.PacienteListaDTO;
import com.fisiogestao.pacientes.dto.PlanoFormDTO;
import com.fisiogestao.pacientes.dto.PlanoResumoDTO;
import com.fisiogestao.pacientes.service.PacoteSessaoService;
import com.fisiogestao.pacientes.service.PlanoService;
import com.fisiogestao.pacientes.service.SessaoConsumoService;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/pacotes-consumo")
@RequiredArgsConstructor
public class PacoteConsumoController {

    private final PlanoService planoService;
    private final SessaoConsumoService sessaoConsumoService;
    private final PacoteSessaoService pacoteSessaoService;
    private final AgendaService agendaService;

    @GetMapping
    public String index(
            @RequestParam(name = "pacienteId", required = false) UUID pacienteId,
            @RequestParam(name = "paciente", required = false) UUID pacienteLegadoId,
            @RequestParam(name = "agendar", defaultValue = "false") boolean abrirAgendamento,
            Model model
    ) {
        UUID selecionado = pacienteId != null ? pacienteId : pacienteLegadoId;
        model.addAttribute("pacientes", planoService.listarPacientesComResumoPlano());
        model.addAttribute("pacienteSelecionadoId", selecionado);
        model.addAttribute("hoje", LocalDate.now());
        model.addAttribute("agendamentoSessaoModalOpen", abrirAgendamento && selecionado != null);
        if (selecionado != null) {
            adicionarContextoPaciente(selecionado, "PACOTE", model);
        }
        return "pacotes-consumo/index";
    }

    @GetMapping("/{pacienteId}/selecionar")
    public String selecionarPaciente(
            @PathVariable UUID pacienteId,
            @RequestParam(defaultValue = "PACOTE") String modo,
            Model model
    ) {
        adicionarContextoPaciente(pacienteId, modo, model);
        return "pacotes-consumo/fragmentos :: configuracao-e-consumo";
    }

    @PostMapping("/{pacienteId}/plano")
    public String salvarPlano(
            @PathVariable UUID pacienteId,
            @ModelAttribute PlanoFormDTO form,
            RedirectAttributes redirectAttributes
    ) {
        try {
            planoService.salvarOuAtualizar(pacienteId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Plano salvo com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/pacotes-consumo?pacienteId=" + pacienteId;
    }

    @PostMapping("/agendar-sessao")
    public String agendarSessao(
            @ModelAttribute AgendaManualForm form,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String usuario = authentication == null ? "Equipe da clínica" : authentication.getName();
            agendaService.agendarSessaoManual(form, usuario);
            redirectAttributes.addFlashAttribute("successMessage", "Sessão agendada com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return construirDestinoLegado(form.getClienteId());
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(
            @PathVariable UUID id,
            @RequestParam(name = "paciente", required = false) UUID pacienteId,
            @RequestParam(name = "pacienteId", required = false) UUID pacienteIdNovo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            pacoteSessaoService.cancelar(id);
            redirectAttributes.addFlashAttribute("successMessage", "Plano de tratamento cancelado com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return construirDestinoLegado(pacienteIdNovo != null ? pacienteIdNovo : pacienteId);
    }

    @PostMapping("/{id}/excluir")
    public String excluir(
            @PathVariable UUID id,
            @RequestParam(name = "paciente", required = false) UUID pacienteId,
            @RequestParam(name = "pacienteId", required = false) UUID pacienteIdNovo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            pacoteSessaoService.excluir(id);
            redirectAttributes.addFlashAttribute("successMessage", "Plano de tratamento excluído com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return construirDestinoLegado(pacienteIdNovo != null ? pacienteIdNovo : pacienteId);
    }

    @PostMapping("/{pacienteId}/avulso")
    public String registrarAvulso(
            @PathVariable UUID pacienteId,
            @ModelAttribute AtendimentoAvulsoFormDTO form,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String usuario = authentication == null ? "Equipe da clínica" : authentication.getName();
            sessaoConsumoService.registrarAvulso(pacienteId, form, usuario);
            redirectAttributes.addFlashAttribute("successMessage", "Atendimento avulso registrado.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/pacotes-consumo?pacienteId=" + pacienteId;
    }

    private void adicionarContextoPaciente(UUID pacienteId, String modoSolicitado, Model model) {
        PacienteListaDTO paciente = planoService.buscarResumoDTO(pacienteId);
        PlanoResumoDTO plano = planoService.buscarPlanoPorPaciente(pacienteId);
        String modo = plano == null ? "AVULSO" : modoSolicitado;
        if (!"AVULSO".equals(modo)) {
            modo = "PACOTE";
        }

        model.addAttribute("paciente", paciente);
        model.addAttribute("plano", plano);
        model.addAttribute("modo", modo);
        model.addAttribute("historico", sessaoConsumoService.historicoPorPaciente(pacienteId));
        model.addAttribute("historicoAvulso", sessaoConsumoService.historicoAvulsoPorPaciente(pacienteId));
        model.addAttribute("resumoAvulso", sessaoConsumoService.resumoAvulso(pacienteId));
        model.addAttribute("hoje", LocalDate.now());
    }

    private String construirDestinoLegado(UUID pacienteId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/pacotes-consumo");
        if (pacienteId != null) {
            builder.queryParam("paciente", pacienteId);
        }
        return "redirect:" + builder.build().encode().toUriString();
    }
}
