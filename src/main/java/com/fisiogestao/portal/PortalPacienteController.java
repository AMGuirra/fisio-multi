package com.fisiogestao.portal;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.config.ClinicTimezoneHolder;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.PacoteSessaoEntity;
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

    private final ClienteRepository clienteRepository;
    private final SessaoRepository sessaoRepository;
    private final PacoteSessaoRepository pacoteSessaoRepository;

    @GetMapping("/{token}")
    public String portal(@PathVariable UUID token, Model model) {
        ClienteEntity paciente = clienteRepository.findByPortalToken(token).orElse(null);

        if (paciente == null) {
            return "portal/token-invalido";
        }

        // Próximas sessões (AGENDADA, a partir de agora)
        List<SessaoEntity> proximasSessoes = sessaoRepository
                .findByClienteIdAndStatusAndDataHoraInicioAfterOrderByDataHoraInicioAsc(
                        paciente.getId(), SessaoStatus.AGENDADA, ClinicTimezoneHolder.now());

        // Histórico recente (últimas 10 sessões realizadas ou canceladas)
        List<SessaoEntity> historicoSessoes = sessaoRepository
                .findTop10ByClienteIdAndStatusNotOrderByDataHoraInicioDesc(
                        paciente.getId(), SessaoStatus.AGENDADA);

        // Pacotes ativos
        List<PacoteSessaoEntity> pacotes = pacoteSessaoRepository
                .findByClienteIdOrderByCreatedAtDesc(paciente.getId());

        model.addAttribute("paciente", paciente);
        model.addAttribute("token", token);
        model.addAttribute("proximasSessoes", proximasSessoes);
        model.addAttribute("historicoSessoes", historicoSessoes);
        model.addAttribute("pacotes", pacotes);

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

        // Busca a sessão verificando que pertence ao paciente e está AGENDADA
        sessaoRepository.findByIdAndClienteIdAndStatus(sessaoId, paciente.getId(), SessaoStatus.AGENDADA)
                .ifPresent(sessao -> {
                    sessao.setConfirmadoPeloPaciente(true);
                    sessaoRepository.save(sessao);
                });

        redirectAttributes.addFlashAttribute("successMessage",
                "Presença confirmada! Te esperamos na sessão.");
        return "redirect:/paciente/" + token;
    }
}
