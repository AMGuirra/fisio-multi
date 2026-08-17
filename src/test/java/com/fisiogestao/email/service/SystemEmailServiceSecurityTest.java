package com.fisiogestao.email.service;

import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.email.EmailConfiguracaoEntity;
import com.fisiogestao.email.EmailLogEntity;
import com.fisiogestao.email.EmailLogType;
import com.fisiogestao.email.repository.EmailConfiguracaoRepository;
import com.fisiogestao.email.repository.EmailLogRepository;
import com.fisiogestao.shared.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemEmailServiceSecurityTest {

    @Mock EmailConfiguracaoRepository configuracaoRepository;
    @Mock EmailLogRepository logRepository;
    @Mock EmailTemplateService templateService;
    @Mock SecretCryptoService cryptoService;
    @Mock AuditService auditService;
    @Mock SmtpEndpointValidator endpointValidator;

    @InjectMocks
    SystemEmailService service;

    @Test
    void falhaSmtpDeveUsarIncidentIdEMascararDestinatarioPersistido() {
        EmailConfiguracaoEntity configuracao = EmailConfiguracaoEntity.builder()
                .smtpHost("smtp.exemplo.invalid")
                .smtpPort(587)
                .emailRemetente("remetente@exemplo.invalid")
                .build();
        when(endpointValidator.validarEResolver(configuracao.getSmtpHost(), configuracao.getSmtpPort()))
                .thenThrow(new BusinessException("Detalhe interno que nao pode vazar"));

        EmailDispatchResult result = service.sendWithConfiguration(configuracao, EmailRequest.builder()
                .type(EmailLogType.TESTE_SMTP)
                .to("usuario@exemplo.invalid")
                .subject("Teste ficticio")
                .build());

        assertFalse(result.sent());
        assertTrue(result.message().contains("Referencia:"));
        assertFalse(result.message().contains("Detalhe interno"));
        assertFalse(result.message().contains("usuario@exemplo.invalid"));

        ArgumentCaptor<EmailLogEntity> captor = ArgumentCaptor.forClass(EmailLogEntity.class);
        verify(logRepository).save(captor.capture());
        assertTrue(captor.getValue().getDestinatarioPrincipal().startsWith("u***"));
        assertFalse(captor.getValue().getMensagemErro().contains("Detalhe interno"));
    }

    @Test
    void sslDiretoDeveManterCadeiaX509PadraoEVerificarHostname() throws Exception {
        EmailConfiguracaoEntity configuracao = EmailConfiguracaoEntity.builder()
                .smtpHost("smtp.exemplo.invalid")
                .smtpPort(465)
                .emailRemetente("remetente@exemplo.invalid")
                .usaSsl(true)
                .usaTls(false)
                .build();
        var endpoint = new SmtpEndpointValidator.SmtpEndpoint(
                "smtp.exemplo.invalid", 465,
                InetAddress.getByAddress(new byte[]{(byte) 203, 0, 113, 10}));

        JavaMailSenderImpl sender = ReflectionTestUtils.invokeMethod(
                service, "buildMailSender", configuracao, endpoint);

        assertFalse(sender.getJavaMailProperties().containsKey("mail.smtp.ssl.trust"));
        assertTrue(Boolean.parseBoolean(
                sender.getJavaMailProperties().getProperty("mail.smtp.ssl.checkserveridentity")));
        assertTrue(sender.getJavaMailProperties().get("mail.smtp.socketFactory")
                instanceof PinnedSmtpSocketFactory);
    }
}
