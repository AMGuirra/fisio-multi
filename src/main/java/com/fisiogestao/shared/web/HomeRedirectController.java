package com.fisiogestao.shared.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeRedirectController {

    @GetMapping({"/", "/home"})
    public String home() {
        return "redirect:/login";
    }
}
