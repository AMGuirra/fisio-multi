package com.fisiogestao.shared.web;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.Year;

@ControllerAdvice
public class ControllerAdviceConfig {

    @ModelAttribute
    public void configureGlobalAttributes(Model model) {
        model.addAttribute("appName", "FisioGestão");
        model.addAttribute("currentYear", Year.now().getValue());
    }
}
