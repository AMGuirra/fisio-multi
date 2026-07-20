package com.fisiogestao.auditoria.repository;

import com.fisiogestao.auditoria.AuditEventEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID>, JpaSpecificationExecutor<AuditEventEntity> {

    @EntityGraph(attributePaths = "fieldChanges")
    Optional<AuditEventEntity> findWithFieldChangesById(UUID id);

    @Query("""
            select distinct e.entidade
            from AuditEventEntity e
            where e.entidade is not null and trim(e.entidade) <> ''
            order by e.entidade
            """)
    List<String> findDistinctEntidades();

    @Query("""
            select distinct e.usuarioNome
            from AuditEventEntity e
            where e.usuarioNome is not null and trim(e.usuarioNome) <> ''
            order by e.usuarioNome
            """)
    List<String> findDistinctUsuarios();
}
