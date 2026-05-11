package com.fisiogestao.whatsapp.service;

import com.fisiogestao.whatsapp.MensagemWhatsAppEntity;
import com.fisiogestao.whatsapp.StatusMensagemWhatsApp;
import com.fisiogestao.whatsapp.gateway.WhatsAppGateway;
import com.fisiogestao.whatsapp.gateway.WhatsAppGatewayException;
import com.fisiogestao.whatsapp.gateway.WhatsAppProperties;
import com.fisiogestao.whatsapp.repository.MensagemWhatsAppRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Processa periodicamente a fila de mensagens WhatsApp com status PENDENTE.
 * <p>
 * O intervalo é configurado via {@code app.whatsapp.intervalo-ms} (padrão 30 s).
 * Para evitar sobrecarga, processa no máximo 20 mensagens por ciclo.
 * Após {@code app.whatsapp.max-tentativas} falhas consecutivas, a mensagem é marcada como ERRO.
 */
@Component
@RequiredArgsConstructor
public class WhatsAppQueueProcessor {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppQueueProcessor.class);
    private static final int BATCH_SIZE = 20;

    private final MensagemWhatsAppRepository repository;
    private final WhatsAppGateway gateway;
    private final WhatsAppProperties props;

    @Scheduled(fixedDelayString = "${app.whatsapp.intervalo-ms:30000}")
    @Transactional
    public void processar() {
        if (!gateway.isOperacional()) {
            // Em modo simulado (dev), ainda processa para registrar no log e limpar a fila
        }

        List<MensagemWhatsAppEntity> pendentes = repository
                .findPendentesParaEnvio(StatusMensagemWhatsApp.PENDENTE, PageRequest.of(0, BATCH_SIZE));

        if (pendentes.isEmpty()) {
            return;
        }

        log.info("[WhatsApp] Processando {} mensagem(ns) pendente(s).", pendentes.size());

        for (MensagemWhatsAppEntity msg : pendentes) {
            tentarEnviar(msg);
        }
    }

    private void tentarEnviar(MensagemWhatsAppEntity msg) {
        try {
            gateway.enviar(msg.getDestinatario(), msg.getConteudo());

            msg.setStatus(StatusMensagemWhatsApp.ENVIADO);
            msg.setEnviadaEm(OffsetDateTime.now());
            msg.setErro(null);
            repository.save(msg);

        } catch (WhatsAppGatewayException e) {
            int tentativas = msg.getTentativas() + 1;
            msg.setTentativas(tentativas);
            msg.setErro(e.getMessage());

            if (tentativas >= props.getMaxTentativas()) {
                log.error("[WhatsApp] Mensagem {} marcada como ERRO após {} tentativas: {}",
                        msg.getId(), tentativas, e.getMessage());
                msg.setStatus(StatusMensagemWhatsApp.ERRO);
            } else {
                log.warn("[WhatsApp] Tentativa {}/{} falhou para {}: {}",
                        tentativas, props.getMaxTentativas(), msg.getDestinatario(), e.getMessage());
            }
            repository.save(msg);
        }
    }
}
