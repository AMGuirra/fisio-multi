package com.fisiogestao.whatsapp.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do gateway WhatsApp lida do application.yml.
 *
 * Exemplo de configuração para Evolution API:
 * <pre>
 * app:
 *   whatsapp:
 *     enabled: true
 *     url-envio: "https://evolution.suaclinica.com/message/sendText/MinhaInstancia"
 *     campo-numero: "number"
 *     campo-mensagem: "text"
 *     token: "SEU_TOKEN_AQUI"
 *     max-tentativas: 3
 * </pre>
 *
 * Exemplo para Z-API:
 * <pre>
 * app:
 *   whatsapp:
 *     enabled: true
 *     url-envio: "https://api.z-api.io/instances/INSTANCE_ID/token/TOKEN/send-text"
 *     campo-numero: "phone"
 *     campo-mensagem: "message"
 *     token: ""
 *     max-tentativas: 3
 * </pre>
 */
@ConfigurationProperties(prefix = "app.whatsapp")
public class WhatsAppProperties {

    /** Ativa o envio real. Se false, usa LoggingWhatsAppGateway. */
    private boolean enabled = false;

    /** URL do endpoint de envio da API escolhida. */
    private String urlEnvio = "";

    /** Nome do campo JSON que representa o número de destino. */
    private String campoNumero = "number";

    /** Nome do campo JSON que representa o texto da mensagem. */
    private String campoMensagem = "text";

    /** Token/API key enviado no header Authorization: Bearer {token}. */
    private String token = "";

    /** Número máximo de tentativas antes de marcar a mensagem como ERRO. */
    private int maxTentativas = 3;

    /** Intervalo em milissegundos entre cada ciclo do processador da fila. */
    private long intervaloMs = 30_000L;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getUrlEnvio() { return urlEnvio; }
    public void setUrlEnvio(String urlEnvio) { this.urlEnvio = urlEnvio; }

    public String getCampoNumero() { return campoNumero; }
    public void setCampoNumero(String campoNumero) { this.campoNumero = campoNumero; }

    public String getCampoMensagem() { return campoMensagem; }
    public void setCampoMensagem(String campoMensagem) { this.campoMensagem = campoMensagem; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public int getMaxTentativas() { return maxTentativas; }
    public void setMaxTentativas(int maxTentativas) { this.maxTentativas = maxTentativas; }

    public long getIntervaloMs() { return intervaloMs; }
    public void setIntervaloMs(long intervaloMs) { this.intervaloMs = intervaloMs; }
}
