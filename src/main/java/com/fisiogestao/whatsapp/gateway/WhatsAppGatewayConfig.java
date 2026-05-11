package com.fisiogestao.whatsapp.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(WhatsAppProperties.class)
public class WhatsAppGatewayConfig {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppGatewayConfig.class);

    @Bean
    public WhatsAppGateway whatsAppGateway(WhatsAppProperties props) {
        if (props.isEnabled() && props.getUrlEnvio() != null && !props.getUrlEnvio().isBlank()) {
            log.info("[WhatsApp] Gateway HTTP ativado. Endpoint: {}", props.getUrlEnvio());
            return new HttpWhatsAppGateway(props);
        }
        log.warn("[WhatsApp] Gateway em modo SIMULADO (logging). "
                + "Configure app.whatsapp.enabled=true e app.whatsapp.url-envio para ativar o envio real.");
        return new LoggingWhatsAppGateway();
    }
}
