package com.fisiogestao.auth;

import com.fisiogestao.shared.web.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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

    private static final int MIN_PASSWORD_LENGTH = 4;

    private final PasswordEncoder passwordEncoder;
    private final UsuarioSistemaRepository repository;

    @PostConstruct
    void seedDefaults() {
        if (repository.count() > 0) {
            return;
        }
        UsuarioSistemaEntity admin = UsuarioSistemaEntity.builder()
                .username("admin")
                .email("admin@fisiogestao.local")
                .passwordHash(passwordEncoder.encode("admin"))
                .role(UserRole.ADMIN)
                .build();
        repository.save(admin);
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

    @Transactional
    public SystemUserView criar(String username, String email, String password, UserRole role) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = normalizeEmail(email);
        String normalizedPassword = normalizePassword(password, true);
        UserRole normalizedRole = normalizeRole(role);

        ensureUsernameDisponivel(normalizedUsername, null);
        ensureEmailDisponivel(normalizedEmail, null);

        UsuarioSistemaEntity user = UsuarioSistemaEntity.builder()
                .username(normalizedUsername)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(normalizedPassword))
                .role(normalizedRole)
                .build();
        return toView(repository.save(user));
    }

    @Transactional
    public SystemUserView atualizar(
            UUID id,
            String username,
            String email,
            String password,
            UserRole role,
            String loggedUsername
    ) {
        UsuarioSistemaEntity existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));

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

        existing.setUsername(normalizedUsername);
        existing.setEmail(normalizedEmail);
        existing.setRole(normalizedRole);

        String normalizedPassword = normalizePassword(password, false);
        if (normalizedPassword != null) {
            existing.setPasswordHash(passwordEncoder.encode(normalizedPassword));
        }

        return toView(repository.save(existing));
    }

    @Transactional
    public void excluir(UUID id, String loggedUsername) {
        UsuarioSistemaEntity existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));

        String normalizedLoggedUsername = normalizeUsernameForLookup(loggedUsername);
        if (normalizedLoggedUsername != null && existing.getUsername().equals(normalizedLoggedUsername)) {
            throw new BusinessException("Você não pode excluir o usuário que está logado.");
        }

        if (existing.getRole() == UserRole.ADMIN && repository.countByRole(UserRole.ADMIN) == 1) {
            throw new BusinessException("Não é possível excluir o único administrador do sistema.");
        }

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
        if (password == null) {
            if (required) {
                throw new BusinessException("Informe a senha.");
            }
            return null;
        }

        String normalized = password.trim();
        if (normalized.isBlank()) {
            if (required) {
                throw new BusinessException("Informe a senha.");
            }
            return null;
        }
        if (normalized.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException("A senha deve ter pelo menos 4 caracteres.");
        }
        return normalized;
    }

    private SystemUserView toView(UsuarioSistemaEntity user) {
        return new SystemUserView(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.getRole().getLabel());
    }

    public record SystemUserCredentials(String username, String passwordHash, UserRole role) {
    }

    public record SystemUserView(UUID id, String username, String email, UserRole role, String label) {
    }
}
