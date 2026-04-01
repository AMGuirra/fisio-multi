package com.fisiogestao.pacientes;

import com.fisiogestao.pacientes.form.PacoteConsumoForm;
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
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/pacotes-consumo")
@RequiredArgsConstructor
public class PacoteConsumoController {

    private final PacienteService pacienteService;
    private final PacoteSessaoService pacoteSessaoService;

    @GetMapping
    public String index(
            @RequestParam(name = "busca", required = false) String buscaPaciente,
            @RequestParam(name = "paciente", required = false) UUID pacienteId,
            @RequestParam(name = "editar", required = false) UUID pacoteEdicaoId,
            Model model
    ) {
        prepararPagina(model, pacienteId, pacoteEdicaoId, buscaPaciente, null, null);
        return "pacotes-consumo/index";
    }

    @PostMapping
    public String criar(
            @Valid @ModelAttribute("pacoteConsumoForm") PacoteConsumoForm form,
            BindingResult bindingResult,
            @RequestParam(name = "busca", required = false) String buscaPaciente,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararPagina(model, form.getClienteId(), null, buscaPaciente, form, null);
            return "pacotes-consumo/index";
        }

        try {
            PacoteSessaoEntity salvo = pacoteSessaoService.criarViaCrud(form);
            redirectAttributes.addFlashAttribute("successMessage", "Pacote criado com sucesso.");
            return construirDestino(salvo.getCliente().getId(), null, buscaPaciente);
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, form.getClienteId(), null, buscaPaciente, form, null);
            return "pacotes-consumo/index";
        }
    }

    @PostMapping("/{id}")
    public String atualizar(
            @PathVariable UUID id,
            @Valid @ModelAttribute("pacoteEdicaoForm") PacoteConsumoForm form,
            BindingResult bindingResult,
            @RequestParam(name = "busca", required = false) String buscaPaciente,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararPagina(model, form.getClienteId(), id, buscaPaciente, null, form);
            return "pacotes-consumo/index";
        }

        try {
            PacoteSessaoEntity salvo = pacoteSessaoService.atualizarViaCrud(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Pacote atualizado com sucesso.");
            return construirDestino(salvo.getCliente().getId(), id, buscaPaciente);
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, form.getClienteId(), id, buscaPaciente, null, form);
            return "pacotes-consumo/index";
        }
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(
            @PathVariable UUID id,
            @RequestParam(name = "busca", required = false) String buscaPaciente,
            @RequestParam(name = "paciente", required = false) UUID pacienteId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            pacoteSessaoService.cancelar(id);
            redirectAttributes.addFlashAttribute("successMessage", "Pacote cancelado com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return construirDestino(pacienteId, null, buscaPaciente);
    }

    @PostMapping("/{id}/excluir")
    public String excluir(
            @PathVariable UUID id,
            @RequestParam(name = "busca", required = false) String buscaPaciente,
            @RequestParam(name = "paciente", required = false) UUID pacienteId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            pacoteSessaoService.excluir(id);
            redirectAttributes.addFlashAttribute("successMessage", "Pacote excluído com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return construirDestino(pacienteId, null, buscaPaciente);
    }

    private void prepararPagina(
            Model model,
            UUID pacienteId,
            UUID pacoteEdicaoId,
            String buscaPaciente,
            PacoteConsumoForm pacoteConsumoForm,
            PacoteConsumoForm pacoteEdicaoForm
    ) {
        List<ClienteEntity> pacientesAtivos = pacienteService.listarAtivos();
        List<ClienteEntity> pacientes = filtrarPacientes(pacientesAtivos, buscaPaciente);
        UUID pacienteSelecionadoId = resolverPacienteSelecionado(model, pacientesAtivos, pacienteId);
        ClienteEntity pacienteSelecionado = pacienteSelecionadoId == null
                ? null
                : pacienteService.buscarPorId(pacienteSelecionadoId);

        List<PacoteSessaoEntity> pacotes = pacienteSelecionadoId == null
                ? List.of()
                : pacoteSessaoService.listarPorCliente(pacienteSelecionadoId);
        PacoteSessaoEntity pacoteEmEdicao = pacotes.stream()
                .filter(pacote -> pacote.getId().equals(pacoteEdicaoId))
                .findFirst()
                .orElse(null);

        PacoteConsumoForm novoForm = pacoteConsumoForm != null ? pacoteConsumoForm : criarFormPadrao(pacienteSelecionadoId);
        if (novoForm.getClienteId() == null) {
            novoForm.setClienteId(pacienteSelecionadoId);
        }

        PacoteConsumoForm editarForm = pacoteEdicaoForm != null
                ? pacoteEdicaoForm
                : (pacoteEmEdicao != null ? paraForm(pacoteEmEdicao) : criarFormPadrao(pacienteSelecionadoId));
        if (editarForm.getClienteId() == null) {
            editarForm.setClienteId(pacienteSelecionadoId);
        }

        int totalContratadas = pacotes.stream().mapToInt(PacoteSessaoEntity::getSessoesContratadas).sum();
        int totalConsumidas = pacotes.stream().mapToInt(PacoteSessaoEntity::getSessoesConsumidas).sum();

        model.addAttribute("pacientes", pacientes);
        model.addAttribute("buscaPaciente", buscaPaciente == null ? "" : buscaPaciente.trim());
        model.addAttribute("totalPacientesAtivos", pacientesAtivos.size());
        model.addAttribute("totalPacientesEncontrados", pacientes.size());
        model.addAttribute("pacienteSelecionadoId", pacienteSelecionadoId);
        model.addAttribute("pacienteSelecionado", pacienteSelecionado);
        model.addAttribute("pacotes", pacotes);
        model.addAttribute("pacoteEmEdicao", pacoteEmEdicao);
        model.addAttribute("pacoteConsumoForm", novoForm);
        model.addAttribute("pacoteEdicaoForm", editarForm);
        model.addAttribute("statusPacoteDisponiveis", PacoteSessaoStatus.values());
        model.addAttribute("totalPacotes", pacotes.size());
        model.addAttribute("totalSessoesContratadas", totalContratadas);
        model.addAttribute("totalSessoesConsumidas", totalConsumidas);
        model.addAttribute("totalSessoesDisponiveis", Math.max(0, totalContratadas - totalConsumidas));
    }

    private UUID resolverPacienteSelecionado(Model model, List<ClienteEntity> pacientes, UUID solicitadoId) {
        if (solicitadoId == null || pacientes.isEmpty()) {
            return null;
        }
        boolean existe = pacientes.stream().anyMatch(paciente -> paciente.getId().equals(solicitadoId));
        if (!existe && !model.containsAttribute("errorMessage")) {
            model.addAttribute("errorMessage", "Paciente selecionado não está disponível.");
        }
        return existe ? solicitadoId : null;
    }

    private List<ClienteEntity> filtrarPacientes(List<ClienteEntity> pacientes, String buscaPaciente) {
        if (buscaPaciente == null || buscaPaciente.isBlank()) {
            return pacientes;
        }

        String termo = buscaPaciente.trim().toLowerCase();
        String termoTelefone = termo.replaceAll("\\D", "");

        return pacientes.stream()
                .filter(paciente -> correspondeBusca(paciente, termo, termoTelefone))
                .toList();
    }

    private boolean correspondeBusca(ClienteEntity paciente, String termo, String termoTelefone) {
        boolean correspondeNome = paciente.getNomeCompleto() != null
                && paciente.getNomeCompleto().toLowerCase().contains(termo);
        boolean correspondeEmail = paciente.getEmail() != null
                && paciente.getEmail().toLowerCase().contains(termo);
        boolean correspondeTelefone = paciente.getTelefone() != null
                && !termoTelefone.isBlank()
                && paciente.getTelefone().replaceAll("\\D", "").contains(termoTelefone);
        return correspondeNome || correspondeEmail || correspondeTelefone;
    }

    private String construirDestino(UUID pacienteId, UUID pacoteEdicaoId, String buscaPaciente) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/pacotes-consumo");
        if (buscaPaciente != null && !buscaPaciente.isBlank()) {
            builder.queryParam("busca", buscaPaciente.trim());
        }
        if (pacienteId != null) {
            builder.queryParam("paciente", pacienteId);
        }
        if (pacoteEdicaoId != null) {
            builder.queryParam("editar", pacoteEdicaoId);
        }
        return "redirect:" + builder.build().encode().toUriString();
    }

    private PacoteConsumoForm criarFormPadrao(UUID pacienteId) {
        PacoteConsumoForm form = new PacoteConsumoForm();
        form.setClienteId(pacienteId);
        form.setSessoesContratadas(10);
        form.setSessoesConsumidas(0);
        form.setDataInicio(LocalDate.now());
        form.setStatus(PacoteSessaoStatus.ATIVO);
        return form;
    }

    private PacoteConsumoForm paraForm(PacoteSessaoEntity pacote) {
        PacoteConsumoForm form = new PacoteConsumoForm();
        form.setClienteId(pacote.getCliente().getId());
        form.setNomePacote(pacote.getNomePacote());
        form.setSessoesContratadas(pacote.getSessoesContratadas());
        form.setSessoesConsumidas(pacote.getSessoesConsumidas());
        form.setValorTotal(pacote.getValorTotal());
        form.setDataInicio(pacote.getDataInicio());
        form.setDataValidade(pacote.getDataValidade());
        form.setStatus(pacote.getStatus());
        return form;
    }
}
