package com.fisiogestao.auth;

import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InMemoryUserCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryUserCatalog.class);

    private final PasswordEncoder passwordEncoder;
    private final UsuarioSistemaRepository repository;
    private final AuditService auditService;
    private final BootstrapAdminProperties bootstrapAdminProperties;

    @PostConstruct
    void seedDefaults() {
        if (repository.count() > 0) {
            return;
        }
        if (!bootstrapAdminProperties.isEnabled()) {
            LOGGER.warn("""
                    Nenhum usuário foi encontrado e o bootstrap do administrador inicial está desabilitado.
                    Configure APP_BOOTSTRAP_ADMIN_ENABLED=true com APP_BOOTSTRAP_ADMIN_USERNAME,
                    APP_BOOTSTRAP_ADMIN_EMAIL e APP_BOOTSTRAP_ADMIN_PASSWORD para criar o primeiro acesso.
                    """.trim());
            return;
        }

        try {
            UsuarioSistemaEntity admin = UsuarioSistemaEntity.builder()
                    .nome(normalizeNome(bootstrapAdminProperties.getNome()))
                    .username(normalizeUsername(bootstrapAdminProperties.getUsername()))
                    .email(normalizeEmail(bootstrapAdminProperties.getEmail()))
                    .passwordHash(passwordEncoder.encode(PasswordPolicy.normalize(bootstrapAdminProperties.getPassword(), true)))
                    .role(UserRole.ADMIN)
                    .build();
            repository.save(admin);
            LOGGER.info("Administrador inicial criado via bootstrap seguro.");
        } catch (BusinessException ex) {
            throw new IllegalStateException("Não foi possível criar o administrador inicial: " + ex.getMessage(), ex);
        }
    }

    public List<SystemUserView> listar() {
        return repository.findAll().stream()
                .map(this::toView)
                .toList();
    }

    public List<UserRole> listarPerfis() {
        return List.of(UserRole.values());
    }

    public Optional<SystemUserView> buscarPorId(UUID id) {
        return repository.findById(id)
                .map(this::toView);
    }

    public Optional<SystemUserCredentials> buscarCredenciais(String username) {
        String normalized = normalizeUsernameForLookup(username);
        if (normalized == null) {
            return Optional.empty();
        }
        return repository.findByUsername(normalized)
                .map(user -> new SystemUserCredentials(user.getUsername(), user.getPasswordHash(), user.getRole()));
    }

    public Optional<String> buscarNomeExibicao(String username) {
        String normalized = normalizeUsernameForLookup(username);
        if (normalized == null) {
            return Optional.empty();
        }
        return repository.findByUsername(normalized)
                .map(UsuarioSistemaEntity::getNome)
                .map(String::trim)
                .filter(nome -> !nome.isBlank());
    }

    @Transactional
    public SystemUserView criar(String nome, String username, String email, String password, UserRole role) {
        String normalizedNome = normalizeNome(nome);
        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = normalizeEmail(email);
        String normalizedPassword = normalizePassword(password, true);
        UserRole normalizedRole = normalizeRole(role);

        ensureUsernameDisponivel(normalizedUsername, null);
        ensureEmailDisponivel(normalizedEmail, null);

        UsuarioSistemaEntity user = UsuarioSistemaEntity.builder()
                .nome(normalizedNome)
                .username(normalizedUsername)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(normalizedPassword))
                .role(normalizedRole)
                .build();
        UsuarioSistemaEntity salvo = repository.save(user);
        auditService.recordCreate(
                AuditModule.USUARIOS,
                "usuario_sistema",
                salvo,
                null,
                "Cadastro manual de usuário.",
                AuditEventOrigin.MANUAL
        );
        return toView(salvo);
    }

    @Transactional
    public SystemUserView atualizar(
            UUID id,
            String nome,
            String username,
            String email,
            String password,
            UserRole role,
            String loggedUsername
    ) {
        UsuarioSistemaEntity existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
        var snapshotAnterior = auditService.snapshotOf(existing);

        String normalizedNome = normalizeNome(nome);
        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = normalizeEmail(email);
        UserRole normalizedRole = normalizeRole(role);
        ensureUsernameDisponivel(normalizedUsername, id);
        ensureEmailDisponivel(normalizedEmail, id);

        if (existing.getRole() == UserRole.ADMIN
                && normalizedRole != UserRole.ADMIN
                && repository.countByRole(UserRole.ADMIN) == 1) {
            throw new BusinessException("Não é possível remover o papel de administrador do único admin do sistema.");
        }

        String normalizedLoggedUsername = normalizeUsernameForLookup(loggedUsername);
        boolean editingCurrentSession = normalizedLoggedUsername != null
                && existing.getUsername().equals(normalizedLoggedUsername);
        if (editingCurrentSession && !existing.getUsername().equals(normalizedUsername)) {
            throw new BusinessException("Não é permitido alterar o usuário da sessão atual.");
        }
        if (editingCurrentSession && existing.getRole() != normalizedRole) {
            throw new BusinessException("Não é permitido alterar o tipo do usuário da sessão atual.");
        }

        existing.setNome(normalizedNome);
        existing.setUsername(normalizedUsername);
        existing.setEmail(normalizedEmail);
        existing.setRole(normalizedRole);

        String normalizedPassword = normalizePassword(password, false);
        if (normalizedPassword != null) {
            existing.setPasswordHash(passwordEncoder.encode(normalizedPassword));
        }

        UsuarioSistemaEntity salvo = repository.save(existing);
        auditService.recordManual(
                AuditModule.USUARIOS,
                "usuario_sistema",
                salvo.getId().toString(),
                salvo.getNome(),
                AuditActionType.UPDATE,
                snapshotAnterior,
                auditService.snapshotOf(salvo),
                null,
                normalizedPassword != null
                        ? "Atualização de usuário com alteração de credencial."
                        : "Atualização manual de usuário.",
                AuditEventOrigin.MANUAL
        );
        return toView(salvo);
    }

    @Transactional
    public void excluir(UUID id, String loggedUsername) {
        UsuarioSistemaEntity existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
        var snapshotAnterior = auditService.snapshotOf(existing);

        String normalizedLoggedUsername = normalizeUsernameForLookup(loggedUsername);
        if (normalizedLoggedUsername != null && existing.getUsername().equals(normalizedLoggedUsername)) {
            throw new BusinessException("Você não pode excluir o usuário que está logado.");
        }

        if (existing.getRole() == UserRole.ADMIN && repository.countByRole(UserRole.ADMIN) == 1) {
            throw new BusinessException("Não é possível excluir o único administrador do sistema.");
        }

        auditService.recordManual(
                AuditModule.USUARIOS,
                "usuario_sistema",
                existing.getId().toString(),
                existing.getNome(),
                AuditActionType.DELETE_PHYSICAL,
                snapshotAnterior,
                null,
                null,
                "Exclusão física de usuário.",
                AuditEventOrigin.MANUAL
        );
        repository.delete(existing);
    }

    private void ensureUsernameDisponivel(String username, UUID ignoredId) {
        boolean usernameEmUso = ignoredId == null
                ? repository.existsByUsername(username)
                : repository.existsByUsernameAndIdNot(username, ignoredId);
        if (usernameEmUso) {
            throw new BusinessException("Já existe um usuário cadastrado com esse nome.");
        }
    }

    private void ensureEmailDisponivel(String email, UUID ignoredId) {
        boolean emailEmUso = ignoredId == null
                ? repository.existsByEmailIgnoreCase(email)
                : repository.existsByEmailIgnoreCaseAndIdNot(email, ignoredId);
        if (emailEmUso) {
            throw new BusinessException("Já existe um usuário cadastrado com esse e-mail.");
        }
    }

    private UserRole normalizeRole(UserRole role) {
        if (role == null) {
            throw new BusinessException("Selecione o tipo de usuário.");
        }
        return role;
    }

    public Optional<UserRole> buscarRole(String username) {
        String normalized = normalizeUsernameForLookup(username);
        if (normalized == null) {
            return Optional.empty();
        }
        return repository.findByUsername(normalized)
                .map(UsuarioSistemaEntity::getRole);
    }

    public Optional<UserThemePreference> buscarTema(String username) {
        String normalized = normalizeUsernameForLookup(username);
        if (normalized == null) {
            return Optional.empty();
        }
        return repository.findByUsername(normalized)
                .map(UsuarioSistemaEntity::getTemaInterface)
                .map(UserThemePreference::fallback);
    }

    @Transactional
    public void atualizarTema(String username, UserThemePreference tema) {
        String normalized = normalizeUsernameForLookup(username);
        if (normalized == null) {
            throw new BusinessException("Usuário não encontrado.");
        }

        UsuarioSistemaEntity existing = repository.findByUsername(normalized)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
        UserThemePreference resolvedTheme = UserThemePreference.fallback(tema);

        if (resolvedTheme == existing.getTemaInterface()) {
            return;
        }

        existing.setTemaInterface(resolvedTheme);
        repository.save(existing);
    }

    private String normalizeNome(String nome) {
        if (nome == null) {
            throw new BusinessException("Informe o nome.");
        }
        String normalized = nome.trim();
        if (normalized.isBlank()) {
            throw new BusinessException("Informe o nome.");
        }
        return normalized;
    }

    private String normalizeUsername(String username) {
        String normalized = normalizeUsernameForLookup(username);
        if (normalized == null) {
            throw new BusinessException("Informe o nome de usuário.");
        }
        return normalized;
    }

    private String normalizeUsernameForLookup(String username) {
        if (username == null) {
            return null;
        }
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new BusinessException("Informe o e-mail.");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException("Informe o e-mail.");
        }
        if (!normalized.contains("@") || normalized.startsWith("@") || normalized.endsWith("@")) {
            throw new BusinessException("Informe um e-mail válido.");
        }
        return normalized;
    }

    private String normalizePassword(String password, boolean required) {
        return PasswordPolicy.normalize(password, required);
    }

    private SystemUserView toView(UsuarioSistemaEntity user) {
        return new SystemUserView(user.getId(), user.getNome(), user.getUsername(), user.getEmail(), user.getRole(), user.getRole().getLabel());
    }

    public record SystemUserCredentials(String username, String passwordHash, UserRole role) {
    }

    public record SystemUserView(UUID id, String nome, String username, String email, UserRole role, String label) {
    }
}
