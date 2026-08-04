package com.fisiogestao.prontuario;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.prontuario.dto.ProntuarioAnexoResumoDto;
import com.fisiogestao.prontuario.form.AvaliacaoForm;
import com.fisiogestao.prontuario.form.EvolucaoForm;
import com.fisiogestao.prontuario.service.ProntuarioAnexoService;
import com.fisiogestao.prontuario.service.ProntuarioService;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private static final int MINIATURA_LADO_MAXIMO = 320;

    private final ProntuarioService prontuarioService;
    private final ProntuarioAnexoService prontuarioAnexoService;
    private final PacienteService pacienteService;
    private final AgendaService agendaService;

    // -------------------------------------------------------------------------
    // Página principal
    // -------------------------------------------------------------------------

    @GetMapping
    @PreAuthorize("@menuAccessSecurity.canAccess('PRONTUARIO')")
    public String index(
            @RequestParam(name = "paciente", required = false) UUID pacienteId,
            Model model
    ) {
        List<ClienteEntity> pacientes = pacienteService.listarAtivos();
        UUID pacienteSelecionadoId = resolverPacienteSelecionado(model, pacientes, pacienteId);

        AvaliacaoForm avaliacaoForm = new AvaliacaoForm();
        avaliacaoForm.setClienteId(pacienteSelecionadoId);
        EvolucaoForm evolucaoForm = new EvolucaoForm();
        evolucaoForm.setClienteId(pacienteSelecionadoId);

        prepararPagina(model, pacientes, pacienteSelecionadoId, avaliacaoForm, evolucaoForm);
        return "prontuario/index";
    }

    // -------------------------------------------------------------------------
    // Avaliação
    // -------------------------------------------------------------------------

    @PostMapping("/avaliacao")
    @PreAuthorize("@menuAccessSecurity.canAccess('PRONTUARIO')")
    public String salvarAvaliacao(
            @Valid @ModelAttribute("avaliacaoForm") AvaliacaoForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        List<ClienteEntity> pacientes = pacienteService.listarAtivos();
        UUID pacienteSelecionadoId = resolverPacienteSelecionado(model, pacientes, form.getClienteId());

        if (bindingResult.hasErrors()) {
            prepararPagina(model, pacientes, pacienteSelecionadoId, form, new EvolucaoForm());
            return "prontuario/index";
        }

        try {
            String usuario = authentication != null ? authentication.getName() : "sistema";
            prontuarioService.salvarAvaliacao(form, usuario);
            redirectAttributes.addFlashAttribute("successMessage", "Avaliação salva com sucesso.");
            return "redirect:/prontuario?paciente=" + form.getClienteId() + "#avaliacao";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, pacientes, pacienteSelecionadoId, form, new EvolucaoForm());
            return "prontuario/index";
        }
    }

    // -------------------------------------------------------------------------
    // Evolução
    // -------------------------------------------------------------------------

    @PostMapping("/evolucao")
    @PreAuthorize("@menuAccessSecurity.canAccess('PRONTUARIO')")
    public String registrarEvolucao(
            @Valid @ModelAttribute("evolucaoForm") EvolucaoForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        List<ClienteEntity> pacientes = pacienteService.listarAtivos();
        UUID pacienteSelecionadoId = resolverPacienteSelecionado(model, pacientes, form.getClienteId());

        if (bindingResult.hasErrors()) {
            prepararPagina(model, pacientes, pacienteSelecionadoId, new AvaliacaoForm(), form);
            return "prontuario/index";
        }

        try {
            String usuario = authentication != null ? authentication.getName() : "sistema";
            prontuarioService.registrarEvolucao(form, usuario);
            redirectAttributes.addFlashAttribute("successMessage", "Evolução registrada com sucesso.");
            return "redirect:/prontuario?paciente=" + form.getClienteId() + "#historicoEvolucao";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, pacientes, pacienteSelecionadoId, new AvaliacaoForm(), form);
            return "prontuario/index";
        }
    }

    // -------------------------------------------------------------------------
    // Anexos — upload (documentos genéricos)
    // -------------------------------------------------------------------------

    @PostMapping("/anexo")
    @PreAuthorize("@menuAccessSecurity.canAccess('PRONTUARIO')")
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
        return "redirect:/prontuario?paciente=" + clienteId + "#documentosAnexos";
    }

    // -------------------------------------------------------------------------
    // Anexos — visualização / download
    // -------------------------------------------------------------------------

    @GetMapping("/anexo/{id}")
    @PreAuthorize("@menuAccessSecurity.canAccess('PRONTUARIO')")
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
        headers.setCacheControl(CacheControl.noStore());
        headers.set("X-Content-Type-Options", "nosniff");

        return ResponseEntity.ok()
                .headers(headers)
                .body(anexo.getDados());
    }

    /** Miniatura de uma foto clínica, para exibição no histórico sem transferir o binário completo. */
    @GetMapping("/foto/{id}/miniatura")
    @PreAuthorize("@menuAccessSecurity.canAccess('PRONTUARIO')")
    public ResponseEntity<byte[]> miniaturaFoto(@PathVariable UUID id) {
        byte[] miniatura;
        try {
            miniatura = prontuarioAnexoService.gerarMiniatura(id, MINIATURA_LADO_MAXIMO);
        } catch (BusinessException ex) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.setCacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate());
        headers.set("X-Content-Type-Options", "nosniff");

        return ResponseEntity.ok().headers(headers).body(miniatura);
    }

    // -------------------------------------------------------------------------
    // Anexos — exclusão
    // -------------------------------------------------------------------------

    @PostMapping("/anexo/{id}/excluir")
    @PreAuthorize("@menuAccessSecurity.canAccess('PRONTUARIO')")
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
        return "redirect:/prontuario?paciente=" + clienteId + "#documentosAnexos";
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void prepararPagina(
            Model model,
            List<ClienteEntity> pacientes,
            UUID pacienteSelecionadoId,
            AvaliacaoForm avaliacaoForm,
            EvolucaoForm evolucaoForm
    ) {
        model.addAttribute("pacientes", pacientes);
        model.addAttribute("avaliacaoForm", avaliacaoForm);
        model.addAttribute("evolucaoForm", evolucaoForm);
        model.addAttribute("pacienteSelecionadoId", pacienteSelecionadoId);

        if (pacienteSelecionadoId == null) {
            model.addAttribute("pacienteSelecionado", null);
            model.addAttribute("sessoesPaciente", List.of());
            model.addAttribute("avaliacao", null);
            model.addAttribute("evolucoes", List.of());
            model.addAttribute("documentos", List.of());
            model.addAttribute("fotosPorEvolucao", Map.of());
            model.addAttribute("totalRegistros", 0);
            return;
        }

        avaliacaoForm.setClienteId(pacienteSelecionadoId);
        evolucaoForm.setClienteId(pacienteSelecionadoId);

        ClienteEntity pacienteSelecionado = pacientes.stream()
                .filter(paciente -> pacienteSelecionadoId.equals(paciente.getId()))
                .findFirst()
                .orElse(null);

        List<SessaoEntity> sessoesPaciente = agendaService.listarSessoesDoPaciente(pacienteSelecionadoId).stream()
                .filter(sessao -> sessao.getStatus() != SessaoStatus.CANCELADA)
                .toList();

        List<ProntuarioSoapEntity> evolucoes = prontuarioService.listarEvolucoes(pacienteSelecionadoId);
        List<ProntuarioAnexoResumoDto> fotos = prontuarioAnexoService.listarPorPaciente(pacienteSelecionadoId).stream()
                .filter(ProntuarioAnexoResumoDto::isFotoClinica)
                .toList();

        Map<UUID, List<ProntuarioAnexoResumoDto>> fotosPorEvolucao = new LinkedHashMap<>();
        for (ProntuarioAnexoResumoDto foto : fotos) {
            if (foto.getProntuarioSoapId() != null) {
                fotosPorEvolucao.computeIfAbsent(foto.getProntuarioSoapId(), key -> new java.util.ArrayList<>()).add(foto);
            }
        }

        model.addAttribute("pacienteSelecionado", pacienteSelecionado);
        model.addAttribute("sessoesPaciente", sessoesPaciente);
        model.addAttribute("avaliacao", prontuarioService.buscarAvaliacao(pacienteSelecionadoId).orElse(null));
        model.addAttribute("evolucoes", evolucoes);
        model.addAttribute("documentos", prontuarioAnexoService.listarDocumentosPorPaciente(pacienteSelecionadoId));
        model.addAttribute("fotosPorEvolucao", fotosPorEvolucao);
        model.addAttribute("totalRegistros", evolucoes.size());
    }

    private UUID resolverPacienteSelecionado(Model model, List<ClienteEntity> pacientes, UUID solicitadoId) {
        if (pacientes.isEmpty()) {
            return null;
        }

        if (solicitadoId == null) {
            return null;
        }

        boolean encontrado = pacientes.stream().anyMatch(paciente -> solicitadoId.equals(paciente.getId()));
        if (encontrado) {
            return solicitadoId;
        }

        if (!model.containsAttribute("errorMessage")) {
            model.addAttribute("errorMessage", "Paciente selecionado não está disponível.");
        }

        return null;
    }
}
