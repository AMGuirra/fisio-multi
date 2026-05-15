package com.fisiogestao.email.service;

import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.email.EmailLogType;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.PlanoSessaoEntity;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PacienteEmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PacienteEmailNotificationService.class);
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SystemEmailService systemEmailService;
    private final ConfiguracaoClinicaRepository configuracaoClinicaRepository;

    @Value("${app.base-url:http://localhost:8083}")
    private String baseUrl;

    public void notificarConsumoPlano(PlanoSessaoEntity plano, int quantidade, String motivo) {
        if (!possuiEmail(plano.getCliente())) {
            return;
        }
        enviarConsumo(
                plano,
                quantidade,
                "DEBITO",
                quantidade > 1 ? "Sessões debitadas do seu plano" : "Sessão debitada do seu plano",
                texto(motivo, "Consumo registrado no plano de tratamento.")
        );
    }

    public void notificarEstornoPlano(PlanoSessaoEntity plano, int quantidade, String motivo) {
        if (!possuiEmail(plano.getCliente())) {
            return;
        }
        enviarConsumo(
                plano,
                quantidade,
                "ESTORNO",
                quantidade > 1 ? "Sessões devolvidas ao seu plano" : "Crédito devolvido ao seu plano",
                texto(motivo, "Crédito devolvido ao plano de tratamento.")
        );
    }

    private void enviarConsumo(
            PlanoSessaoEntity plano,
            int quantidade,
            String movimento,
            String assunto,
            String motivo
    ) {
        ClienteEntity paciente = plano.getCliente();
        try {
            systemEmailService.send(EmailRequest.builder()
                    .type(EmailLogType.PLANO_CONSUMO)
                    .subject(assunto)
                    .to(paciente.getEmail())
                    .templateName("email/plano-consumo")
                    .templateId("plano-consumo")
                    .entityType("plano_sessao")
                    .entityId(plano.getId())
                    .triggeredBy("sistema")
                    .templateVariables(variaveisConsumo(plano, Math.max(quantidade, 1), movimento, motivo))
                    .build());
        } catch (Exception ex) {
            log.error("Falha inesperada ao enviar notificação de consumo do plano para {}: {}",
                    paciente.getEmail(), ex.getMessage(), ex);
        }
    }

    private Map<String, Object> variaveisConsumo(
            PlanoSessaoEntity plano,
            int quantidade,
            String movimento,
            String motivo
    ) {
        int contratadas = numero(plano.getSessoesContratadas());
        int consumidas = numero(plano.getSessoesConsumidas());
        int restantes = Math.max(contratadas - consumidas, 0);
        ClienteEntity paciente = plano.getCliente();
        ConfiguracaoClinicaEntity clinica = configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc().orElse(null);

        LinkedHashMap<String, Object> variaveis = new LinkedHashMap<>();
        variaveis.put("preheader", "Confira o saldo atualizado do seu plano de tratamento.");
        variaveis.put("nomePaciente", texto(paciente.getNomeCompleto(), "Paciente"));
        variaveis.put("nomeClinica", clinica != null ? texto(clinica.getNomeClinica(), "Clínica") : "Clínica");
        variaveis.put("nomePlano", texto(plano.getNomePlano(), "Plano de tratamento"));
        variaveis.put("movimento", movimento);
        variaveis.put("movimentoLabel", "DEBITO".equals(movimento) ? "Débito de sessão" : "Estorno de sessão");
        variaveis.put("quantidade", quantidade);
        variaveis.put("motivo", motivo);
        variaveis.put("sessoesContratadas", contratadas);
        variaveis.put("sessoesConsumidas", consumidas);
        variaveis.put("sessoesRestantes", restantes);
        variaveis.put("valorTotal", dinheiro(plano.getValorTotal()));
        variaveis.put("dataInicio", plano.getDataInicio() == null ? "Não informada" : plano.getDataInicio().format(DATA));
        variaveis.put("dataValidade", plano.getDataValidade() == null ? "Sem validade definida" : plano.getDataValidade().format(DATA));
        variaveis.put("portalUrl", portalUrl(paciente));
        return Map.copyOf(variaveis);
    }

    private String portalUrl(ClienteEntity paciente) {
        return sanitizeBaseUrl(baseUrl) + "/paciente/" + paciente.getPortalToken();
    }

    private boolean possuiEmail(ClienteEntity paciente) {
        return paciente != null
                && paciente.getEmail() != null
                && !paciente.getEmail().isBlank()
                && paciente.getEmail().contains("@");
    }

    private int numero(Integer value) {
        return value == null ? 0 : value;
    }

    private String dinheiro(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor == null ? BigDecimal.ZERO : valor);
    }

    private String texto(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor.trim();
    }

    private String sanitizeBaseUrl(String value) {
        String normalized = value == null || value.isBlank() ? "http://localhost:8083" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
