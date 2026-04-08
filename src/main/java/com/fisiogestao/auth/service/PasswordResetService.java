package com.fisiogestao.auth.service;

import com.fisiogestao.auth.PasswordResetTokenEntity;
import com.fisiogestao.auth.PasswordResetTokenRepository;
import com.fisiogestao.auth.PasswordPolicy;
import com.fisiogestao.auth.UsuarioSistemaEntity;
import com.fisiogestao.auth.UsuarioSistemaRepository;
import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.config.TimezoneConfig;
import com.fisiogestao.email.EmailLogType;
import com.fisiogestao.email.service.EmailDispatchResult;
import com.fisiogestao.email.service.EmailRequest;
import com.fisiogestao.email.service.SystemEmailService;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PasswordResetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int TOKEN_TTL_MINUTES = 60;
    private static final ZoneId APP_ZONE_ID = ZoneId.of(TimezoneConfig.CLINIC_TIMEZONE);

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemEmailService systemEmailService;
    private final AuditService auditService;

    @Value("${app.base-url:http://localhost:8083}")
    private String baseUrl;

    @Transactional
    public EmailDispatchResult solicitarReset(String email, String requestIp, String userAgent) {
        return solicitarReset(email, requestIp, userAgent, null);
    }

    @Transactional
    public EmailDispatchResult solicitarReset(String email, String requestIp, String userAgent, String requestBaseUrl) {
        if (email == null || email.isBlank()) {
            return EmailDispatchResult.success("Se o e-mail estiver cadastrado, enviaremos as instruções de recuperação.");
        }

        return usuarioSistemaRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                .map(usuario -> enviarEmailReset(
                        usuario,
                        requestIp,
                        userAgent,
                        requestBaseUrl,
                        EmailLogType.RESET_SENHA,
                        "email/reset-senha",
                        "reset-senha"
                ))
                .orElseGet(() -> EmailDispatchResult.success("Se o e-mail estiver cadastrado, enviaremos as instruções de recuperação."));
    }

    @Transactional
    public PasswordResetLink gerarLinkDefinicaoSenha(UsuarioSistemaEntity usuario, String requestIp, String userAgent) {
        return gerarLinkDefinicaoSenha(usuario, requestIp, userAgent, null);
    }

    @Transactional
    public void redefinirSenha(String rawToken, String novaSenha, String confirmacaoSenha) {
        String senhaNormalizada = PasswordPolicy.normalize(novaSenha, true);
        String confirmacaoNormalizada = confirmacaoSenha == null ? null : confirmacaoSenha.trim();

        if (!senhaNormalizada.equals(confirmacaoNormalizada)) {
            throw new BusinessException("A confirmação da senha não confere.");
        }

        PasswordResetTokenEntity token = validarToken(rawToken);
        UsuarioSistemaEntity usuario = token.getUsuario();
        Map<String, Object> snapshotAnterior = snapshotResetUsuario(usuario, null);
        usuario.setPasswordHash(passwordEncoder.encode(senhaNormalizada));
        OffsetDateTime redefinicao = OffsetDateTime.now(APP_ZONE_ID);
        token.setUsedAt(redefinicao);
        usuarioSistemaRepository.save(usuario);
        passwordResetTokenRepository.save(token);
        auditService.recordManual(
                AuditModule.AUTENTICACAO,
                "usuario_sistema",
                usuario.getId().toString(),
                usuario.getNome(),
                AuditActionType.RESET_PASSWORD,
                snapshotAnterior,
                snapshotResetUsuario(usuario, redefinicao),
                null,
                "Senha redefinida pelo fluxo de recuperação.",
                AuditEventOrigin.ONLINE
        );
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
        return enviarEmailReset(usuario, requestIp, userAgent, null, EmailLogType.BOAS_VINDAS_USUARIO, "email/novo-usuario", "novo-usuario");
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
            String requestBaseUrl,
            EmailLogType tipo,
            String template,
            String templateId
    ) {
        PasswordResetLink link = gerarLinkDefinicaoSenha(usuario, requestIp, userAgent, requestBaseUrl);
        EmailDispatchResult result = systemEmailService.send(
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
        logFallbackLinkIfNeeded(usuario, tipo, link, result);
        return result;
    }

    private PasswordResetTokenEntity validarToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException("O link de redefinição é inválido.");
        }
        String hash = hashToken(rawToken);
        PasswordResetTokenEntity token = passwordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException("O link de redefinição é inválido ou já expirou."));

        OffsetDateTime agora = OffsetDateTime.now(APP_ZONE_ID);
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
        return montarLink(token, null);
    }

    private PasswordResetLink gerarLinkDefinicaoSenha(
            UsuarioSistemaEntity usuario,
            String requestIp,
            String userAgent,
            String requestBaseUrl
    ) {
        String token = gerarTokenSeguro();
        String tokenHash = hashToken(token);
        OffsetDateTime agora = OffsetDateTime.now(APP_ZONE_ID);

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

        return new PasswordResetLink(token, montarLink(token, requestBaseUrl), entity.getExpiresAt());
    }

    private String montarLink(String token, String requestBaseUrl) {
        String effectiveBaseUrl = sanitizeBaseUrl(requestBaseUrl);
        if (effectiveBaseUrl == null) {
            effectiveBaseUrl = sanitizeBaseUrl(baseUrl);
        }
        if (effectiveBaseUrl == null) {
            effectiveBaseUrl = "http://localhost:8083";
        }
        return effectiveBaseUrl + "/resetar-senha?token=" + token;
    }

    private String sanitizeBaseUrl(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        String normalized = candidate.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isBlank() ? null : normalized;
    }

    private void logFallbackLinkIfNeeded(
            UsuarioSistemaEntity usuario,
            EmailLogType tipo,
            PasswordResetLink link,
            EmailDispatchResult result
    ) {
        if (result.sent() || !isLocalDevelopmentLink(link.link())) {
            return;
        }
        LOGGER.warn(
                "Falha ao enviar e-mail {} para {}. Link de redefinição disponível para desenvolvimento local: {}",
                tipo,
                usuario.getEmail(),
                link.link()
        );
    }

    private boolean isLocalDevelopmentLink(String link) {
        String normalized = link == null ? "" : link.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("localhost") || normalized.contains("127.0.0.1");
    }

    private Map<String, Object> snapshotResetUsuario(UsuarioSistemaEntity usuario, OffsetDateTime redefinicao) {
        LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", usuario.getId());
        snapshot.put("nome", usuario.getNome());
        snapshot.put("username", usuario.getUsername());
        snapshot.put("email", usuario.getEmail());
        snapshot.put("senhaRedefinida", redefinicao != null);
        snapshot.put("senhaRedefinidaEm", redefinicao);
        return snapshot;
    }

    public record PasswordResetLink(String token, String link, OffsetDateTime expiresAt) {
    }
}
