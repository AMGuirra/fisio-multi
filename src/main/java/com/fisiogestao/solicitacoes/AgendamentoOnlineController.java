package com.fisiogestao.solicitacoes;

import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.solicitacoes.form.AgendamentoOnlineForm;
import com.fisiogestao.solicitacoes.service.SolicitacaoAgendamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/agendamento-online")
@RequiredArgsConstructor
public class AgendamentoOnlineController {

    private final AgendaService agendaService;
    private final SolicitacaoAgendamentoService solicitacaoAgendamentoService;

    @GetMapping
    public String formulario(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            Model model
    ) {
        AgendamentoOnlineForm form = new AgendamentoOnlineForm();
        form.setData(resolverDataInicial(data));
        prepararPagina(model, form);
        return "publico/agendamento-online";
    }

    @PostMapping
    public String solicitar(
            @Valid @ModelAttribute("form") AgendamentoOnlineForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararPagina(model, form);
            return "publico/agendamento-online";
        }

        try {
            solicitacaoAgendamentoService.criarSolicitacaoPublica(form);
            redirectAttributes.addFlashAttribute("successMessage", "Sua solicitação foi registrada com sucesso.");
            return "redirect:/agendamento-online/sucesso";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, form);
            return "publico/agendamento-online";
        }
    }

    @GetMapping("/sucesso")
    public String sucesso() {
        return "publico/agendamento-online-sucesso";
    }

    private void prepararPagina(Model model, AgendamentoOnlineForm form) {
        model.addAttribute("form", form);
        model.addAttribute("configuracaoClinica", agendaService.obterConfiguracaoAtiva());
        model.addAttribute("slots", form.getData() == null ? java.util.List.of() : agendaService.gerarSlotsDisponiveis(form.getData()));
    }

    private LocalDate resolverDataInicial(LocalDate dataSolicitada) {
        if (dataSolicitada != null) {
            return dataSolicitada;
        }

        LocalDate hoje = LocalDate.now();
        for (int dias = 0; dias < 30; dias++) {
            LocalDate data = hoje.plusDays(dias);
            if (!agendaService.gerarSlotsDisponiveis(data).isEmpty()) {
                return data;
            }
        }
        return hoje;
    }
}
