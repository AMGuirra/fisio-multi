package com.fisiogestao.landing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LandingContentService {

    private final LpConfigRepository       configRepo;
    private final LpTestemunhoRepository   testemunhoRepo;
    private final LpPlanoRepository        planoRepo;
    private final LpFaqRepository          faqRepo;
    private final LpBeneficioRepository    beneficioRepo;
    private final LpStepRepository         stepRepo;

    // ── DTOs públicos ────────────────────────────────────────────────────────

    public record HeroData(
        String badge,
        String titulo,
        String subtitulo,
        String ctaPrimario,
        String ctaPrimarioLink,
        String ctaSecundario,
        String ctaSecundarioLink,
        String stat1Valor, String stat1Label,
        String stat2Valor, String stat2Label,
        String stat3Valor, String stat3Label,
        String stat4Valor, String stat4Label
    ) {}

    public record SecoesConfig(
        boolean comoFunciona,
        boolean funcionalidades,
        boolean beneficios,
        boolean depoimentos,
        boolean planos,
        boolean faq
    ) {}

    public record PlanoRecurso(String texto, boolean incluido) {}

    public record PlanoDto(
        UUID   id,
        String nome,
        String descricao,
        String preco,
        String periodo,
        List<PlanoRecurso> recursos,
        boolean destaque,
        String badgeTexto,
        String ctaTexto,
        String ctaLink,
        int ordem
    ) {}

    public record LandingPageModel(
        HeroData             hero,
        SecoesConfig         secoes,
        List<LpTestemunhoEntity> testemunhos,
        List<PlanoDto>       planos,
        List<LpFaqEntity>    faqs,
        List<LpBeneficioEntity> beneficios,
        List<LpStepEntity>   steps
    ) {}

    // ── Carregamento para a landing page pública ─────────────────────────────

    @Transactional(readOnly = true)
    public LandingPageModel loadLandingData() {
        Map<String, String> cfg = loadConfigMap();
        return new LandingPageModel(
            buildHero(cfg),
            buildSecoes(cfg),
            testemunhoRepo.findAllByAtivoTrueOrderByOrdemAsc(),
            planoRepo.findAllByAtivoTrueOrderByOrdemAsc().stream().map(this::toPlanoDto).toList(),
            faqRepo.findAllByAtivoTrueOrderByOrdemAsc(),
            beneficioRepo.findAllByAtivoTrueOrderByOrdemAsc(),
            stepRepo.findAllByAtivoTrueOrderByOrdemAsc()
        );
    }

    // ── Config: hero e seções ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, String> loadConfigMap() {
        return configRepo.findAll().stream()
            .filter(e -> e.getValor() != null)
            .collect(Collectors.toMap(LpConfigEntity::getChave, LpConfigEntity::getValor));
    }

    @Transactional
    public void saveConfigs(Map<String, String> params) {
        params.forEach((chave, valor) -> {
            LpConfigEntity e = configRepo.findById(chave)
                .orElseGet(() -> LpConfigEntity.builder().chave(chave).build());
            e.setValor(valor);
            configRepo.save(e);
        });
    }

    // ── Testemunhos ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LpTestemunhoEntity> todosTestemunhos() {
        return testemunhoRepo.findAllByOrderByOrdemAsc();
    }

    @Transactional
    public void salvarTestemunho(LpTestemunhoEntity e) {
        testemunhoRepo.save(e);
    }

    @Transactional
    public void excluirTestemunho(UUID id) {
        testemunhoRepo.deleteById(id);
    }

    @Transactional
    public void alternarTestemunho(UUID id) {
        testemunhoRepo.findById(id).ifPresent(e -> {
            e.setAtivo(!Boolean.TRUE.equals(e.getAtivo()));
            testemunhoRepo.save(e);
        });
    }

    @Transactional(readOnly = true)
    public LpTestemunhoEntity buscarTestemunho(UUID id) {
        return testemunhoRepo.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Depoimento não encontrado"));
    }

    // ── Planos ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LpPlanoEntity> todosPlanos() {
        return planoRepo.findAllByOrderByOrdemAsc();
    }

    @Transactional
    public void salvarPlano(LpPlanoEntity e) {
        planoRepo.save(e);
    }

    @Transactional
    public void excluirPlano(UUID id) {
        planoRepo.deleteById(id);
    }

    @Transactional
    public void alternarPlano(UUID id) {
        planoRepo.findById(id).ifPresent(e -> {
            e.setAtivo(!Boolean.TRUE.equals(e.getAtivo()));
            planoRepo.save(e);
        });
    }

    @Transactional(readOnly = true)
    public LpPlanoEntity buscarPlano(UUID id) {
        return planoRepo.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Plano não encontrado"));
    }

    // ── FAQ ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LpFaqEntity> todosFaqs() {
        return faqRepo.findAllByOrderByOrdemAsc();
    }

    @Transactional
    public void salvarFaq(LpFaqEntity e) {
        faqRepo.save(e);
    }

    @Transactional
    public void excluirFaq(UUID id) {
        faqRepo.deleteById(id);
    }

    @Transactional
    public void alternarFaq(UUID id) {
        faqRepo.findById(id).ifPresent(e -> {
            e.setAtivo(!Boolean.TRUE.equals(e.getAtivo()));
            faqRepo.save(e);
        });
    }

    @Transactional(readOnly = true)
    public LpFaqEntity buscarFaq(UUID id) {
        return faqRepo.findById(id)
            .orElseThrow(() -> new NoSuchElementException("FAQ não encontrado"));
    }

    // ── Benefícios ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LpBeneficioEntity> todosBeneficios() {
        return beneficioRepo.findAllByOrderByOrdemAsc();
    }

    @Transactional
    public void salvarBeneficio(LpBeneficioEntity e) {
        beneficioRepo.save(e);
    }

    @Transactional
    public void excluirBeneficio(UUID id) {
        beneficioRepo.deleteById(id);
    }

    @Transactional
    public void alternarBeneficio(UUID id) {
        beneficioRepo.findById(id).ifPresent(e -> {
            e.setAtivo(!Boolean.TRUE.equals(e.getAtivo()));
            beneficioRepo.save(e);
        });
    }

    @Transactional(readOnly = true)
    public LpBeneficioEntity buscarBeneficio(UUID id) {
        return beneficioRepo.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Benefício não encontrado"));
    }

    // ── Steps ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LpStepEntity> todosSteps() {
        return stepRepo.findAllByOrderByOrdemAsc();
    }

    @Transactional
    public void salvarStep(LpStepEntity e) {
        stepRepo.save(e);
    }

    @Transactional
    public void excluirStep(UUID id) {
        stepRepo.deleteById(id);
    }

    @Transactional
    public void alternarStep(UUID id) {
        stepRepo.findById(id).ifPresent(e -> {
            e.setAtivo(!Boolean.TRUE.equals(e.getAtivo()));
            stepRepo.save(e);
        });
    }

    @Transactional(readOnly = true)
    public LpStepEntity buscarStep(UUID id) {
        return stepRepo.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Passo não encontrado"));
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private HeroData buildHero(Map<String, String> cfg) {
        return new HeroData(
            cfg.getOrDefault("hero.badge",              "Software especializado em fisioterapia"),
            cfg.getOrDefault("hero.titulo",             "Gestão clínica que trabalha com você"),
            cfg.getOrDefault("hero.subtitulo",          ""),
            cfg.getOrDefault("hero.cta_primario",       "Ver funcionalidades"),
            cfg.getOrDefault("hero.cta_primario_link",  "#funcionalidades"),
            cfg.getOrDefault("hero.cta_secundario",     "Acessar o sistema"),
            cfg.getOrDefault("hero.cta_secundario_link","/login"),
            cfg.getOrDefault("hero.stat1_valor",        "360°"),
            cfg.getOrDefault("hero.stat1_label",        "Visão da clínica"),
            cfg.getOrDefault("hero.stat2_valor",        "24h"),
            cfg.getOrDefault("hero.stat2_label",        "Lembretes automáticos"),
            cfg.getOrDefault("hero.stat3_valor",        "100%"),
            cfg.getOrDefault("hero.stat3_label",        "LGPD Compliant"),
            cfg.getOrDefault("hero.stat4_valor",        "0"),
            cfg.getOrDefault("hero.stat4_label",        "Sessões perdidas sem aviso")
        );
    }

    private SecoesConfig buildSecoes(Map<String, String> cfg) {
        return new SecoesConfig(
            isTrue(cfg, "secao.como_funciona"),
            isTrue(cfg, "secao.funcionalidades"),
            isTrue(cfg, "secao.beneficios"),
            isTrue(cfg, "secao.depoimentos"),
            isTrue(cfg, "secao.planos"),
            isTrue(cfg, "secao.faq")
        );
    }

    private boolean isTrue(Map<String, String> cfg, String key) {
        return "true".equalsIgnoreCase(cfg.getOrDefault(key, "true"));
    }

    public PlanoDto toPlanoDto(LpPlanoEntity e) {
        return new PlanoDto(
            e.getId(),
            e.getNome(),
            e.getDescricao(),
            formatarPreco(e.getPreco()),
            e.getPeriodo(),
            parseRecursos(e.getRecursos()),
            Boolean.TRUE.equals(e.getDestaque()),
            e.getBadgeTexto(),
            e.getCtaTexto(),
            e.getCtaLink(),
            e.getOrdem() != null ? e.getOrdem() : 0
        );
    }

    public List<PlanoRecurso> parseRecursos(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split("\n"))
            .map(String::trim)
            .filter(l -> l.startsWith("+") || l.startsWith("-"))
            .map(l -> new PlanoRecurso(l.substring(1).trim(), l.startsWith("+")))
            .toList();
    }

    private String formatarPreco(BigDecimal preco) {
        if (preco == null) return "0";
        return preco.stripTrailingZeros().toPlainString().replaceAll("\\.0+$", "");
    }

    @Transactional(readOnly = true)
    public SecoesConfig buildSecoesFromDb() {
        return buildSecoes(loadConfigMap());
    }

    /** Lista de ícones disponíveis para benefícios */
    public static final List<String> ICONES_DISPONIVEIS = List.of(
        "BELL", "LIST", "CHART", "CARD", "MOBILE", "LOCK",
        "CLOCK", "USERS", "CALENDAR", "STAR", "CHECK", "PLUS"
    );
}
