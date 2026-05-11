package com.fisiogestao.whatsapp.gateway;

public class WhatsAppGatewayException extends RuntimeException {

    private final int httpStatus;

    public WhatsAppGatewayException(String message) {
        super(message);
        this.httpStatus = 0;
    }

    public WhatsAppGatewayException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public WhatsAppGatewayException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = 0;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
