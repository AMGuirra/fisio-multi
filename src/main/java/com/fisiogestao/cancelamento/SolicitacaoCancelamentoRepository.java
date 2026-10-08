package com.fisiogestao.cancelamento;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoCancelamentoRepository extends JpaRepository<SolicitacaoCancelamentoEntity, UUID> {

    @EntityGraph(attributePaths = "historico")
    Optional<SolicitacaoCancelamentoEntity> findWithHistoricoById(UUID id);

    long countByStatusIn(Collection<StatusSolicitacaoCancelamento> status);

    /** Solicitações da própria clínica (exibidas em Config. Clínica). */
    List<SolicitacaoCancelamentoEntity> findTop5ByEmpresaIdOrderByCreatedAtDesc(UUID empresaId);

    boolean existsByEmpresaIdAndStatusIn(UUID empresaId, Collection<StatusSolicitacaoCancelamento> status);

    @Query("""
            select s from SolicitacaoCancelamentoEntity s
            where (:status is null or s.status = :status)
              and (:empresaId is null or s.empresaId = :empresaId)
              and (:responsavel = '%'
                   or lower(s.nomeSolicitante) like :responsavel
                   or lower(s.emailContato) like :responsavel)
              and s.createdAt >= :inicio
              and s.createdAt < :fim
            """)
    Page<SolicitacaoCancelamentoEntity> buscar(
            @Param("status") StatusSolicitacaoCancelamento status,
            @Param("empresaId") UUID empresaId,
            @Param("responsavel") String responsavel,
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim,
            Pageable pageable
    );
}
