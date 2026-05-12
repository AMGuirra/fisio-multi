package com.fisiogestao.agenda;

import com.fisiogestao.agenda.form.BloqueioAgendaCadastroForm;
import com.fisiogestao.agenda.form.BloqueioAgendaFiltroForm;
import com.fisiogestao.agenda.form.ConfiguracaoClinicaForm;
import com.fisiogestao.agenda.service.BloqueioAgendaService;
import com.fisiogestao.agenda.service.ConfiguracaoClinicaService;
import com.fisiogestao.shared.web.BusinessException;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping({"/configuracoes/clinica", "/configuracao/clinica"})
@RequiredArgsConstructor
public class ConfiguracaoClinicaController {

    private final ConfiguracaoClinicaService configuracaoClinicaService;
    private final BloqueioAgendaService bloqueioAgendaService;

    @GetMapping
    public String index(
            @RequestParam(name = "editar", required = false) UUID editarId,
            @RequestParam(name = "data", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataSugerida,
            @ModelAttribute("filtroForm") BloqueioAgendaFiltroForm filtroForm,
            Model model
    ) {
        prepararPagina(model, configuracaoClinicaService.carregarFormulario(), filtroPadrao(filtroForm), editarId, dataSugerida);
        return "configuracoes/clinica";
    }

    @PostMapping
    public String salvar(
            @Valid @ModelAttribute("configuracaoClinicaForm") ConfiguracaoClinicaForm form,
            BindingResult bindingResult,
            @ModelAttribute("filtroForm") BloqueioAgendaFiltroForm filtroForm,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararPagina(model, form, filtroPadrao(filtroForm), null, null);
            return "configuracoes/clinica";
        }

        try {
            configuracaoClinicaService.salvar(form);
            redirectAttributes.addFlashAttribute("successMessage", "Configuração da clínica atualizada com sucesso.");
            return "redirect:/configuracoes/clinica";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPagina(model, form, filtroPadrao(filtroForm), null, null);
            return "configuracoes/clinica";
        }
    }

    private void prepararPagina(Model model, ConfiguracaoClinicaForm form, BloqueioAgendaFiltroForm filtroForm, UUID editarId, LocalDate dataSugerida) {
        model.addAttribute("configuracaoClinicaForm", form);
        model.addAttribute("diasSemana", List.of(
                new DiaSemanaOption(DayOfWeek.MONDAY, "Segunda-feira"),
                new DiaSemanaOption(DayOfWeek.TUESDAY, "Terça-feira"),
                new DiaSemanaOption(DayOfWeek.WEDNESDAY, "Quarta-feira"),
                new DiaSemanaOption(DayOfWeek.THURSDAY, "Quinta-feira"),
                new DiaSemanaOption(DayOfWeek.FRIDAY, "Sexta-feira"),
                new DiaSemanaOption(DayOfWeek.SATURDAY, "Sábado"),
                new DiaSemanaOption(DayOfWeek.SUNDAY, "Domingo")
        ));
        model.addAttribute("timezonesClinica", configuracaoClinicaService.listarTimezonesSuportados());

        prepararBloqueios(model, filtroForm, editarId, dataSugerida);
    }

    private void prepararBloqueios(Model model, BloqueioAgendaFiltroForm filtroForm, UUID editarId, LocalDate dataSugerida) {
        BloqueioAgendaCadastroForm bloqueioForm;
        if (editarId != null) {
            var edicao = bloqueioAgendaService.carregarParaEdicao(editarId);
            bloqueioForm = edicao.form();
            model.addAttribute("editMode", true);
            model.addAttribute("editingId", editarId);
            model.addAttribute("bloqueiosRelacionados", edicao.grupo());
        } else {
            bloqueioForm = formularioPadrao(dataSugerida);
            model.addAttribute("editMode", false);
            model.addAttribute("bloqueiosRelacionados", java.util.List.of());
        }

        if (!model.containsAttribute("bloqueioForm")) {
            model.addAttribute("bloqueioForm", bloqueioForm);
        }
        model.addAttribute("filtroForm", filtroForm);
        model.addAttribute("bloqueios", bloqueioAgendaService.listarPorFiltro(filtroForm));
        model.addAttribute("resumoBloqueios", bloqueioAgendaService.resumo(filtroForm));
    }

    private BloqueioAgendaFiltroForm filtroPadrao(BloqueioAgendaFiltroForm filtroForm) {
        if (filtroForm == null) {
            filtroForm = new BloqueioAgendaFiltroForm();
        }
        if (filtroForm.getDataInicial() == null) {
            filtroForm.setDataInicial(LocalDate.now().minusDays(7));
        }
        if (filtroForm.getDataFinal() == null) {
            filtroForm.setDataFinal(LocalDate.now().plusDays(45));
        }
        return filtroForm;
    }

    private BloqueioAgendaCadastroForm formularioPadrao(LocalDate dataSugerida) {
        LocalDate base = dataSugerida != null ? dataSugerida : LocalDate.now();
        BloqueioAgendaCadastroForm form = new BloqueioAgendaCadastroForm();
        form.setDataInicial(base);
        form.setDataFinal(base);
        form.setBloqueioDiaInteiro(true);
        return form;
    }

    public record DiaSemanaOption(DayOfWeek value, String label) {
    }
}
