package com.fisiogestao.prontuario;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.prontuario.form.ProntuarioSoapForm;
import com.fisiogestao.prontuario.service.ProntuarioService;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/prontuario")
@RequiredArgsConstructor
public class ProntuarioController {

    private final ProntuarioService prontuarioService;
    private final PacienteService pacienteService;
    private final AgendaService agendaService;

    @GetMapping
    public String index(
            @RequestParam(name = "paciente", required = false) UUID pacienteId,
            Model model
    ) {
        List<ClienteEntity> pacientes = pacienteService.listarAtivos();
        UUID pacienteSelecionadoId = resolverPacienteSelecionado(model, pacientes, pacienteId);

        ProntuarioSoapForm prontuarioForm = new ProntuarioSoapForm();
        prontuarioForm.setClienteId(pacienteSelecionadoId);

        prepararPagina(model, pacientes, pacienteSelecionadoId, prontuarioForm);
        return "prontuario/index";
    }

    @PostMapping("/soap")
    public String registrarSoap(
            @Valid @ModelAttribute("prontuarioForm") ProntuarioSoapForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        List<ClienteEntity> pacientes = pacienteService.listarAtivos();
        UUID pacienteSelecionadoId = resolverPacienteSelecionado(model, pacientes, form.getClienteId());

        if (bindingResult.hasErrors()) {
            prepararPagina(model, pacientes, pacienteSelecionadoId, form);
            return "prontuario/index";
        }

        try {
            String usuario = authentication != null ? authentication.getName() : "sistema";
            prontuarioService.registrarSoap(form, usuario);
            redirectAttributes.addFlashAttribute("successMessage", "Registro SOAP salvo com sucesso.");
            return "redirect:/prontuario?paciente=" + form.getClienteId();
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, pacientes, pacienteSelecionadoId, form);
            return "prontuario/index";
        }
    }

    private void prepararPagina(
            Model model,
            List<ClienteEntity> pacientes,
            UUID pacienteSelecionadoId,
            ProntuarioSoapForm form
    ) {
        model.addAttribute("pacientes", pacientes);
        model.addAttribute("prontuarioForm", form);
        model.addAttribute("pacienteSelecionadoId", pacienteSelecionadoId);

        if (pacienteSelecionadoId == null) {
            form.setClienteId(null);
            model.addAttribute("pacienteSelecionado", null);
            model.addAttribute("sessoesPaciente", List.of());
            model.addAttribute("prontuarios", List.of());
            return;
        }

        form.setClienteId(pacienteSelecionadoId);

        ClienteEntity pacienteSelecionado = pacientes.stream()
                .filter(paciente -> pacienteSelecionadoId.equals(paciente.getId()))
                .findFirst()
                .orElse(null);

        List<SessaoEntity> sessoesPaciente = agendaService.listarSessoesDoPaciente(pacienteSelecionadoId).stream()
                .filter(sessao -> sessao.getStatus() != SessaoStatus.CANCELADA)
                .toList();

        model.addAttribute("pacienteSelecionado", pacienteSelecionado);
        model.addAttribute("sessoesPaciente", sessoesPaciente);
        model.addAttribute("prontuarios", prontuarioService.listarPorPaciente(pacienteSelecionadoId));
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
}
