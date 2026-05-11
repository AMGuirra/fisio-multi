package com.fisiogestao.whatsapp.gateway;

/**
 * Abstração para envio de mensagens WhatsApp.
 * Implemente esta interface para integrar com qualquer provedor
 * (Evolution API, Z-API, Twilio, etc.) sem alterar o restante do sistema.
 */
public interface WhatsAppGateway {

    /**
     * Envia uma mensagem de texto para o número informado.
     *
     * @param numero   Número do destinatário com DDI+DDD (apenas dígitos, ex: 5567999990000)
     * @param mensagem Texto da mensagem
     * @throws WhatsAppGatewayException se o envio falhar
     */
    void enviar(String numero, String mensagem) throws WhatsAppGatewayException;

    /**
     * Retorna {@code true} se este gateway está configurado e operacional.
     * O {@link LoggingWhatsAppGateway} retorna {@code false} pois é apenas
     * um placeholder de desenvolvimento.
     */
    default boolean isOperacional() {
        return true;
    }
}
