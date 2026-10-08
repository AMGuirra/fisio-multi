package com.fisiogestao.suporte;

import com.fisiogestao.cancelamento.CancelamentoService;
import com.fisiogestao.cancelamento.MotivoCancelamento;
import com.fisiogestao.empresa.MotivoAcessoNegado;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Páginas públicas de acesso negado, suporte e cancelamento — acessíveis mesmo quando a
 * clínica não consegue entrar no sistema. Nenhuma delas revela dados de empresas.
 */
@Controller
@RequiredArgsConstructor
public class SuportePublicoController {

    private final SuporteService suporteService;
    private final CancelamentoService cancelamentoService;

    @GetMapping("/acesso-negado")
    public String acessoNegado(@RequestParam(required = false) String motivo, Model model) {
        model.addAttribute("motivo", MotivoAcessoNegado.fromParam(motivo));
        return "publico/acesso-negado";
    }

    @GetMapping("/clinica-indisponivel")
    public String clinicaIndisponivel(@RequestParam(required = false) String motivo, Model model) {
        model.addAttribute("naoEncontrada", "nao-encontrada".equals(motivo));
        return "publico/clinica-indisponivel";
    }

    @GetMapping("/suporte/solicitar")
    public String formulario(Model model) {
        model.addAttribute("limiteDescricao", SolicitacaoSuporteEntity.LIMITE_DESCRICAO);
        return "publico/suporte-solicitar";
    }

    @PostMapping("/suporte/solicitar")
    public String solicitar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String cnpj,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String descricao,
            HttpServletRequest request,
            Model model
    ) {
        try {
            suporteService.registrarPublica(nome, cpf, cnpj, email, descricao, request.getRemoteAddr());
            return "redirect:/suporte/solicitar/sucesso";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("nome", nome);
            model.addAttribute("cpf", cpf);
            model.addAttribute("cnpj", cnpj);
            model.addAttribute("email", email);
            model.addAttribute("descricao", descricao);
            model.addAttribute("limiteDescricao", SolicitacaoSuporteEntity.LIMITE_DESCRICAO);
            return "publico/suporte-solicitar";
        }
    }

    @GetMapping("/suporte/solicitar/sucesso")
    public String sucesso(Model model) {
        model.addAttribute("tituloSucesso", "Solicitação enviada");
        model.addAttribute("textoSucesso",
                "Recebemos sua solicitação de suporte. Nossa equipe responderá pelo e-mail informado.");
        return "publico/suporte-sucesso";
    }

    @GetMapping("/suporte/cancelamento")
    public String formularioCancelamento(Model model) {
        model.addAttribute("motivos", MotivoCancelamento.values());
        model.addAttribute("textoConfirmacao", CancelamentoService.TEXTO_CONFIRMACAO);
        return "publico/suporte-cancelamento";
    }

    @PostMapping("/suporte/cancelamento")
    public String solicitarCancelamento(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String cnpj,
            @RequestParam(required = false) MotivoCancelamento motivo,
            @RequestParam(required = false) String descricao,
            @RequestParam(defaultValue = "false") boolean confirmacao,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            cancelamentoService.solicitarPublicamente(nome, email, cpf, cnpj, motivo, descricao, confirmacao,
                    request.getRemoteAddr());
            return "redirect:/suporte/cancelamento/sucesso";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("nome", nome);
            model.addAttribute("email", email);
            model.addAttribute("cpf", cpf);
            model.addAttribute("cnpj", cnpj);
            model.addAttribute("motivoSelecionado", motivo);
            model.addAttribute("descricao", descricao);
            return formularioCancelamento(model);
        }
    }

    @GetMapping("/suporte/cancelamento/sucesso")
    public String cancelamentoSucesso(Model model) {
        model.addAttribute("tituloSucesso", "Solicitação de cancelamento recebida");
        model.addAttribute("textoSucesso",
                "Sua solicitação será analisada pela nossa equipe. Nenhum dado foi alterado ou removido; "
                        + "responderemos pelo e-mail informado.");
        return "publico/suporte-sucesso";
    }
}
