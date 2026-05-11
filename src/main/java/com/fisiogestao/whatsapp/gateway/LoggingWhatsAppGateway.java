package com.fisiogestao.whatsapp.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementação de desenvolvimento/fallback do {@link WhatsAppGateway}.
 * Apenas registra a mensagem no log — não realiza envio real.
 * Ativado automaticamente quando nenhuma configuração HTTP está presente.
 */
public class LoggingWhatsAppGateway implements WhatsAppGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingWhatsAppGateway.class);

    @Override
    public void enviar(String numero, String mensagem) {
        log.info("[WhatsApp SIMULADO] Para: {} | Mensagem: {}", numero, mensagem);
    }

    @Override
    public boolean isOperacional() {
        return false;
    }
}
