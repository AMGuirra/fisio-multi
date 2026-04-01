package com.fisiogestao.auth.service;

import com.fisiogestao.auth.PasswordResetTokenEntity;
import com.fisiogestao.auth.PasswordResetTokenRepository;
import com.fisiogestao.auth.UsuarioSistemaEntity;
import com.fisiogestao.auth.UsuarioSistemaRepository;
import com.fisiogestao.email.EmailLogType;
import com.fisiogestao.email.service.EmailDispatchResult;
import com.fisiogestao.email.service.EmailRequest;
import com.fisiogestao.email.service.SystemEmailService;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int TOKEN_TTL_MINUTES = 60;

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemEmailService systemEmailService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Transactional
    public EmailDispatchResult solicitarReset(String email, String requestIp, String userAgent) {
        if (email == null || email.isBlank()) {
            return EmailDispatchResult.success("Se o e-mail estiver cadastrado, enviaremos as instruções de recuperação.");
        }

        return usuarioSistemaRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                .map(usuario -> enviarEmailReset(usuario, requestIp, userAgent, EmailLogType.RESET_SENHA, "email/reset-senha", "reset-senha"))
                .orElseGet(() -> EmailDispatchResult.success("Se o e-mail estiver cadastrado, enviaremos as instruções de recuperação."));
    }

    @Transactional
    public PasswordResetLink gerarLinkDefinicaoSenha(UsuarioSistemaEntity usuario, String requestIp, String userAgent) {
        String token = gerarTokenSeguro();
        String tokenHash = hashToken(token);
        OffsetDateTime agora = OffsetDateTime.now(ZoneId.systemDefault());

        passwordResetTokenRepository.findByUsuarioIdAndUsedAtIsNull(usuario.getId())
                .forEach(item -> item.setUsedAt(agora));

        PasswordResetTokenEntity entity = PasswordResetTokenEntity.builder()
                .usuario(usuario)
                .tokenHash(tokenHash)
                .expiresAt(agora.plusMinutes(TOKEN_TTL_MINUTES))
                .requestedIp(requestIp)
                .requestedUserAgent(userAgent != null && userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent)
                .build();
        passwordResetTokenRepository.save(entity);

        return new PasswordResetLink(token, montarLink(token), entity.getExpiresAt());
    }

    @Transactional
    public void redefinirSenha(String rawToken, String novaSenha, String confirmacaoSenha) {
        if (novaSenha == null || novaSenha.isBlank()) {
            throw new BusinessException("Informe a nova senha.");
        }
        if (!novaSenha.equals(confirmacaoSenha)) {
            throw new BusinessException("A confirmação da senha não confere.");
        }

        PasswordResetTokenEntity token = validarToken(rawToken);
        UsuarioSistemaEntity usuario = token.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(novaSenha.trim()));
        token.setUsedAt(OffsetDateTime.now(ZoneId.systemDefault()));
        usuarioSistemaRepository.save(usuario);
        passwordResetTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public boolean tokenValido(String rawToken) {
        try {
            validarToken(rawToken);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    public EmailDispatchResult enviarEmailBoasVindas(UsuarioSistemaEntity usuario, String requestIp, String userAgent) {
        return enviarEmailReset(usuario, requestIp, userAgent, EmailLogType.BOAS_VINDAS_USUARIO, "email/novo-usuario", "novo-usuario");
    }

    public EmailDispatchResult enviarEmailBoasVindasParaUsuarioId(java.util.UUID usuarioId, String requestIp, String userAgent) {
        UsuarioSistemaEntity usuario = usuarioSistemaRepository.findById(usuarioId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado para envio do acesso."));
        return enviarEmailBoasVindas(usuario, requestIp, userAgent);
    }

    private EmailDispatchResult enviarEmailReset(
            UsuarioSistemaEntity usuario,
            String requestIp,
            String userAgent,
            EmailLogType tipo,
            String template,
            String templateId
    ) {
        PasswordResetLink link = gerarLinkDefinicaoSenha(usuario, requestIp, userAgent);
        return systemEmailService.send(
                EmailRequest.builder()
                        .type(tipo)
                        .subject(tipo == EmailLogType.RESET_SENHA ? "Recuperação de senha - FisioGestao" : "Seu acesso ao FisioGestao")
                        .to(usuario.getEmail())
                        .templateName(template)
                        .templateId(templateId)
                        .entityType("usuario_sistema")
                        .entityId(usuario.getId())
                        .triggeredBy("sistema")
                        .templateVariables(Map.of(
                                "nomeUsuario", usuario.getNome(),
                                "email", usuario.getEmail(),
                                "linkRedefinicao", link.link(),
                                "expiraEm", link.expiresAt(),
                                "baseUrl", baseUrl
                        ))
                        .build()
        );
    }

    private PasswordResetTokenEntity validarToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException("O link de redefinição é inválido.");
        }
        String hash = hashToken(rawToken);
        PasswordResetTokenEntity token = passwordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException("O link de redefinição é inválido ou já expirou."));

        OffsetDateTime agora = OffsetDateTime.now(ZoneId.systemDefault());
        if (!token.ativoEm(agora)) {
            throw new BusinessException("O link de redefinição é inválido ou já expirou.");
        }
        return token;
    }

    private String gerarTokenSeguro() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new BusinessException("Não foi possível validar o token de redefinição.");
        }
    }

    private String montarLink(String token) {
        return baseUrl + "/resetar-senha?token=" + token;
    }

    public record PasswordResetLink(String token, String link, OffsetDateTime expiresAt) {
    }
}
