package com.fisiogestao.whatsapp;

import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.config.ClinicTimezoneHolder;
import com.fisiogestao.pacientes.ClienteEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gera links wa.me com mensagens pré-configuradas para os pacientes.
 * Nenhuma mensagem é enviada pelo servidor: o usuário clica e envia pelo próprio WhatsApp.
 */
@Service
@RequiredArgsConstructor
public class WhatsAppMensagemService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ConfiguracaoClinicaRepository configuracaoClinicaRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /** Links de WhatsApp disponíveis para uma sessão agendada. */
    public record LinksSessao(String agendamento, String lembrete, String confirmacao, String portal) {
    }

    /**
     * Monta os links das sessões agendadas cujo paciente tem telefone válido, indexados pelo id da sessão.
     * Sessões sem link não entram no mapa.
     */
    @Transactional(readOnly = true)
    public Map<UUID, LinksSessao> linksSessoes(Collection<SessaoEntity> sessoes) {
        Map<UUID, LinksSessao> links = new LinkedHashMap<>();
        if (sessoes == null || sessoes.isEmpty()) {
            return links;
        }
        String nomeClinica = nomeClinica();
        LocalDate hoje = ClinicTimezoneHolder.today();
        for (SessaoEntity sessao : sessoes) {
            ClienteEntity paciente = sessao.getCliente();
            if (sessao.getStatus() != SessaoStatus.AGENDADA || paciente == null
                    || WhatsAppUrlBuilder.normalizarNumero(paciente.getTelefone()) == null) {
                continue;
            }
            String telefone = paciente.getTelefone();
            links.put(sessao.getId(), new LinksSessao(
                    WhatsAppUrlBuilder.montarUrl(telefone, mensagemAgendamento(sessao, nomeClinica)),
                    WhatsAppUrlBuilder.montarUrl(telefone, mensagemLembrete(sessao, nomeClinica, hoje)),
                    WhatsAppUrlBuilder.montarUrl(telefone, mensagemConfirmacao(sessao, hoje)),
                    WhatsAppUrlBuilder.montarUrl(telefone, mensagemPortal(paciente))
            ));
        }
        return links;
    }

    /** Links de envio do portal do paciente, indexados pelo id do paciente. Pacientes sem telefone válido ficam de fora. */
    public Map<UUID, String> linksPortal(Collection<ClienteEntity> pacientes) {
        Map<UUID, String> links = new LinkedHashMap<>();
        if (pacientes == null) {
            return links;
        }
        for (ClienteEntity paciente : pacientes) {
            String url = linkPortal(paciente);
            if (url != null) {
                links.put(paciente.getId(), url);
            }
        }
        return links;
    }

    /** Link para enviar ao paciente o acesso ao portal, ou {@code null} sem telefone válido. */
    public String linkPortal(ClienteEntity paciente) {
        if (paciente == null || paciente.getPortalToken() == null) {
            return null;
        }
        return WhatsAppUrlBuilder.montarUrl(paciente.getTelefone(), mensagemPortal(paciente));
    }

    /**
     * Link para o paciente falar com a clínica pelo número configurado em Configurações da Clínica,
     * ou {@code null} quando a clínica não tiver WhatsApp válido cadastrado.
     */
    public String linkContatoClinica(ConfiguracaoClinicaEntity clinica, ClienteEntity paciente) {
        if (clinica == null) {
            return null;
        }
        String mensagem = paciente != null && paciente.getNomeCompleto() != null && !paciente.getNomeCompleto().isBlank()
                ? "Olá! Sou " + paciente.getNomeCompleto().trim() + ", paciente da clínica."
                : null;
        return WhatsAppUrlBuilder.montarUrl(clinica.getTelefoneWhatsapp(), mensagem);
    }

    String mensagemAgendamento(SessaoEntity sessao, String nomeClinica) {
        ZonedDateTime inicio = inicioNoFusoDaClinica(sessao);
        return "Olá " + saudacao(sessao.getCliente()) + ", seu agendamento na " + nomeClinica
                + " foi realizado com sucesso para " + inicio.format(DATA) + " às " + inicio.format(HORA) + ".";
    }

    String mensagemLembrete(SessaoEntity sessao, String nomeClinica, LocalDate hoje) {
        ZonedDateTime inicio = inicioNoFusoDaClinica(sessao);
        String dia = referenciaDia(inicio.toLocalDate(), hoje);
        return "Olá " + saudacao(sessao.getCliente()) + ", lembramos da sua sessão de Fisioterapia agendada para "
                + (dia != null ? dia + ", " : "") + inicio.format(DATA) + " às " + inicio.format(HORA)
                + ", na " + nomeClinica + ". Te esperamos!";
    }

    String mensagemConfirmacao(SessaoEntity sessao, LocalDate hoje) {
        ZonedDateTime inicio = inicioNoFusoDaClinica(sessao);
        String dia = referenciaDia(inicio.toLocalDate(), hoje);
        return "Olá " + saudacao(sessao.getCliente()) + "! Confirmamos sua sessão "
                + (dia != null ? dia + ", " : "em ") + inicio.format(DATA) + " às " + inicio.format(HORA) + "?"
                + " Responda com:\n1 - Sim, confirmo\n2 - Não poderei ir (preciso reagendar)";
    }

    String mensagemPortal(ClienteEntity paciente) {
        return "Olá " + saudacao(paciente) + ", acesse seu portal exclusivo para acompanhar o histórico"
                + " das suas sessões e saldo de pacotes: " + urlPortal(paciente);
    }

    private String urlPortal(ClienteEntity paciente) {
        String base = baseUrl == null || baseUrl.isBlank() ? "http://localhost:8080" : baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/paciente/" + paciente.getPortalToken();
    }

    private String nomeClinica() {
        return configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc()
                .map(ConfiguracaoClinicaEntity::getNomeClinica)
                .filter(nome -> !nome.isBlank())
                .map(String::trim)
                .orElse("nossa clínica");
    }

    private ZonedDateTime inicioNoFusoDaClinica(SessaoEntity sessao) {
        return sessao.getDataHoraInicio().atZoneSameInstant(ClinicTimezoneHolder.zoneId());
    }

    private String referenciaDia(LocalDate data, LocalDate hoje) {
        if (data.equals(hoje)) {
            return "hoje";
        }
        if (data.equals(hoje.plusDays(1))) {
            return "amanhã";
        }
        return null;
    }

    /** Primeiro nome do paciente, para uma saudação mais próxima. */
    private String saudacao(ClienteEntity paciente) {
        if (paciente == null || paciente.getNomeCompleto() == null || paciente.getNomeCompleto().isBlank()) {
            return "paciente";
        }
        return paciente.getNomeCompleto().trim().split("\\s+")[0];
    }
}
