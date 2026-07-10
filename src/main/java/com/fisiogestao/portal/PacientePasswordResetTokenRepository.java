package com.fisiogestao.portal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PacientePasswordResetTokenRepository extends JpaRepository<PacientePasswordResetTokenEntity, UUID> {

    Optional<PacientePasswordResetTokenEntity> findByTokenHash(String tokenHash);

    List<PacientePasswordResetTokenEntity> findByClienteIdAndUsedAtIsNull(UUID clienteId);

    void deleteByClienteId(UUID clienteId);
}
