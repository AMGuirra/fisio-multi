package com.fisiogestao.prontuario;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.prontuario.form.ProntuarioSoapForm;
import com.fisiogestao.prontuario.service.ProntuarioAnexoService;
import com.fisiogestao.prontuario.service.ProntuarioService;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/prontuario")
@RequiredArgsConstructor
public class ProntuarioController {

    /** Tipos MIME que podem ser exibidos inline no browser (sem forçar download). */
    private static final Set<String> TIPOS_INLINE = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final ProntuarioService prontuarioService;
    private final ProntuarioAnexoService prontuarioAnexoService;
    private final PacienteService pacienteService;
    private final AgendaService agendaService;

    // -------------------------------------------------------------------------
    // Página principal
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Registro SOAP
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Anexos — upload
    // -------------------------------------------------------------------------

    @PostMapping("/anexo")
    public String uploadAnexo(
            @RequestParam("clienteId") UUID clienteId,
            @RequestParam(value = "prontuarioSoapId", required = false) UUID prontuarioSoapId,
            @RequestParam(value = "descricao", required = false) String descricao,
            @RequestParam("arquivo") MultipartFile arquivo,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String usuario = authentication != null ? authentication.getName() : "sistema";
            prontuarioAnexoService.salvar(clienteId, prontuarioSoapId, descricao, arquivo, usuario);
            redirectAttributes.addFlashAttribute("successMessage", "Arquivo anexado com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/prontuario?paciente=" + clienteId;
    }

    // -------------------------------------------------------------------------
    // Anexos — visualização / download
    // -------------------------------------------------------------------------

    @GetMapping("/anexo/{id}")
    public ResponseEntity<byte[]> visualizarAnexo(
            @PathVariable UUID id,
            @RequestParam(value = "download", defaultValue = "false") boolean forcarDownload
    ) {
        ProntuarioAnexoEntity anexo;
        try {
            anexo = prontuarioAnexoService.buscarParaDownload(id);
        } catch (BusinessException ex) {
            return ResponseEntity.notFound().build();
        }

        boolean exibirInline = !forcarDownload && TIPOS_INLINE.contains(anexo.getTipoMime());

        ContentDisposition contentDisposition = exibirInline
                ? ContentDisposition.inline()
                        .filename(anexo.getNomeOriginal(), StandardCharsets.UTF_8)
                        .build()
                : ContentDisposition.attachment()
                        .filename(anexo.getNomeOriginal(), StandardCharsets.UTF_8)
                        .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(anexo.getTipoMime()));
        headers.setContentDisposition(contentDisposition);
        headers.setContentLength(anexo.getTamanhoBytes());

        return ResponseEntity.ok()
                .headers(headers)
                .body(anexo.getDados());
    }

    // -------------------------------------------------------------------------
    // Anexos — exclusão
    // -------------------------------------------------------------------------

    @PostMapping("/anexo/{id}/excluir")
    public String excluirAnexo(
            @PathVariable UUID id,
            @RequestParam("clienteId") UUID clienteId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String usuario = authentication != null ? authentication.getName() : "sistema";
            prontuarioAnexoService.excluir(id, usuario);
            redirectAttributes.addFlashAttribute("successMessage", "Arquivo excluído com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/prontuario?paciente=" + clienteId;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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
            model.addAttribute("anexos", List.of());
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
        model.addAttribute("anexos", prontuarioAnexoService.listarPorPaciente(pacienteSelecionadoId));
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
