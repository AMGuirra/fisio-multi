package com.fisiogestao.agenda;

import com.fisiogestao.agenda.form.BloqueioAgendaCadastroForm;
import com.fisiogestao.agenda.form.BloqueioAgendaFiltroForm;
import com.fisiogestao.agenda.service.BloqueioAgendaService;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/bloqueios")
@RequiredArgsConstructor
public class BloqueiosController {

    private final BloqueioAgendaService bloqueioAgendaService;

    @GetMapping
    public String index(
            @RequestParam(name = "editar", required = false) UUID editarId,
            @RequestParam(name = "data", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataSugerida,
            @ModelAttribute("filtroForm") BloqueioAgendaFiltroForm filtroForm,
            Model model
    ) {
        prepararPagina(model, filtroPadrao(filtroForm), editarId, dataSugerida);
        return "bloqueios/index";
    }

    @PostMapping
    public String criar(
            @Valid @ModelAttribute("bloqueioForm") BloqueioAgendaCadastroForm form,
            BindingResult bindingResult,
            @ModelAttribute("filtroForm") BloqueioAgendaFiltroForm filtroForm,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararPaginaComForm(model, filtroPadrao(filtroForm), form, null);
            return "bloqueios/index";
        }
        try {
            bloqueioAgendaService.criar(form, usuario(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Bloqueio cadastrado com sucesso.");
            return "redirect:/bloqueios";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPaginaComForm(model, filtroPadrao(filtroForm), form, null);
            return "bloqueios/index";
        }
    }

    @PostMapping("/{id}/editar")
    public String editar(
            @PathVariable UUID id,
            @Valid @ModelAttribute("bloqueioForm") BloqueioAgendaCadastroForm form,
            BindingResult bindingResult,
            @ModelAttribute("filtroForm") BloqueioAgendaFiltroForm filtroForm,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararPaginaComForm(model, filtroPadrao(filtroForm), form, id);
            return "bloqueios/index";
        }
        try {
            bloqueioAgendaService.atualizar(id, form, usuario(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Bloqueio atualizado com sucesso.");
            return "redirect:/bloqueios";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepararPaginaComForm(model, filtroPadrao(filtroForm), form, id);
            return "bloqueios/index";
        }
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(
            @PathVariable UUID id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            bloqueioAgendaService.cancelar(id, usuario(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Bloqueio cancelado com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/bloqueios";
    }

    private void prepararPagina(Model model, BloqueioAgendaFiltroForm filtroForm, UUID editarId, LocalDate dataSugerida) {
        BloqueioAgendaCadastroForm form = formularioPadrao(dataSugerida);
        if (editarId != null) {
            var edicao = bloqueioAgendaService.carregarParaEdicao(editarId);
            form = edicao.form();
            model.addAttribute("editMode", true);
            model.addAttribute("editingId", editarId);
            model.addAttribute("bloqueiosRelacionados", edicao.grupo());
        } else {
            model.addAttribute("editMode", false);
            model.addAttribute("bloqueiosRelacionados", java.util.List.of());
        }
        prepararPaginaComForm(model, filtroForm, form, editarId);
    }

    private void prepararPaginaComForm(Model model, BloqueioAgendaFiltroForm filtroForm, BloqueioAgendaCadastroForm form, UUID editarId) {
        model.addAttribute("filtroForm", filtroForm);
        model.addAttribute("bloqueioForm", form);
        model.addAttribute("bloqueios", bloqueioAgendaService.listarPorFiltro(filtroForm));
        model.addAttribute("resumoBloqueios", bloqueioAgendaService.resumo(filtroForm));
        if (!model.containsAttribute("editMode")) {
            model.addAttribute("editMode", editarId != null);
        }
        if (editarId != null) {
            model.addAttribute("editingId", editarId);
            if (!model.containsAttribute("bloqueiosRelacionados")) {
                model.addAttribute("bloqueiosRelacionados", bloqueioAgendaService.carregarParaEdicao(editarId).grupo());
            }
        } else if (!model.containsAttribute("bloqueiosRelacionados")) {
            model.addAttribute("bloqueiosRelacionados", java.util.List.of());
        }
    }

    private BloqueioAgendaFiltroForm filtroPadrao(BloqueioAgendaFiltroForm filtroForm) {
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

    private String usuario(Authentication authentication) {
        return authentication != null ? authentication.getName() : "sistema";
    }
}
