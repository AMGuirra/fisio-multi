package com.fisiogestao.pacientes;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.pacientes.form.PacienteForm;
import com.fisiogestao.pacientes.form.PacoteSessaoForm;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.pacientes.service.PacoteSessaoService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/pacientes")
@RequiredArgsConstructor
public class PacientesController {

    private final PacienteService pacienteService;
    private final PacoteSessaoService pacoteSessaoService;
    private final AgendaService agendaService;

    @GetMapping
    public String index(
            @RequestParam(name = "paciente", required = false) UUID pacienteId,
            Model model
    ) {
        List<ClienteEntity> pacientes = pacienteService.listarAtivos();
        UUID pacienteSelecionadoId = resolverPacienteSelecionado(model, pacientes, pacienteId);

        PacienteForm pacienteForm = new PacienteForm();
        PacoteSessaoForm pacoteForm = criarPacoteFormPadrao();

        prepararPagina(model, pacientes, pacienteSelecionadoId, pacienteForm, pacoteForm);
        return "pacientes/index";
    }

    @PostMapping
    public String criarPaciente(
            @Valid @ModelAttribute("pacienteForm") PacienteForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        List<ClienteEntity> pacientes = pacienteService.listarAtivos();

        if (bindingResult.hasErrors()) {
            UUID pacienteSelecionadoId = resolverPacienteSelecionado(model, pacientes, null);
            prepararPagina(model, pacientes, pacienteSelecionadoId, form, criarPacoteFormPadrao());
            return "pacientes/index";
        }

        try {
            ClienteEntity paciente = pacienteService.cadastrar(form);
            redirectAttributes.addFlashAttribute("successMessage", "Paciente cadastrado com sucesso.");
            return "redirect:/pacientes?paciente=" + paciente.getId();
        } catch (BusinessException ex) {
            UUID pacienteSelecionadoId = resolverPacienteSelecionado(model, pacientes, null);
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, pacientes, pacienteSelecionadoId, form, criarPacoteFormPadrao());
            return "pacientes/index";
        }
    }

    @PostMapping("/{id}/pacotes")
    public String criarPacote(
            @PathVariable UUID id,
            @Valid @ModelAttribute("pacoteForm") PacoteSessaoForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        List<ClienteEntity> pacientes = pacienteService.listarAtivos();

        if (bindingResult.hasErrors()) {
            UUID pacienteSelecionadoId = resolverPacienteSelecionado(model, pacientes, id);
            prepararPagina(model, pacientes, pacienteSelecionadoId, new PacienteForm(), form);
            return "pacientes/index";
        }

        try {
            pacoteSessaoService.criar(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Pacote cadastrado com sucesso.");
            return "redirect:/pacientes?paciente=" + id;
        } catch (BusinessException ex) {
            UUID pacienteSelecionadoId = resolverPacienteSelecionado(model, pacientes, id);
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, pacientes, pacienteSelecionadoId, new PacienteForm(), form);
            return "pacientes/index";
        }
    }

    private void prepararPagina(
            Model model,
            List<ClienteEntity> pacientes,
            UUID pacienteSelecionadoId,
            PacienteForm pacienteForm,
            PacoteSessaoForm pacoteForm
    ) {
        model.addAttribute("pacientes", pacientes);
        model.addAttribute("pacienteForm", pacienteForm);
        model.addAttribute("pacoteForm", pacoteForm);
        model.addAttribute("pacienteSelecionadoId", pacienteSelecionadoId);

        if (pacienteSelecionadoId == null) {
            model.addAttribute("pacienteSelecionado", null);
            model.addAttribute("pacotesPaciente", List.of());
            model.addAttribute("sessoesPaciente", List.of());
            model.addAttribute("pacoteAtivoPaciente", null);
            model.addAttribute("sessoesDisponiveis", 0);
            return;
        }

        ClienteEntity pacienteSelecionado = pacientes.stream()
                .filter(paciente -> pacienteSelecionadoId.equals(paciente.getId()))
                .findFirst()
                .orElse(null);
        model.addAttribute("pacienteSelecionado", pacienteSelecionado);

        List<PacoteSessaoEntity> pacotes = pacoteSessaoService.listarPorCliente(pacienteSelecionadoId);
        List<SessaoEntity> sessoesPaciente = agendaService.listarSessoesDoPaciente(pacienteSelecionadoId);
        model.addAttribute("pacotesPaciente", pacotes);
        model.addAttribute("sessoesPaciente", sessoesPaciente);
        model.addAttribute("pacoteAtivoPaciente", pacoteSessaoService.buscarPrimeiroDisponivel(pacienteSelecionadoId).orElse(null));

        int sessoesDisponiveis = pacotes.stream()
                .filter(pacote -> pacote.getStatus() == PacoteSessaoStatus.ATIVO)
                .mapToInt(pacoteSessaoService::calcularSessoesDisponiveis)
                .sum();
        model.addAttribute("sessoesDisponiveis", sessoesDisponiveis);
    }

    private UUID resolverPacienteSelecionado(Model model, List<ClienteEntity> pacientes, UUID solicitadoId) {
        if (pacientes.isEmpty()) {
            return null;
        }

        if (solicitadoId == null) {
            return pacientes.get(0).getId();
        }

        boolean encontrado = pacientes.stream().anyMatch(paciente -> solicitadoId.equals(paciente.getId()));
        if (encontrado) {
            return solicitadoId;
        }

        if (!model.containsAttribute("errorMessage")) {
            model.addAttribute("errorMessage", "Paciente selecionado não está disponível.");
        }
        return pacientes.get(0).getId();
    }

    private PacoteSessaoForm criarPacoteFormPadrao() {
        PacoteSessaoForm form = new PacoteSessaoForm();
        form.setDataInicio(LocalDate.now());
        return form;
    }
}
