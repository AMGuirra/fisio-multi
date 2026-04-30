package com.fisiogestao.auth;

import com.fisiogestao.auth.service.PasswordResetService;
import com.fisiogestao.email.EmailLogType;
import com.fisiogestao.email.service.EmailDispatchResult;
import com.fisiogestao.email.service.EmailRequest;
import com.fisiogestao.email.service.SystemEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.junit.jupiter.api.extension.ExtendWith(OutputCaptureExtension.class)
@SuppressWarnings("null")
class PasswordResetFlowIntegrationTest {

    private static final String TEST_EMAIL = "reset-flow@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioSistemaRepository usuarioSistemaRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetService passwordResetService;

    @MockitoBean
    private SystemEmailService systemEmailService;

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();
        usuarioSistemaRepository.findByEmailIgnoreCase(TEST_EMAIL)
                .ifPresent(usuarioSistemaRepository::delete);
    }

    @Test
    void forgotPasswordGeraTokenERegistraLinkNoLogQuandoEmailFalha(CapturedOutput output) throws Exception {
        UsuarioSistemaEntity usuario = criarUsuarioTeste();
        when(systemEmailService.send(any())).thenReturn(EmailDispatchResult.failure("SMTP indisponível para o teste."));

        mockMvc.perform(get("/esqueci-minha-senha"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")));

        mockMvc.perform(post("/esqueci-minha-senha")
                        .with(csrf())
                        .param("email", usuario.getEmail()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/esqueci-minha-senha"));

        assertTrue(passwordResetTokenRepository.findByUsuarioIdAndUsedAtIsNull(usuario.getId()).size() == 1);
        verify(systemEmailService).send(argThat(emailRequestComLinkDeResetPara(usuario)));
        assertTrue(output.getOut().contains("Link de redefinição disponível para desenvolvimento local com token oculto"));
        assertTrue(output.getOut().contains("http://localhost:8083/resetar-senha?token=<oculto>"));
        assertTrue(output.getOut().contains("app.security.log-sensitive-reset-links=true"));
    }

    @Test
    void resetPasswordConsomeTokenEAtualizaSenha() throws Exception {
        UsuarioSistemaEntity usuario = criarUsuarioTeste();
        PasswordResetService.PasswordResetLink link = passwordResetService.gerarLinkDefinicaoSenha(
                usuario,
                "127.0.0.1",
                "JUnit"
        );

        mockMvc.perform(get("/resetar-senha")
                        .param("token", link.token()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Salvar nova senha")));

        mockMvc.perform(post("/resetar-senha")
                        .with(csrf())
                        .param("token", link.token())
                        .param("password", "NovaSenha123")
                        .param("confirmPassword", "NovaSenha123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        UsuarioSistemaEntity usuarioAtualizado = usuarioSistemaRepository.findById(usuario.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("NovaSenha123", usuarioAtualizado.getPasswordHash()));
        assertTrue(passwordResetTokenRepository.findByUsuarioIdAndUsedAtIsNull(usuario.getId()).isEmpty());
        assertFalse(passwordResetService.tokenValido(link.token()));
    }

    @Test
    void forgotPasswordUsaBaseUrlEncaminhadaQuandoDisponivel() throws Exception {
        UsuarioSistemaEntity usuario = criarUsuarioTeste();
        when(systemEmailService.send(any())).thenReturn(EmailDispatchResult.failure("SMTP indisponível para o teste."));

        mockMvc.perform(post("/esqueci-minha-senha")
                        .with(csrf())
                        .header("X-Forwarded-Proto", "https")
                        .header("X-Forwarded-Host", "fisiogestao.exemplo.com")
                        .header("X-Forwarded-Port", "443")
                        .param("email", usuario.getEmail()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/esqueci-minha-senha"));

        verify(systemEmailService).send(argThat(emailRequestComLinkDeResetPara(usuario, "https://fisiogestao.exemplo.com/resetar-senha?token=")));
    }

    private UsuarioSistemaEntity criarUsuarioTeste() {
        String username = "rfu" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return usuarioSistemaRepository.save(UsuarioSistemaEntity.builder()
                .nome("Usuário Reset")
                .username(username)
                .email(TEST_EMAIL)
                .passwordHash(passwordEncoder.encode("SenhaAtual123"))
                .role(UserRole.ADMIN)
                .build());
    }

    private ArgumentMatcher<EmailRequest> emailRequestComLinkDeResetPara(UsuarioSistemaEntity usuario) {
        return emailRequestComLinkDeResetPara(usuario, "http://localhost:8083/resetar-senha?token=");
    }

    private ArgumentMatcher<EmailRequest> emailRequestComLinkDeResetPara(UsuarioSistemaEntity usuario, String prefixoEsperado) {
        return request -> request != null
                && request.type() == EmailLogType.RESET_SENHA
                && request.to() != null
                && request.to().contains(usuario.getEmail())
                && request.templateVariables().get("linkRedefinicao") instanceof String link
                && link.startsWith(prefixoEsperado);
    }
}
