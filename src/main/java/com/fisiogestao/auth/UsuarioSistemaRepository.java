package com.fisiogestao.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Usuários não usam o filtro automático de tenant porque o SUPER_ADMIN não possui empresa.
 * Por isso toda consulta aqui recebe a empresa explicitamente (ou exige {@code empresa_id IS NULL}).
 * Não exponha buscas globais por username/e-mail fora do fluxo de login.
 */
public interface UsuarioSistemaRepository extends JpaRepository<UsuarioSistemaEntity, UUID> {

    Optional<UsuarioSistemaEntity> findByEmpresaIdAndUsername(UUID empresaId, String username);

    Optional<UsuarioSistemaEntity> findByEmpresaIdIsNullAndUsername(String username);

    Optional<UsuarioSistemaEntity> findByEmpresaIdAndEmailIgnoreCase(UUID empresaId, String email);

    Optional<UsuarioSistemaEntity> findByEmpresaIdIsNullAndEmailIgnoreCase(String email);

    Optional<UsuarioSistemaEntity> findByIdAndEmpresaId(UUID id, UUID empresaId);

    Optional<UsuarioSistemaEntity> findByIdAndEmpresaIdIsNull(UUID id);

    /** Apenas para o login sem código da clínica: candidatos são checados pela senha em seguida. */
    List<UsuarioSistemaEntity> findByUsernameAndEmpresaIdIsNotNull(String username);

    List<UsuarioSistemaEntity> findByEmailIgnoreCaseAndEmpresaIdIsNotNull(String email);

    List<UsuarioSistemaEntity> findByEmpresaIdOrderByNomeAsc(UUID empresaId);

    boolean existsByEmpresaIdAndUsername(UUID empresaId, String username);

    boolean existsByEmpresaIdAndUsernameAndIdNot(UUID empresaId, String username, UUID id);

    boolean existsByEmpresaIdAndEmailIgnoreCase(UUID empresaId, String email);

    boolean existsByEmpresaIdAndEmailIgnoreCaseAndIdNot(UUID empresaId, String email, UUID id);

    boolean existsByEmpresaIdIsNullAndUsername(String username);

    long countByEmpresaIdAndRole(UUID empresaId, UserRole role);

    long countByEmpresaId(UUID empresaId);

    long countByEmpresaIdIsNotNull();
}
