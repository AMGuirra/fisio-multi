package com.fisiogestao.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioSistemaRepository extends JpaRepository<UsuarioSistemaEntity, UUID> {

    Optional<UsuarioSistemaEntity> findByUsername(String username);

    Optional<UsuarioSistemaEntity> findByEmailIgnoreCase(String email);

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameAndIdNot(String username, UUID id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    long countByRole(UserRole role);
}
