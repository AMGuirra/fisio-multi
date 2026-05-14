package com.fisiogestao.auditoria.repository;

import com.fisiogestao.auditoria.AuditEventEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID>, JpaSpecificationExecutor<AuditEventEntity> {

    @EntityGraph(attributePaths = "fieldChanges")
    Optional<AuditEventEntity> findWithFieldChangesById(UUID id);

    long countByUsuarioId(UUID usuarioId);
}
