package com.fisiogestao.auth;

import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.repository.AuditEventRepository;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.email.repository.EmailLogRepository;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Catálogo de usuários do sistema. Todas as operações atuam apenas na empresa do
 * contexto atual ({@link TenantContext}); para o SUPER_ADMIN, o escopo é o dos usuários
 * sem empresa. Não há como ler ou alterar usuários de outra clínica por aqui.
 */
@Component
@RequiredArgsConstructor
public class InMemoryUserCatalog {

    private final PasswordEncoder passwordEncoder;
    private final UsuarioSistemaRepository repository;
    private final AuditService auditService;
    private final AuditEventRepository auditEventRepository;
    private final EmailLogRepository emailLogRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public List<SystemUserView> listar() {
        UUID empresaId = TenantContext.empresaIdOuNull();
        if (empresaId == null) {
            return List.of();
        }
        return repository.findByEmpresaIdOrderByNomeAsc(empresaId).stream()
                .map(this::toView)
                .toList();
    }

    /** Uso exclusivo da área Gestor: dados administrativos da equipe de uma empresa. */
    public List<SystemUserView> listarPorEmpresa(UUID empresaId) {
        return repository.findByEmpresaIdOrderByNomeAsc(empresaId).stream()
                .map(this::toView)
                .toList();
    }

    public List<UserRole> listarPerfis() {
        return UserRole.papeisDeClinica();
    }

    public Optional<SystemUserView> buscarPorId(UUID id) {
        return buscarEntidadeNoEscopo(id).map(this::toView);
    }

    public Optional<SystemUserCredentials> buscarCredenciais(String username) {
        return findByUsernameOrEmail(username)
                .map(user -> new SystemUserCredentials(user.getUsername(), user.getPasswordHash(), user.getRole()));
    }

    public Optional<String> buscarNomeExibicao(String username) {
        return findByUsernameOrEmail(username)
                .map(UsuarioSistemaEntity::getNome)
                .map(String::trim)
                .filter(nome -> !nome.isBlank());
    }

    public Optional<SystemUserSessionView> buscarSessao(String username) {
        return findByUsernameOrEmail(username)
                .map(user -> new SystemUserSessionView(
                        user.getUsername(),
                        user.getNome(),
                        user.getRole(),
                        UserThemePreference.fallback(user.getTemaInterface())
                ));
    }

    @Transactional
    public SystemUserView criar(String nome, String username, String email, String password, UserRole role) {
        UUID empresaId = TenantContext.requireEmpresaId();
        return toView(criarNaEmpresa(empresaId, nome, username, email, password, role, "Cadastro manual de usuário."));
    }

    /**
     * Cria um usuário na empresa informada. Usado pelo cadastro público de clínica (primeiro
     * ADMIN_CLINICA) e pelo bootstrap; o chamador é responsável por já estar no escopo da empresa.
     */
    @Transactional
    public UsuarioSistemaEntity criarNaEmpresa(
            UUID empresaId,
            String nome,
            String username,
            String email,
            String password,
            UserRole role,
            String observacaoAuditoria
    ) {
        if (empresaId == null) {
            throw new BusinessException("Usuários de clínica precisam estar vinculados a uma empresa.");
        }
        String normalizedNome = normalizeNome(nome);
        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = normalizeEmail(email);
        String normalizedPassword = normalizePassword(password, true);
        UserRole normalizedRole = normalizeRole(role);

        ensureUsernameDisponivel(empresaId, normalizedUsername, null);
        ensureEmailDisponivel(empresaId, normalizedEmail, null);

        UsuarioSistemaEntity user = UsuarioSistemaEntity.builder()
                .nome(normalizedNome)
                .username(normalizedUsername)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(normalizedPassword))
                .role(normalizedRole)
                .empresaId(empresaId)
                .build();
        UsuarioSistemaEntity salvo = repository.save(user);
        TenantContext.executarComo(empresaId, () -> auditService.recordCreate(
                AuditModule.USUARIOS,
                "usuario_sistema",
                salvo,
                null,
                observacaoAuditoria,
                AuditEventOrigin.MANUAL
        ));
        return salvo;
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
        UUID empresaId = TenantContext.requireEmpresaId();
        UsuarioSistemaEntity existing = repository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
        var snapshotAnterior = auditService.snapshotOf(existing);

        String normalizedNome = normalizeNome(nome);
        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = normalizeEmail(email);
        UserRole normalizedRole = normalizeRole(role);
        ensureUsernameDisponivel(empresaId, normalizedUsername, id);
        ensureEmailDisponivel(empresaId, normalizedEmail, id);

