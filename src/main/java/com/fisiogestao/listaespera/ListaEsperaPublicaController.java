package com.fisiogestao.listaespera;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ListaEsperaPublicaController {

    @GetMapping({"/lista-espera-publica", "/lista-espera-publica/sucesso"})
    public String redirectGet() {
        return "redirect:/agendamento-online" + com.fisiogestao.tenant.PublicTenantFilter.parametroClinica();
    }

    @PostMapping("/lista-espera-publica")
    public String redirectPost() {
        return "redirect:/agendamento-online" + com.fisiogestao.tenant.PublicTenantFilter.parametroClinica();
    }
}
