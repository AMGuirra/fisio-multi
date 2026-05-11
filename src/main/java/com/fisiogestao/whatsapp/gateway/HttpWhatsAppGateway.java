package com.fisiogestao.whatsapp.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Implementação HTTP genérica do {@link WhatsAppGateway}.
 * Funciona com Evolution API, Z-API e qualquer API que aceite
 * um JSON simples com campos configuráveis.
 */
public class HttpWhatsAppGateway implements WhatsAppGateway {

    private static final Logger log = LoggerFactory.getLogger(HttpWhatsAppGateway.class);

    private final WhatsAppProperties props;
    private final HttpClient httpClient;

    public HttpWhatsAppGateway(WhatsAppProperties props) {
        this.props = props;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public void enviar(String numero, String mensagem) throws WhatsAppGatewayException {
        String corpo = buildJson(numero, mensagem);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(props.getUrlEnvio()))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8));

        if (props.getToken() != null && !props.getToken().isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + props.getToken());
        }

        HttpRequest request = requestBuilder.build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status < 200 || status >= 300) {
                log.warn("[WhatsApp] Envio falhou para {}. HTTP {}: {}", numero, status, response.body());
                throw new WhatsAppGatewayException(
                        "API retornou HTTP " + status + ": " + truncate(response.body(), 300),
                        status
                );
            }

            log.info("[WhatsApp] Mensagem enviada para {}. HTTP {}", numero, status);

        } catch (WhatsAppGatewayException e) {
            throw e;
        } catch (Exception e) {
            log.error("[WhatsApp] Erro de conexão ao enviar para {}: {}", numero, e.getMessage());
            throw new WhatsAppGatewayException("Erro de comunicação com a API WhatsApp: " + e.getMessage(), e);
        }
    }

    /**
     * Constrói o corpo JSON substituindo os campos configurados.
     * Escapa aspas e quebras de linha para garantir JSON válido.
     */
    private String buildJson(String numero, String mensagem) {
        return "{\"" + esc(props.getCampoNumero()) + "\":\"" + esc(numero) + "\","
                + "\"" + esc(props.getCampoMensagem()) + "\":\"" + esc(mensagem) + "\"}";
    }

    private static String esc(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
