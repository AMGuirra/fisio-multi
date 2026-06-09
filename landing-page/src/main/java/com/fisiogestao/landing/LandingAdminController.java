package com.fisiogestao.landing;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin/landing")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class LandingAdminController {

    private final LandingContentService svc;

    // ── Overview ─────────────────────────────────────────────────────────────

    @GetMapping
    public String index(Model model) {
        model.addAttribute("secoes",       svc.buildSecoesFromDb());
        model.addAttribute("totalDepos",   svc.todosTestemunhos().size());
        model.addAttribute("totalPlanos",  svc.todosPlanos().size());
        model.addAttribute("totalFaqs",    svc.todosFaqs().size());
        model.addAttribute("totalBeneficios", svc.todosBeneficios().size());
        model.addAttribute("totalSteps",   svc.todosSteps().size());
        return "landing-admin/index";
    }

    // ── Hero ─────────────────────────────────────────────────────────────────

    @GetMapping("/hero")
    public String heroForm(Model model) {
        model.addAttribute("cfg", svc.loadConfigMap());
        return "landing-admin/hero";
    }

    @PostMapping("/hero")
    public String heroSave(@RequestParam Map<String, String> params,
                           RedirectAttributes ra) {
        // Mantém apenas chaves do hero e das seções
        Map<String, String> heroCfg = new java.util.LinkedHashMap<>();
        params.forEach((k, v) -> {
            if (k.startsWith("hero.") || k.startsWith("secao.")) heroCfg.put(k, v);
        });
        svc.saveConfigs(heroCfg);
        ra.addFlashAttribute("successMessage", "Hero atualizado com sucesso.");
        return "redirect:/admin/landing/hero";
    }

    // ── Visibilidade de seções ────────────────────────────────────────────────

    @GetMapping("/secoes")
    public String secoesForm(Model model) {
        model.addAttribute("cfg", svc.loadConfigMap());
        return "landing-admin/secoes";
    }

    @PostMapping("/secoes")
    public String secoesSave(@RequestParam Map<String, String> params,
                             RedirectAttributes ra) {
        // Checkboxes não enviados = false; enviados = "true"
        String[] chaveSecoes = {
            "secao.como_funciona","secao.funcionalidades","secao.beneficios",
            "secao.depoimentos","secao.planos","secao.faq"
        };
        Map<String, String> updates = new java.util.LinkedHashMap<>();
        for (String ch : chaveSecoes) {
            updates.put(ch, params.getOrDefault(ch, "false"));
        }
        svc.saveConfigs(updates);
        ra.addFlashAttribute("successMessage", "Visibilidade das seções salva.");
        return "redirect:/admin/landing/secoes";
    }

    // ── Depoimentos ──────────────────────────────────────────────────────────

    @GetMapping("/testemunhos")
    public String testemunhos(Model model) {
        model.addAttribute("lista", svc.todosTestemunhos());
        model.addAttribute("novo", new LpTestemunhoEntity());
        return "landing-admin/testemunhos";
    }

    @PostMapping("/testemunhos/novo")
    public String testemunhoNovo(@RequestParam String nome,
                                 @RequestParam String cargo,
                                 @RequestParam String depoimento,
                                 @RequestParam String iniciais,
                                 @RequestParam int corAvatar,
                                 @RequestParam int estrelas,
                                 @RequestParam int ordem,
                                 RedirectAttributes ra) {
        svc.salvarTestemunho(LpTestemunhoEntity.builder()
            .nome(nome).cargo(cargo).depoimento(depoimento)
            .iniciais(iniciais).corAvatar(corAvatar).estrelas(estrelas)
            .ordem(ordem).ativo(true).build());
        ra.addFlashAttribute("successMessage", "Depoimento adicionado.");
        return "redirect:/admin/landing/testemunhos";
    }

    @PostMapping("/testemunhos/{id}/editar")
    public String testemunhoEditar(@PathVariable UUID id,
                                   @RequestParam String nome,
                                   @RequestParam String cargo,
                                   @RequestParam String depoimento,
                                   @RequestParam String iniciais,
                                   @RequestParam int corAvatar,
                                   @RequestParam int estrelas,
                                   @RequestParam int ordem,
                                   RedirectAttributes ra) {
        LpTestemunhoEntity e = svc.buscarTestemunho(id);
        e.setNome(nome); e.setCargo(cargo); e.setDepoimento(depoimento);
        e.setIniciais(iniciais); e.setCorAvatar(corAvatar); e.setEstrelas(estrelas);
        e.setOrdem(ordem);
        svc.salvarTestemunho(e);
        ra.addFlashAttribute("successMessage", "Depoimento atualizado.");
        return "redirect:/admin/landing/testemunhos";
    }

    @PostMapping("/testemunhos/{id}/alternar")
    public String testemunhoAlternar(@PathVariable UUID id, RedirectAttributes ra) {
        svc.alternarTestemunho(id);
        ra.addFlashAttribute("successMessage", "Status do depoimento alterado.");
        return "redirect:/admin/landing/testemunhos";
    }

    @PostMapping("/testemunhos/{id}/excluir")
    public String testemunhoExcluir(@PathVariable UUID id, RedirectAttributes ra) {
        svc.excluirTestemunho(id);
        ra.addFlashAttribute("successMessage", "Depoimento removido.");
        return "redirect:/admin/landing/testemunhos";
    }

    // ── Planos ───────────────────────────────────────────────────────────────

    @GetMapping("/planos")
    public String planos(Model model) {
        model.addAttribute("lista", svc.todosPlanos());
        return "landing-admin/planos";
    }

    @PostMapping("/planos/novo")
    public String planoNovo(@RequestParam String nome,
                            @RequestParam(required = false) String descricao,
                            @RequestParam BigDecimal preco,
                            @RequestParam String periodo,
                            @RequestParam(required = false) String recursos,
                            @RequestParam(defaultValue = "false") boolean destaque,
                            @RequestParam(required = false) String badgeTexto,
                            @RequestParam String ctaTexto,
                            @RequestParam String ctaLink,
                            @RequestParam int ordem,
                            RedirectAttributes ra) {
        svc.salvarPlano(LpPlanoEntity.builder()
            .nome(nome).descricao(descricao).preco(preco).periodo(periodo)
            .recursos(recursos).destaque(destaque).badgeTexto(badgeTexto)
            .ctaTexto(ctaTexto).ctaLink(ctaLink).ordem(ordem).ativo(true).build());
        ra.addFlashAttribute("successMessage", "Plano adicionado.");
        return "redirect:/admin/landing/planos";
    }

    @PostMapping("/planos/{id}/editar")
    public String planoEditar(@PathVariable UUID id,
                              @RequestParam String nome,
                              @RequestParam(required = false) String descricao,
                              @RequestParam BigDecimal preco,
                              @RequestParam String periodo,
                              @RequestParam(required = false) String recursos,
                              @RequestParam(defaultValue = "false") boolean destaque,
                              @RequestParam(required = false) String badgeTexto,
                              @RequestParam String ctaTexto,
                              @RequestParam String ctaLink,
                              @RequestParam int ordem,
                              RedirectAttributes ra) {
        LpPlanoEntity e = svc.buscarPlano(id);
        e.setNome(nome); e.setDescricao(descricao); e.setPreco(preco);
        e.setPeriodo(periodo); e.setRecursos(recursos); e.setDestaque(destaque);
        e.setBadgeTexto(badgeTexto); e.setCtaTexto(ctaTexto); e.setCtaLink(ctaLink);
        e.setOrdem(ordem);
        svc.salvarPlano(e);
        ra.addFlashAttribute("successMessage", "Plano atualizado.");
        return "redirect:/admin/landing/planos";
    }

    @PostMapping("/planos/{id}/alternar")
    public String planoAlternar(@PathVariable UUID id, RedirectAttributes ra) {
        svc.alternarPlano(id);
        return "redirect:/admin/landing/planos";
    }

    @PostMapping("/planos/{id}/excluir")
    public String planoExcluir(@PathVariable UUID id, RedirectAttributes ra) {
        svc.excluirPlano(id);
        ra.addFlashAttribute("successMessage", "Plano removido.");
        return "redirect:/admin/landing/planos";
    }

    // ── FAQ ──────────────────────────────────────────────────────────────────

    @GetMapping("/faq")
    public String faq(Model model) {
        model.addAttribute("lista", svc.todosFaqs());
        return "landing-admin/faq";
    }

    @PostMapping("/faq/novo")
    public String faqNovo(@RequestParam String pergunta,
                          @RequestParam String resposta,
                          @RequestParam int ordem,
                          RedirectAttributes ra) {
        svc.salvarFaq(LpFaqEntity.builder()
            .pergunta(pergunta).resposta(resposta).ordem(ordem).ativo(true).build());
        ra.addFlashAttribute("successMessage", "Pergunta adicionada.");
        return "redirect:/admin/landing/faq";
    }

    @PostMapping("/faq/{id}/editar")
    public String faqEditar(@PathVariable UUID id,
                            @RequestParam String pergunta,
                            @RequestParam String resposta,
                            @RequestParam int ordem,
                            RedirectAttributes ra) {
        LpFaqEntity e = svc.buscarFaq(id);
        e.setPergunta(pergunta); e.setResposta(resposta); e.setOrdem(ordem);
        svc.salvarFaq(e);
        ra.addFlashAttribute("successMessage", "Pergunta atualizada.");
        return "redirect:/admin/landing/faq";
    }

    @PostMapping("/faq/{id}/alternar")
    public String faqAlternar(@PathVariable UUID id, RedirectAttributes ra) {
        svc.alternarFaq(id);
        return "redirect:/admin/landing/faq";
    }

    @PostMapping("/faq/{id}/excluir")
    public String faqExcluir(@PathVariable UUID id, RedirectAttributes ra) {
        svc.excluirFaq(id);
        ra.addFlashAttribute("successMessage", "Pergunta removida.");
        return "redirect:/admin/landing/faq";
    }

    // ── Benefícios ───────────────────────────────────────────────────────────

    @GetMapping("/beneficios")
    public String beneficios(Model model) {
        model.addAttribute("lista", svc.todosBeneficios());
        model.addAttribute("icones", LandingContentService.ICONES_DISPONIVEIS);
        return "landing-admin/beneficios";
    }

    @PostMapping("/beneficios/novo")
    public String beneficioNovo(@RequestParam String icone,
                                @RequestParam int cor,
                                @RequestParam String titulo,
                                @RequestParam(required = false) String descricao,
                                @RequestParam int ordem,
                                RedirectAttributes ra) {
        svc.salvarBeneficio(LpBeneficioEntity.builder()
            .icone(icone).cor(cor).titulo(titulo).descricao(descricao)
            .ordem(ordem).ativo(true).build());
        ra.addFlashAttribute("successMessage", "Benefício adicionado.");
        return "redirect:/admin/landing/beneficios";
    }

    @PostMapping("/beneficios/{id}/editar")
    public String beneficioEditar(@PathVariable UUID id,
                                  @RequestParam String icone,
                                  @RequestParam int cor,
                                  @RequestParam String titulo,
                                  @RequestParam(required = false) String descricao,
                                  @RequestParam int ordem,
                                  RedirectAttributes ra) {
        LpBeneficioEntity e = svc.buscarBeneficio(id);
        e.setIcone(icone); e.setCor(cor); e.setTitulo(titulo);
        e.setDescricao(descricao); e.setOrdem(ordem);
        svc.salvarBeneficio(e);
        ra.addFlashAttribute("successMessage", "Benefício atualizado.");
        return "redirect:/admin/landing/beneficios";
    }

    @PostMapping("/beneficios/{id}/alternar")
    public String beneficioAlternar(@PathVariable UUID id, RedirectAttributes ra) {
        svc.alternarBeneficio(id);
        return "redirect:/admin/landing/beneficios";
    }

    @PostMapping("/beneficios/{id}/excluir")
    public String beneficioExcluir(@PathVariable UUID id, RedirectAttributes ra) {
        svc.excluirBeneficio(id);
        ra.addFlashAttribute("successMessage", "Benefício removido.");
        return "redirect:/admin/landing/beneficios";
    }

    // ── Steps ────────────────────────────────────────────────────────────────

    @GetMapping("/steps")
    public String steps(Model model) {
        model.addAttribute("lista", svc.todosSteps());
        return "landing-admin/steps";
    }

    @PostMapping("/steps/novo")
    public String stepNovo(@RequestParam int numero,
                           @RequestParam String titulo,
                           @RequestParam(required = false) String descricao,
                           @RequestParam int ordem,
                           RedirectAttributes ra) {
        svc.salvarStep(LpStepEntity.builder()
            .numero(numero).titulo(titulo).descricao(descricao)
            .ordem(ordem).ativo(true).build());
        ra.addFlashAttribute("successMessage", "Passo adicionado.");
        return "redirect:/admin/landing/steps";
    }

    @PostMapping("/steps/{id}/editar")
    public String stepEditar(@PathVariable UUID id,
                             @RequestParam int numero,
                             @RequestParam String titulo,
                             @RequestParam(required = false) String descricao,
                             @RequestParam int ordem,
                             RedirectAttributes ra) {
        LpStepEntity e = svc.buscarStep(id);
        e.setNumero(numero); e.setTitulo(titulo);
        e.setDescricao(descricao); e.setOrdem(ordem);
        svc.salvarStep(e);
        ra.addFlashAttribute("successMessage", "Passo atualizado.");
        return "redirect:/admin/landing/steps";
    }

    @PostMapping("/steps/{id}/alternar")
    public String stepAlternar(@PathVariable UUID id, RedirectAttributes ra) {
        svc.alternarStep(id);
        return "redirect:/admin/landing/steps";
    }

    @PostMapping("/steps/{id}/excluir")
    public String stepExcluir(@PathVariable UUID id, RedirectAttributes ra) {
        svc.excluirStep(id);
        ra.addFlashAttribute("successMessage", "Passo removido.");
        return "redirect:/admin/landing/steps";
    }
}
