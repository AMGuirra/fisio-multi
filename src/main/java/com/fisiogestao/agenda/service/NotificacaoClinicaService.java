package com.fisiogestao.agenda.service;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.config.ClinicTimezoneHolder;
import com.fisiogestao.email.EmailLogType;
import com.fisiogestao.email.service.EmailRequest;
import com.fisiogestao.email.service.SystemEmailService;
import com.fisiogestao.financeiro.StatusLancamentoFinanceiro;
import com.fisiogestao.financeiro.TipoLancamentoFinanceiro;
import com.fisiogestao.financeiro.repository.LancamentoFinanceiroRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Serviço de notificações automáticas diárias para o fisioterapeuta.
 * <ul>
 *   <li><b>06h</b> (fuso da clínica) — Agenda do dia com todas as sessões agendadas.</li>
 *   <li><b>19h</b> (fuso da clínica) — Resumo noturno: sessões pendentes de baixa + financeiro pendente.</li>
 * </ul>
 * Ambos os e-mails são enviados para o {@code emailContato} da configuração operacional da clínica.
 */
@Service
@RequiredArgsConstructor
public class NotificacaoClinicaService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoClinicaService.class);

    private static final DateTimeFormatter FMT_DATA  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA  = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_DIA_SEMANA = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy",
            java.util.Locale.forLanguageTag("pt-BR"));

    private final SessaoRepository                sessaoRepository;
    private final LancamentoFinanceiroRepository  lancamentoRepository;
    private final ConfiguracaoClinicaRepository   configuracaoClinicaRepository;
    private final SystemEmailService              systemEmailService;

    /**
     * Dispara a cada hora exata. Verifica se é 6h ou 19h no fuso da clínica
     * e aciona a notificação correspondente.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void verificarNotificacoes() {
        int hora = ClinicTimezoneHolder.now().getHour();
        if (hora == 6)  enviarAgendaDiaria();
        if (hora == 19) enviarResumoNoturno();
    }

    // -----------------------------------------------------------------------
    // Agenda diária — 6h
    // -----------------------------------------------------------------------

    void enviarAgendaDiaria() {
        Optional<ConfiguracaoClinicaEntity> configOpt =
                configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc();

        String emailDestino = configOpt.map(ConfiguracaoClinicaEntity::getEmailContato).orElse(null);
        if (emailDestino == null || emailDestino.isBlank() || !emailDestino.contains("@")) {
            log.warn("[AgendaDiaria] emailContato não configurado — notificação ignorada.");
            return;
        }

        ZoneId zone        = configOpt.map(c -> ZoneId.of(c.getTimezoneId())).orElse(ClinicTimezoneHolder.zoneId());
        String nomeClinica = configOpt.map(ConfiguracaoClinicaEntity::getNomeClinica).orElse("Fisioterapia");

        LocalDate hoje      = ClinicTimezoneHolder.today();
        OffsetDateTime ini  = hoje.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime fim  = hoje.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        List<SessaoEntity> sessoes = sessaoRepository.findAgendadasNoPeriodo(ini, fim);
        if (sessoes.isEmpty()) {
            log.info("[AgendaDiaria] Nenhuma sessão agendada para hoje — e-mail não enviado.");
            return;
        }

        List<Map<String, String>> linhas = new ArrayList<>();
        for (SessaoEntity s : sessoes) {
            Map<String, String> linha = new HashMap<>();
            linha.put("hora",             s.getDataHoraInicio().atZoneSameInstant(zone).format(FMT_HORA));
            linha.put("paciente",         s.getCliente().getNomeCompleto());
            linha.put("tipoAtendimento",  s.getTipoAtendimento() != null && !s.getTipoAtendimento().isBlank()
                                          ? s.getTipoAtendimento() : "—");
            linhas.add(linha);
        }

        String dataFormatada = hoje.atStartOfDay(zone).format(FMT_DIA_SEMANA);

        try {
            systemEmailService.send(EmailRequest.builder()
                    .type(EmailLogType.AGENDA_DIARIA)
                    .subject("📅 Agenda de hoje — " + dataFormatada.substring(0, 1).toUpperCase()
                             + dataFormatada.substring(1) + " | " + nomeClinica)
                    .to(emailDestino)
                    .templateName("email/agenda-diaria")
                    .templateId("email/agenda-diaria")
                    .triggeredBy("sistema")
                    .entityType("agenda")
                    .templateVariables(Map.of(
                            "nomeClinica",   nomeClinica,
                            "data",          dataFormatada,
                            "sessoes",       linhas,
                            "totalSessoes",  sessoes.size()
                    ))
                    .build());

            log.info("[AgendaDiaria] E-mail enviado para {} com {} sessão(ões).", emailDestino, sessoes.size());
        } catch (Exception ex) {
            log.error("[AgendaDiaria] Falha ao enviar e-mail: {}", ex.getMessage(), ex);
        }
    }

    // -----------------------------------------------------------------------
    // Resumo noturno — 19h
    // -----------------------------------------------------------------------

    void enviarResumoNoturno() {
        Optional<ConfiguracaoClinicaEntity> configOpt =
                configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc();

        String emailDestino = configOpt.map(ConfiguracaoClinicaEntity::getEmailContato).orElse(null);
        if (emailDestino == null || emailDestino.isBlank() || !emailDestino.contains("@")) {
            log.warn("[ResumoNoturno] emailContato não configurado — notificação ignorada.");
            return;
        }

        ZoneId zone        = configOpt.map(c -> ZoneId.of(c.getTimezoneId())).orElse(ClinicTimezoneHolder.zoneId());
        String nomeClinica = configOpt.map(ConfiguracaoClinicaEntity::getNomeClinica).orElse("Fisioterapia");

        OffsetDateTime agora = ClinicTimezoneHolder.now();

        // Sessões que já passaram e ainda estão AGENDADAS (precisam de baixa)
        List<SessaoEntity> pendentesBaixa = sessaoRepository.findAgendadasPendentesDeBaixa(agora);

        // Financeiro pendente (receitas)
        long      totalLancamentos  = lancamentoRepository.countByTipoAndStatus(
                TipoLancamentoFinanceiro.RECEITA, StatusLancamentoFinanceiro.PENDENTE);
        BigDecimal valorPendente    = lancamentoRepository.sumValorByTipoAndStatus(
                TipoLancamentoFinanceiro.RECEITA, StatusLancamentoFinanceiro.PENDENTE);

        boolean temPendencia = !pendentesBaixa.isEmpty() || totalLancamentos > 0;
        if (!temPendencia) {
            log.info("[ResumoNoturno] Nenhuma pendência encontrada — e-mail não enviado.");
            return;
        }

        List<Map<String, String>> linhasBaixa = new ArrayList<>();
        for (SessaoEntity s : pendentesBaixa) {
            Map<String, String> linha = new HashMap<>();
            linha.put("hora",     s.getDataHoraInicio().atZoneSameInstant(zone).format(FMT_HORA));
            linha.put("data",     s.getDataHoraInicio().atZoneSameInstant(zone).format(FMT_DATA));
            linha.put("paciente", s.getCliente().getNomeCompleto());
            linhasBaixa.add(linha);
        }

        String dataHoje = agora.atZoneSameInstant(zone).format(FMT_DATA);

        // Formata o valor monetário em pt-BR
        String valorFormatado = java.text.NumberFormat
                .getCurrencyInstance(java.util.Locale.forLanguageTag("pt-BR"))
                .format(valorPendente != null ? valorPendente : BigDecimal.ZERO);

        try {
            systemEmailService.send(EmailRequest.builder()
                    .type(EmailLogType.RESUMO_NOTURNO)
                    .subject("🌙 Pendências do dia — " + dataHoje + " | " + nomeClinica)
                    .to(emailDestino)
                    .templateName("email/resumo-noturno")
                    .templateId("email/resumo-noturno")
                    .triggeredBy("sistema")
                    .entityType("agenda")
                    .templateVariables(Map.of(
                            "nomeClinica",       nomeClinica,
                            "data",              dataHoje,
                            "sessoesBaixa",      linhasBaixa,
                            "totalBaixa",        pendentesBaixa.size(),
                            "totalFinanceiro",   totalLancamentos,
                            "valorFinanceiro",   valorFormatado
                    ))
                    .build());

            log.info("[ResumoNoturno] E-mail enviado para {} — {} baixa(s) pendente(s), {} lançamento(s) financeiro(s).",
                    emailDestino, pendentesBaixa.size(), totalLancamentos);
        } catch (Exception ex) {
            log.error("[ResumoNoturno] Falha ao enviar e-mail: {}", ex.getMessage(), ex);
        }
    }
}
