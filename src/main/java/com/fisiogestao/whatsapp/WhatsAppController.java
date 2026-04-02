package com.fisiogestao.whatsapp;

import com.fisiogestao.whatsapp.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    @GetMapping
    public String index(
            @RequestParam(name = "status", required = false) StatusMensagemWhatsApp status,
            Model model
    ) {
        model.addAttribute("statusSelecionado", status);
        model.addAttribute("statusMensagens", StatusMensagemWhatsApp.values());
        model.addAttribute("mensagensWhatsApp", whatsAppService.listarFila(status));
        model.addAttribute("totalMensagens", whatsAppService.totalPorStatus(null));
        model.addAttribute("totalPendentes", whatsAppService.totalPorStatus(StatusMensagemWhatsApp.PENDENTE));
        model.addAttribute("totalEnviadas", whatsAppService.totalPorStatus(StatusMensagemWhatsApp.ENVIADO));
        model.addAttribute("totalErros", whatsAppService.totalPorStatus(StatusMensagemWhatsApp.ERRO));
        return "whatsapp/index";
    }
}
