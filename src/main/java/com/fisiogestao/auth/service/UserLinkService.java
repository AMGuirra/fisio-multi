package com.fisiogestao.auth.service;

import com.fisiogestao.auditoria.repository.AuditEventRepository;
import com.fisiogestao.auth.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserLinkService {

    private final AuditEventRepository auditEventRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public List<UserLink> findLinks(UUID usuarioId) {
        List<UserLink> links = new ArrayList<>();

        long auditCount = auditEventRepository.countByUsuarioId(usuarioId);
        if (auditCount > 0) {
            links.add(new UserLink(
                    "Registros de Auditoria",
                    auditCount,
                    "Ações realizadas por este usuário que estão registradas no histórico do sistema.",
                    "shield-check"
            ));
        }

        long tokenCount = passwordResetTokenRepository.countByUsuarioId(usuarioId);
        if (tokenCount > 0) {
            links.add(new UserLink(
                    "Tokens de Segurança",
                    tokenCount,
                    "Tokens de redefinição de senha ou convites de acesso vinculados a este usuário.",
                    "key"
            ));
        }

        return links;
    }

    public record UserLink(String tipo, long quantidade, String descricao, String icone) {}
}