        if (existing.getRole() == UserRole.ADMIN_CLINICA
                && normalizedRole != UserRole.ADMIN_CLINICA
                && repository.countByEmpresaIdAndRole(empresaId, UserRole.ADMIN_CLINICA) == 1) {
            throw new BusinessException("Não é possível remover o papel de administrador do único admin da clínica.");
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
        UUID empresaId = TenantContext.requireEmpresaId();
        UsuarioSistemaEntity existing = repository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));

        String normalizedLoggedUsername = normalizeUsernameForLookup(loggedUsername);
        if (normalizedLoggedUsername != null && existing.getUsername().equals(normalizedLoggedUsername)) {
            throw new BusinessException("Você não pode excluir o usuário que está logado.");
        }

        if (existing.getRole() == UserRole.ADMIN_CLINICA
                && repository.countByEmpresaIdAndRole(empresaId, UserRole.ADMIN_CLINICA) == 1) {
            throw new BusinessException("Não é possível excluir o único administrador da clínica.");
        }

        // A exclusão é definitiva: remove tokens, e-mails e toda a auditoria em
        // que o usuário aparece como autor ou como registro afetado. Os campos
        // de auditoria filhos são removidos em cascata pela própria entidade.
        passwordResetTokenRepository.deleteByUsuarioId(id);
        emailLogRepository.deleteByEmpresaIdAndEntidadeTipoAndEntidadeId(empresaId, "usuario_sistema", id);
        auditEventRepository.deleteAll(auditEventRepository
                .findDoUsuarioNaEmpresa(empresaId, id, "usuario_sistema", id.toString()));
        repository.delete(existing);
    }

    public Optional<UserRole> buscarRole(String username) {
        return findByUsernameOrEmail(username).map(UsuarioSistemaEntity::getRole);
    }

    public Optional<UserThemePreference> buscarTema(String username) {
        return findByUsernameOrEmail(username)
                .map(UsuarioSistemaEntity::getTemaInterface)
                .map(UserThemePreference::fallback);
    }

    @Transactional
    public void atualizarTema(String username, UserThemePreference tema) {
        UsuarioSistemaEntity existing = findByUsernameOrEmail(username)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
        UserThemePreference resolvedTheme = UserThemePreference.fallback(tema);

        if (resolvedTheme == existing.getTemaInterface()) {
            return;
        }

        existing.setTemaInterface(resolvedTheme);
        repository.save(existing);
    }

    private Optional<UsuarioSistemaEntity> buscarEntidadeNoEscopo(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        UUID empresaId = TenantContext.empresaIdOuNull();
        return empresaId == null
                ? repository.findByIdAndEmpresaIdIsNull(id)
                : repository.findByIdAndEmpresaId(id, empresaId);
    }

    private void ensureUsernameDisponivel(UUID empresaId, String username, UUID ignoredId) {
        boolean usernameEmUso = ignoredId == null
                ? repository.existsByEmpresaIdAndUsername(empresaId, username)
                : repository.existsByEmpresaIdAndUsernameAndIdNot(empresaId, username, ignoredId);
        if (usernameEmUso) {
            throw new BusinessException("Já existe um usuário cadastrado com esse nome.");
        }
    }

    private void ensureEmailDisponivel(UUID empresaId, String email, UUID ignoredId) {
        boolean emailEmUso = ignoredId == null
                ? repository.existsByEmpresaIdAndEmailIgnoreCase(empresaId, email)
                : repository.existsByEmpresaIdAndEmailIgnoreCaseAndIdNot(empresaId, email, ignoredId);
        if (emailEmUso) {
            throw new BusinessException("Já existe um usuário cadastrado com esse e-mail.");
        }
    }

    private UserRole normalizeRole(UserRole role) {
        if (role == null) {
            throw new BusinessException("Selecione o tipo de usuário.");
        }
        if (role.isSuperAdmin()) {
            throw new BusinessException("Tipo de usuário inválido para uma clínica.");
        }
        return role;
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

    static String normalizeUsernameForLookup(String username) {
        if (username == null) {
            return null;
        }
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    /** Busca restrita ao escopo atual: empresa do contexto, ou usuários sem empresa (superadmin). */
    private Optional<UsuarioSistemaEntity> findByUsernameOrEmail(String usernameOrEmail) {
        String normalized = normalizeUsernameForLookup(usernameOrEmail);
        if (normalized == null) {
            return Optional.empty();
        }
        UUID empresaId = TenantContext.empresaIdOuNull();
        if (normalized.contains("@")) {
            return empresaId == null
                    ? repository.findByEmpresaIdIsNullAndEmailIgnoreCase(normalized)
                    : repository.findByEmpresaIdAndEmailIgnoreCase(empresaId, normalized);
        }
        return empresaId == null
                ? repository.findByEmpresaIdIsNullAndUsername(normalized)
                : repository.findByEmpresaIdAndUsername(empresaId, normalized);
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

    public record SystemUserSessionView(String username, String nome, UserRole role, UserThemePreference temaInterface) {
    }
}
