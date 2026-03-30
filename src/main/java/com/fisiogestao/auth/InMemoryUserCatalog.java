package com.fisiogestao.auth;

import com.fisiogestao.shared.web.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InMemoryUserCatalog {

    private static final int MIN_PASSWORD_LENGTH = 4;

    private final PasswordEncoder passwordEncoder;
    private final Map<UUID, SystemUser> users = new LinkedHashMap<>();

    @PostConstruct
    void seedDefaults() {
        if (!users.isEmpty()) {
            return;
        }
        addSeed("admin", "admin123", UserRole.ADMIN);
        addSeed("adm", "adm123", UserRole.ADMINISTRATIVO);
        addSeed("fisio", "fisio123", UserRole.FISIOTERAPEUTA);
    }

    public synchronized List<SystemUserView> listar() {
        return users.values().stream()
                .map(this::toView)
                .toList();
    }

    public synchronized List<UserRole> listarPerfis() {
        return List.of(UserRole.values());
    }

    public synchronized Optional<SystemUserView> buscarPorId(UUID id) {
        return Optional.ofNullable(users.get(id))
                .map(this::toView);
    }

    public synchronized Optional<SystemUserCredentials> buscarCredenciais(String username) {
        String normalized = normalizeUsernameForLookup(username);
        if (normalized == null) {
            return Optional.empty();
        }
        return users.values().stream()
                .filter(user -> user.username().equals(normalized))
                .findFirst()
                .map(user -> new SystemUserCredentials(user.username(), user.passwordHash(), user.role()));
    }

    public synchronized SystemUserView criar(String username, String password, UserRole role) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedPassword = normalizePassword(password, true);
        UserRole normalizedRole = normalizeRole(role);

        ensureUsernameDisponivel(normalizedUsername, null);

        SystemUser user = new SystemUser(
                UUID.randomUUID(),
                normalizedUsername,
                passwordEncoder.encode(normalizedPassword),
                normalizedRole
        );
        users.put(user.id(), user);
        return toView(user);
    }

    public synchronized SystemUserView atualizar(
            UUID id,
            String username,
            String password,
            UserRole role,
            String loggedUsername
    ) {
        SystemUser existing = users.get(id);
        if (existing == null) {
            throw new BusinessException("Usuário não encontrado.");
        }

        String normalizedUsername = normalizeUsername(username);
        UserRole normalizedRole = normalizeRole(role);
        ensureUsernameDisponivel(normalizedUsername, id);

        if (existing.role() == UserRole.ADMIN
                && normalizedRole != UserRole.ADMIN
                && contarAdminsInterno() == 1) {
            throw new BusinessException("Não é possível remover o papel de administrador do único admin do sistema.");
        }

        String normalizedLoggedUsername = normalizeUsernameForLookup(loggedUsername);
        boolean editingCurrentSession = normalizedLoggedUsername != null
                && existing.username().equals(normalizedLoggedUsername);
        if (editingCurrentSession && !existing.username().equals(normalizedUsername)) {
            throw new BusinessException("Não é permitido alterar o usuário da sessão atual.");
        }
        if (editingCurrentSession && existing.role() != normalizedRole) {
            throw new BusinessException("Não é permitido alterar o tipo do usuário da sessão atual.");
        }

        String updatedPasswordHash = existing.passwordHash();
        String normalizedPassword = normalizePassword(password, false);
        if (normalizedPassword != null) {
            updatedPasswordHash = passwordEncoder.encode(normalizedPassword);
        }

        SystemUser updated = new SystemUser(
                existing.id(),
                normalizedUsername,
                updatedPasswordHash,
                normalizedRole
        );
        users.put(updated.id(), updated);
        return toView(updated);
    }

    public synchronized void excluir(UUID id, String loggedUsername) {
        SystemUser existing = users.get(id);
        if (existing == null) {
            throw new BusinessException("Usuário não encontrado.");
        }

        String normalizedLoggedUsername = normalizeUsernameForLookup(loggedUsername);
        if (normalizedLoggedUsername != null && existing.username().equals(normalizedLoggedUsername)) {
            throw new BusinessException("Você não pode excluir o usuário que está logado.");
        }

        if (existing.role() == UserRole.ADMIN && contarAdminsInterno() == 1) {
            throw new BusinessException("Não é possível excluir o único administrador do sistema.");
        }

        users.remove(id);
    }

    private void addSeed(String username, String rawPassword, UserRole role) {
        SystemUser user = new SystemUser(
                UUID.randomUUID(),
                username,
                passwordEncoder.encode(rawPassword),
                role
        );
        users.put(user.id(), user);
    }

    private void ensureUsernameDisponivel(String username, UUID ignoredId) {
        boolean usernameEmUso = users.values().stream()
                .anyMatch(user -> user.username().equals(username) && !Objects.equals(user.id(), ignoredId));
        if (usernameEmUso) {
            throw new BusinessException("Já existe um usuário cadastrado com esse nome.");
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

    private long contarAdminsInterno() {
        return users.values().stream()
                .filter(user -> user.role() == UserRole.ADMIN)
                .count();
    }

    private SystemUserView toView(SystemUser user) {
        return new SystemUserView(user.id(), user.username(), user.role(), user.role().getLabel());
    }

    private record SystemUser(UUID id, String username, String passwordHash, UserRole role) {
    }

    public record SystemUserCredentials(String username, String passwordHash, UserRole role) {
    }

    public record SystemUserView(UUID id, String username, UserRole role, String label) {
    }

    public enum UserRole {
        ADMIN("Administrador"),
        ADMINISTRATIVO("Administrativo"),
        FISIOTERAPEUTA("Fisioterapeuta");

        private final String label;

        UserRole(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
