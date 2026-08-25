package com.fisiogestao.auth;

import com.fisiogestao.auditoria.repository.AuditEventRepository;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.email.repository.EmailLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InMemoryUserCatalogDeletionTest {

    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UsuarioSistemaRepository repository;
    @Mock private AuditService auditService;
    @Mock private AuditEventRepository auditEventRepository;
    @Mock private EmailLogRepository emailLogRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private BootstrapAdminProperties bootstrapAdminProperties;

    @InjectMocks private InMemoryUserCatalog userCatalog;

    @Test
    void excluirRemoveTodosOsRegistrosRelacionadosAoUsuario() {
        UUID userId = UUID.randomUUID();
        UsuarioSistemaEntity usuario = UsuarioSistemaEntity.builder()
                .nome("Carla")
                .username("carla")
                .passwordHash("hash")
                .role(UserRole.RECEPCAO)
                .build();
        usuario.setId(userId);
        when(repository.findById(userId)).thenReturn(Optional.of(usuario));
        when(auditEventRepository.findByUsuarioIdOrEntidadeAndEntidadeId(
                userId, "usuario_sistema", userId.toString())).thenReturn(List.of());

        userCatalog.excluir(userId, "administrador");

        verify(passwordResetTokenRepository).deleteByUsuarioId(userId);
        verify(emailLogRepository).deleteByEntidadeTipoAndEntidadeId("usuario_sistema", userId);
        verify(auditEventRepository).deleteAll(List.of());
        verify(repository).delete(usuario);
    }
}
