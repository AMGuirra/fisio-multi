package com.fisiogestao.auditoria.repository;

import com.fisiogestao.auditoria.AuditEventEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Auditoria não usa o filtro automático de tenant (eventos da plataforma têm empresa nula),
 * então toda consulta recebe a empresa explicitamente. {@code empresaId = null} significa
 * "eventos da plataforma", nunca "todas as empresas".
 */
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID>, JpaSpecificationExecutor<AuditEventEntity> {

    @EntityGraph(attributePaths = "fieldChanges")
    @Query("""
            select e from AuditEventEntity e
            where e.id = :id
              and ((:empresaId is null and e.empresaId is null) or e.empresaId = :empresaId)
            """)
    Optional<AuditEventEntity> findDetalheNoEscopo(@Param("id") UUID id, @Param("empresaId") UUID empresaId);

    @Query("""
            select e from AuditEventEntity e
            where e.empresaId = :empresaId
              and (e.usuarioId = :usuarioId or (e.entidade = :entidade and e.entidadeId = :entidadeId))
            """)
    List<AuditEventEntity> findDoUsuarioNaEmpresa(
            @Param("empresaId") UUID empresaId,
            @Param("usuarioId") UUID usuarioId,
            @Param("entidade") String entidade,
            @Param("entidadeId") String entidadeId
    );

    @Query("""
            select distinct e.entidade
            from AuditEventEntity e
            where e.entidade is not null and trim(e.entidade) <> ''
              and ((:empresaId is null and e.empresaId is null) or e.empresaId = :empresaId)
            order by e.entidade
            """)
    List<String> findDistinctEntidades(@Param("empresaId") UUID empresaId);

    @Query("""
            select distinct e.usuarioNome
            from AuditEventEntity e
            where e.usuarioNome is not null and trim(e.usuarioNome) <> ''
              and ((:empresaId is null and e.empresaId is null) or e.empresaId = :empresaId)
            order by e.usuarioNome
            """)
    List<String> findDistinctUsuarios(@Param("empresaId") UUID empresaId);

    long countByEmpresaId(UUID empresaId);
}
