package com.fisiogestao.whatsapp;

import com.fisiogestao.whatsapp.gateway.WhatsAppGateway;
import com.fisiogestao.whatsapp.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {

    private final WhatsAppService whatsAppService;
    private final WhatsAppGateway whatsAppGateway;

    @GetMapping
    public String index(@RequestParam(required = false) String status, Model model) {
        StatusMensagemWhatsApp filtroStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                filtroStatus = StatusMensagemWhatsApp.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // filtro inválido — ignora
            }
        }

        model.addAttribute("mensagens", whatsAppService.listarFila(filtroStatus));
        model.addAttribute("totalPendentes", whatsAppService.totalPorStatus(StatusMensagemWhatsApp.PENDENTE));
        model.addAttribute("totalEnviados", whatsAppService.totalPorStatus(StatusMensagemWhatsApp.ENVIADO));
        model.addAttribute("totalErros", whatsAppService.totalPorStatus(StatusMensagemWhatsApp.ERRO));
        model.addAttribute("gatewayOperacional", whatsAppGateway.isOperacional());
        model.addAttribute("filtroStatus", filtroStatus);
        return "whatsapp/index";
    }

    @PostMapping("/{id}/reenviar")
    public String reenviar(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        whatsAppService.recolocarNaFila(id);
        redirectAttributes.addFlashAttribute("successMessage", "Mensagem colocada novamente na fila.");
        return "redirect:/whatsapp";
    }
}
