package com.fisiogestao.portal;

import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.config.ClinicTimezoneHolder;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.PacoteSessaoEntity;
import com.fisiogestao.pacientes.PacoteSessaoStatus;
import com.fisiogestao.pacientes.repository.ClienteRepository;
import com.fisiogestao.pacientes.repository.PacoteSessaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Portal público de autoatendimento do paciente.
 * Acessível sem login via token único por paciente.
 */
@Controller
@RequestMapping("/paciente")
@RequiredArgsConstructor
public class PortalPacienteController {

    private static final DateTimeFormatter GCAL_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private final ClienteRepository clienteRepository;
    private final SessaoRepository sessaoRepository;
    private final PacoteSessaoRepository pacoteSessaoRepository;
    private final ConfiguracaoClinicaRepository configuracaoClinicaRepository;

    @GetMapping("/{token}")
    public String portal(@PathVariable UUID token, Model model) {
        ClienteEntity paciente = clienteRepository.findByPortalToken(token).orElse(null);

        if (paciente == null) {
            return "portal/token-invalido";
        }

        ConfiguracaoClinicaEntity clinica = configuracaoClinicaRepository
                .findFirstByAtivoTrueOrderByCreatedAtAsc()
                .orElse(null);

        String nomeClinica = clinica != null ? clinica.getNomeClinica() : "Fisioterapia";

        // Próximas sessões (AGENDADA, a partir de agora) → envoltas no view-record
        List<SessaoEntity> proximasRaw = sessaoRepository
                .findByClienteIdAndStatusAndDataHoraInicioAfterOrderByDataHoraInicioAsc(
                        paciente.getId(), SessaoStatus.AGENDADA, ClinicTimezoneHolder.now());

        List<SessaoPortalView> proximasSessoes = proximasRaw.stream()
                .map(s -> new SessaoPortalView(s, buildCalendarUrl(s, nomeClinica)))
                .toList();

        // Histórico recente (últimas 10 sessões realizadas ou canceladas)
        List<SessaoEntity> historicoSessoes = sessaoRepository
                .findTop10ByClienteIdAndStatusNotOrderByDataHoraInicioDesc(
                        paciente.getId(), SessaoStatus.AGENDADA);

        // Pacotes: separados por status para o template
        List<PacoteSessaoEntity> pacotes = pacoteSessaoRepository
                .findByClienteIdOrderByCreatedAtDesc(paciente.getId());

        List<PacoteSessaoEntity> pacotesAtivos = pacotes.stream()
                .filter(p -> p.getStatus() == PacoteSessaoStatus.ATIVO)
                .toList();

        List<PacoteSessaoEntity> pacotesEncerrados = pacotes.stream()
                .filter(p -> p.getStatus() != PacoteSessaoStatus.ATIVO)
                .toList();

        // WhatsApp link: remove tudo que não seja dígito para montar a URL wa.me/
        String whatsappUrl = null;
        if (clinica != null && clinica.getTelefoneWhatsapp() != null
                && !clinica.getTelefoneWhatsapp().isBlank()) {
            String soDigitos = clinica.getTelefoneWhatsapp().replaceAll("[^0-9]", "");
            whatsappUrl = "https://wa.me/" + soDigitos;
        }

        model.addAttribute("paciente", paciente);
        model.addAttribute("token", token);
        model.addAttribute("clinica", clinica);
        model.addAttribute("whatsappUrl", whatsappUrl);
        model.addAttribute("proximasSessoes", proximasSessoes);
        model.addAttribute("historicoSessoes", historicoSessoes);
        model.addAttribute("pacotesAtivos", pacotesAtivos);
        model.addAttribute("pacotesEncerrados", pacotesEncerrados);

        return "portal/index";
    }

    /**
     * Confirmação de presença: paciente confirma que comparecerá à sessão.
     * Apenas muda um flag visual — não altera status real da sessão.
     */
    @PostMapping("/{token}/sessao/{sessaoId}/confirmar")
    public String confirmarPresenca(
            @PathVariable UUID token,
            @PathVariable UUID sessaoId,
            RedirectAttributes redirectAttributes) {

        ClienteEntity paciente = clienteRepository.findByPortalToken(token).orElse(null);
        if (paciente == null) {
            return "redirect:/paciente/" + token;
        }

        sessaoRepository.findByIdAndClienteIdAndStatus(sessaoId, paciente.getId(), SessaoStatus.AGENDADA)
                .ifPresent(sessao -> {
                    sessao.setConfirmadoPeloPaciente(true);
                    sessaoRepository.save(sessao);
                });

        redirectAttributes.addFlashAttribute("successMessage",
                "Presença confirmada! Te esperamos na sessão.");
        return "redirect:/paciente/" + token;
    }

    /**
     * Constrói uma URL do Google Calendar para adicionar a sessão agendada.
     * Datas convertidas para UTC no formato exigido pela API do Google Calendar.
     */
    private String buildCalendarUrl(SessaoEntity sessao, String nomeClinica) {
        String inicio = sessao.getDataHoraInicio().toInstant().atOffset(ZoneOffset.UTC).format(GCAL_FMT);
        String fim = sessao.getDataHoraFim().toInstant().atOffset(ZoneOffset.UTC).format(GCAL_FMT);
        String titulo = URLEncoder.encode("Fisioterapia – " + nomeClinica, StandardCharsets.UTF_8);
        return "https://calendar.google.com/calendar/render?action=TEMPLATE"
                + "&text=" + titulo
                + "&dates=" + inicio + "/" + fim;
    }
}
