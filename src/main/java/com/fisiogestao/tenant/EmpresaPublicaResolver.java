package com.fisiogestao.tenant;

import com.fisiogestao.auth.LoginDetails;
import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.empresa.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolve a empresa destinatária de recursos públicos a partir de identificadores
 * validados no servidor: slug da clínica, token do portal do paciente ou token de
 * redefinição de senha do portal. Nunca recebe {@code empresa_id} do navegador.
 *
 * <p>As buscas por token usam SQL direto porque, antes de conhecer a empresa, o filtro
 * automático do Hibernate não permite enxergar nenhum registro de clínica.</p>
 */
@Component
@RequiredArgsConstructor
public class EmpresaPublicaResolver {

    private final EmpresaRepository empresaRepository;
    private final JdbcTemplate jdbcTemplate;

    /** Slug usado por links públicos antigos, criados antes do multiempresa (sem {@code ?clinica=}). */
    @Value("${app.tenant.slug-publico-padrao:}")
    private String slugPublicoPadrao;

    public Optional<EmpresaEntity> porSlug(String slugInformado) {
        String slug = LoginDetails.normalizarSlug(slugInformado);
        if (slug == null) {
            return Optional.empty();
        }
        return empresaRepository.findBySlug(slug);
    }

    /** Slug informado ou, na ausência dele, o slug padrão de compatibilidade (se configurado). */
    public Optional<EmpresaEntity> porSlugOuPadrao(String slugInformado) {
        if (slugInformado != null && !slugInformado.isBlank()) {
            return porSlug(slugInformado);
        }
        if (slugPublicoPadrao == null || slugPublicoPadrao.isBlank()) {
            return Optional.empty();
        }
        return porSlug(slugPublicoPadrao);
    }

    public Optional<EmpresaEntity> porTokenPortal(UUID portalToken) {
        if (portalToken == null) {
            return Optional.empty();
        }
        List<UUID> ids = jdbcTemplate.queryForList(
                "SELECT empresa_id FROM cliente WHERE portal_token = ?", UUID.class, portalToken);
        return ids.isEmpty() ? Optional.empty() : empresaRepository.findById(ids.getFirst());
    }

    public Optional<EmpresaEntity> porTokenResetPaciente(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 200) {
            return Optional.empty();
        }
        List<UUID> ids = jdbcTemplate.queryForList(
                "SELECT empresa_id FROM cliente_password_reset_token WHERE token_hash = ?",
                UUID.class,
                sha256(rawToken));
        return ids.isEmpty() ? Optional.empty() : empresaRepository.findById(ids.getFirst());
    }

    private static String sha256(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
