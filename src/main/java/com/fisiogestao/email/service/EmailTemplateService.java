package com.fisiogestao.email.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;

@Service
public class EmailTemplateService {

    private final TemplateEngine templateEngine;

    public EmailTemplateService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String render(String templateName, Map<String, Object> variables) {
        Context context = new Context(Locale.forLanguageTag("pt-BR"));
        if (variables != null) {
            context.setVariables(variables);
        }
        return templateEngine.process(templateName, context);
    }
}
