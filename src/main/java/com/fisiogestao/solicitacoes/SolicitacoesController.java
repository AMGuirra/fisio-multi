package com.fisiogestao.solicitacoes;

import com.fisiogestao.solicitacoes.form.AprovacaoSolicitacaoForm;
import com.fisiogestao.solicitacoes.service.SolicitacaoAgendamentoService;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/solicitacoes")
@Slf4j
@RequiredArgsConstructor
public class SolicitacoesController {

    private final SolicitacaoAgendamentoService solicitacaoAgendamentoService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("pendentes", solicitacaoAgendamentoService.listarPendentes());
        model.addAttribute("historico", solicitacaoAgendamentoService.listarTodas());
        model.addAttribute("aprovacaoForm", new AprovacaoSolicitacaoForm());
        return "solicitacoes/index";
    }

    @PostMapping("/{id}/aprovar")
    public String aprovar(
            @PathVariable UUID id,
            @ModelAttribute("aprovacaoForm") AprovacaoSolicitacaoForm form,
            RedirectAttributes redirectAttributes
    ) {
        try {
            solicitacaoAgendamentoService.aprovarSolicitacao(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Solicitação aprovada com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Erro inesperado ao aprovar solicitacao {}.", id, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Não foi possível aprovar a solicitação. Verifique a configuração do banco de dados.");
        }
        return "redirect:/solicitacoes";
    }

    @PostMapping("/{id}/cadastrar")
    public String cadastrar(
            @PathVariable UUID id,
            @ModelAttribute("aprovacaoForm") AprovacaoSolicitacaoForm form,
            RedirectAttributes redirectAttributes
    ) {
        try {
            solicitacaoAgendamentoService.cadastrarPaciente(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Paciente cadastrado com sucesso. Agora você já pode aprovar a solicitação.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Erro inesperado ao cadastrar paciente da solicitacao {}. Tipo: {}. Mensagem: {}", id, ex.getClass().getSimpleName(), ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Não foi possível cadastrar o paciente da solicitação: " + ex.getClass().getSimpleName() + " — " + ex.getMessage());
        }
        return "redirect:/solicitacoes";
    }

    @PostMapping("/{id}/rejeitar")
    public String rejeitar(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            solicitacaoAgendamentoService.rejeitarSolicitacao(id);
            redirectAttributes.addFlashAttribute("successMessage", "Solicitação rejeitada e horário liberado.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Erro inesperado ao rejeitar solicitacao {}.", id, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Não foi possível rejeitar a solicitação.");
        }
        return "redirect:/solicitacoes";
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            solicitacaoAgendamentoService.cancelarSolicitacaoAprovada(id);
            redirectAttributes.addFlashAttribute("successMessage", "Agendamento cancelado com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Erro inesperado ao cancelar solicitacao {}.", id, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Não foi possível cancelar a solicitação.");
        }
        return "redirect:/solicitacoes";
    }

    @PostMapping("/{id}/restaurar")
    public String restaurar(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            solicitacaoAgendamentoService.restaurarSolicitacao(id);
            redirectAttributes.addFlashAttribute("successMessage", "Solicitação restaurada para pendente.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Erro inesperado ao restaurar solicitacao {}.", id, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Não foi possível restaurar a solicitação.");
        }
        return "redirect:/solicitacoes";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            solicitacaoAgendamentoService.excluirSolicitacao(id);
            redirectAttributes.addFlashAttribute("successMessage", "Solicitação excluída permanentemente.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Erro inesperado ao excluir solicitacao {}.", id, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Não foi possível excluir a solicitação.");
        }
        return "redirect:/solicitacoes";
    }
}
