package com.fisiogestao.pacientes;

import com.fisiogestao.agenda.form.AgendaManualForm;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.pacientes.form.PacoteConsumoForm;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.pacientes.service.PacoteSessaoService;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.security.core.Authentication;
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
    private final AgendaService agendaService;

    @GetMapping
    public String index(
            @RequestParam(name = "busca", required = false) String buscaPaciente,
            @RequestParam(name = "paciente", required = false) UUID pacienteId,
            @RequestParam(name = "editar", required = false) UUID pacoteEdicaoId,
            @RequestParam(name = "pacote", required = false) UUID pacoteAgendamentoId,
            @RequestParam(name = "agendar", defaultValue = "false") boolean abrirAgendamento,
            Model model
    ) {
        prepararPagina(model, pacienteId, pacoteEdicaoId, pacoteAgendamentoId, buscaPaciente, null, null, null, abrirAgendamento && pacienteId != null);
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
            prepararPagina(model, form.getClienteId(), null, null, buscaPaciente, form, null, null, false);
            return "pacotes-consumo/index";
        }

        try {
            PacoteSessaoEntity salvo = pacoteSessaoService.criarViaCrud(form);
            redirectAttributes.addFlashAttribute("successMessage", "Plano de tratamento criado com sucesso.");
            return construirDestino(salvo.getCliente().getId(), null, buscaPaciente);
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, form.getClienteId(), null, null, buscaPaciente, form, null, null, false);
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
            prepararPagina(model, form.getClienteId(), id, null, buscaPaciente, null, form, null, false);
            return "pacotes-consumo/index";
        }

        try {
            PacoteSessaoEntity salvo = pacoteSessaoService.atualizarViaCrud(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Plano de tratamento atualizado com sucesso.");
            return construirDestino(salvo.getCliente().getId(), id, buscaPaciente);
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, form.getClienteId(), id, null, buscaPaciente, null, form, null, false);
            return "pacotes-consumo/index";
        }
    }

    @PostMapping("/agendar-sessao")
    public String agendarSessao(
            @Valid @ModelAttribute("agendamentoSessaoForm") AgendaManualForm form,
            BindingResult bindingResult,
            @RequestParam(name = "busca", required = false) String buscaPaciente,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararPagina(model, form.getClienteId(), null, form.getPacoteSessaoId(), buscaPaciente, null, null, form, true);
            return "pacotes-consumo/index";
        }

        try {
            agendaService.agendarSessaoManual(form, authentication != null ? authentication.getName() : "Equipe da clínica");
            redirectAttributes.addFlashAttribute("successMessage", "Sessão agendada com sucesso.");
            return construirDestino(form.getClienteId(), null, buscaPaciente);
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, form.getClienteId(), null, form.getPacoteSessaoId(), buscaPaciente, null, null, form, true);
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
            redirectAttributes.addFlashAttribute("successMessage", "Plano de tratamento cancelado com sucesso.");
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
            redirectAttributes.addFlashAttribute("successMessage", "Plano de tratamento excluído com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return construirDestino(pacienteId, null, buscaPaciente);
    }

    private void prepararPagina(
            Model model,
            UUID pacienteId,
            UUID pacoteEdicaoId,
            UUID pacoteAgendamentoId,
            String buscaPaciente,
            PacoteConsumoForm pacoteConsumoForm,
            PacoteConsumoForm pacoteEdicaoForm,
            AgendaManualForm agendamentoSessaoForm,
            boolean agendamentoSessaoModalOpen
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
        List<PacoteSessaoEntity> pacotesDisponiveisAgenda = pacienteSelecionadoId == null
                ? List.of()
                : pacoteSessaoService.listarDisponiveisParaAgenda(pacienteSelecionadoId);
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

        AgendaManualForm agendaForm = agendamentoSessaoForm != null
                ? agendamentoSessaoForm
                : criarAgendamentoPadrao(pacienteSelecionadoId, pacotesDisponiveisAgenda, pacoteAgendamentoId);
        if (agendaForm.getClienteId() == null) {
            agendaForm.setClienteId(pacienteSelecionadoId);
        }
        if (agendaForm.getPacoteSessaoId() == null && !pacotesDisponiveisAgenda.isEmpty()) {
            agendaForm.setPacoteSessaoId(pacotesDisponiveisAgenda.get(0).getId());
        }
        boolean agendamentoPlanoTravado = pacoteAgendamentoId != null
                && agendaForm.getPacoteSessaoId() != null
                && pacoteAgendamentoId.equals(agendaForm.getPacoteSessaoId());

        int totalContratadas = pacotes.stream().mapToInt(PacoteSessaoEntity::getSessoesContratadas).sum();
        int totalConsumidas = pacotes.stream().mapToInt(PacoteSessaoEntity::getSessoesConsumidas).sum();

        model.addAttribute("pacientes", pacientes);
        model.addAttribute("buscaPaciente", buscaPaciente == null ? "" : buscaPaciente.trim());
        model.addAttribute("totalPacientesAtivos", pacientesAtivos.size());
        model.addAttribute("totalPacientesEncontrados", pacientes.size());
        model.addAttribute("pacienteSelecionadoId", pacienteSelecionadoId);
        model.addAttribute("pacienteSelecionado", pacienteSelecionado);
        model.addAttribute("pacotes", pacotes);
        model.addAttribute("pacotesDisponiveisAgenda", pacotesDisponiveisAgenda);
        model.addAttribute("pacoteEmEdicao", pacoteEmEdicao);
        model.addAttribute("pacoteConsumoForm", novoForm);
        model.addAttribute("pacoteEdicaoForm", editarForm);
        model.addAttribute("agendamentoSessaoForm", agendaForm);
        model.addAttribute("agendamentoSessaoModalOpen", agendamentoSessaoModalOpen);
        model.addAttribute("agendamentoPlanoTravado", agendamentoPlanoTravado);
        model.addAttribute("agendaConfigurada", agendaConfigurada());
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

    private AgendaManualForm criarAgendamentoPadrao(UUID pacienteId, List<PacoteSessaoEntity> pacotesDisponiveisAgenda, UUID pacoteAgendamentoId) {
        AgendaManualForm form = new AgendaManualForm();
        form.setClienteId(pacienteId);
        form.setData(LocalDate.now());
        if (pacoteAgendamentoId != null && pacotesDisponiveisAgenda.stream().anyMatch(pacote -> pacote.getId().equals(pacoteAgendamentoId))) {
            form.setPacoteSessaoId(pacoteAgendamentoId);
            return form;
        }
        if (!pacotesDisponiveisAgenda.isEmpty()) {
            form.setPacoteSessaoId(pacotesDisponiveisAgenda.get(0).getId());
        }
        return form;
    }

    private boolean agendaConfigurada() {
        try {
            agendaService.obterConfiguracaoAtiva();
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }
}
