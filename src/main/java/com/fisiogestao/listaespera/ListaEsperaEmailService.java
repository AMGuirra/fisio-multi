package com.fisiogestao.listaespera;

import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.email.EmailLogType;
import com.fisiogestao.email.service.EmailRequest;
import com.fisiogestao.email.service.SystemEmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ListaEsperaEmailService {

    private static final Logger log = LoggerFactory.getLogger(ListaEsperaEmailService.class);

    private final SystemEmailService systemEmailService;
    private final ConfiguracaoClinicaRepository configuracaoClinicaRepository;

    public void enviarConfirmacaoEntrada(ListaEsperaEntity entrada) {
        String email = resolverEmail(entrada);
        if (!possuiEmail(email)) return;

        String periodo = switch (entrada.getPeriodoPreferido()) {
            case MANHA -> "Manhã";
            case TARDE -> "Tarde";
            case NOITE -> "Noite";
            default    -> "Qualquer horário";
        };

        try {
            systemEmailService.send(EmailRequest.builder()
                    .type(EmailLogType.LISTA_ESPERA_CONFIRMACAO)
                    .subject("Você está na lista de espera")
                    .to(email)
                    .templateName("email/lista-espera-confirmacao")
                    .templateId("email/lista-espera-confirmacao")
                    .entityType("lista_espera")
                    .entityId(entrada.getId())
                    .triggeredBy("sistema")
                    .templateVariables(Map.of(
                            "nomePaciente", entrada.getNomeExibicao(),
                            "periodoPreferido", periodo
                    ))
                    .build());
        } catch (Exception ex) {
            log.error("Falha ao enviar confirmação de lista de espera para {}: {}", email, ex.getMessage());
        }
    }

    public void enviarNotificacaoVaga(ListaEsperaEntity entrada) {
        String email = resolverEmail(entrada);
        if (!possuiEmail(email)) return;

        String nomeClinica = configuracaoClinicaRepository
                .findFirstByAtivoTrueOrderByCreatedAtAsc()
                .map(c -> c.getNomeClinica())
                .orElse("Fisioterapia");

        try {
            systemEmailService.send(EmailRequest.builder()
                    .type(EmailLogType.LISTA_ESPERA_VAGA_DISPONIVEL)
                    .subject("Há uma vaga disponível para você — " + nomeClinica)
                    .to(email)
                    .templateName("email/lista-espera-vaga-disponivel")
                    .templateId("email/lista-espera-vaga-disponivel")
                    .entityType("lista_espera")
                    .entityId(entrada.getId())
                    .triggeredBy("sistema")
                    .templateVariables(Map.of(
                            "nomePaciente", entrada.getNomeExibicao(),
                            "nomeClinica", nomeClinica
                    ))
                    .build());
        } catch (Exception ex) {
            log.error("Falha ao enviar notificação de vaga para {}: {}", email, ex.getMessage());
        }
    }

    private String resolverEmail(ListaEsperaEntity entrada) {
        if (entrada.getCliente() != null && possuiEmail(entrada.getCliente().getEmail())) {
            return entrada.getCliente().getEmail();
        }
        return entrada.getEmailPublico();
    }

    private boolean possuiEmail(String email) {
        return email != null && !email.isBlank() && email.contains("@");
    }
}
