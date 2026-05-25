package com.fisiogestao.listaespera;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/lista-espera")
public class ListaEsperaController {

    @GetMapping
    public String redirect() {
        return "redirect:/solicitacoes#lista-espera";
    }
}
