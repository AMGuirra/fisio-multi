package com.fisiogestao.solicitacoes.repository;

import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import com.fisiogestao.solicitacoes.SolicitacaoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoAgendamentoRepository extends JpaRepository<SolicitacaoAgendamentoEntity, UUID> {

    List<SolicitacaoAgendamentoEntity> findByStatusOrderByCreatedAtAsc(SolicitacaoStatus status);

    List<SolicitacaoAgendamentoEntity> findAllByOrderByCreatedAtDesc();

    long countByStatus(SolicitacaoStatus status);

    List<SolicitacaoAgendamentoEntity> findByStatusAndDataHoraInicioBetweenOrderByDataHoraInicioAsc(
            SolicitacaoStatus status,
            OffsetDateTime inicio,
            OffsetDateTime fim
    );

    List<SolicitacaoAgendamentoEntity> findByDataHoraInicioBetweenOrderByDataHoraInicioAsc(
            OffsetDateTime inicio,
            OffsetDateTime fim
    );

    Optional<SolicitacaoAgendamentoEntity> findByIdAndStatus(UUID id, SolicitacaoStatus status);

    boolean existsByClienteId(UUID clienteId);

    @Query("""
           SELECT s
           FROM SolicitacaoAgendamentoEntity s
           WHERE (
               (:status IS NULL AND s.status <> :pendente)
               OR (:status IS NOT NULL AND s.status = :status)
           )
           AND (:inicio IS NULL OR s.updatedAt >= :inicio)
           AND (:fim IS NULL OR s.updatedAt < :fim)
           AND (
               :buscaTexto IS NULL
               OR LOWER(s.nomePaciente) LIKE LOWER(CONCAT('%', :buscaTexto, '%'))
               OR (:buscaTelefone IS NOT NULL AND s.telefone LIKE CONCAT('%', :buscaTelefone, '%'))
           )
           ORDER BY s.updatedAt DESC
           """)
    List<SolicitacaoAgendamentoEntity> filtrarHistorico(
            @Param("pendente") SolicitacaoStatus pendente,
            @Param("status") SolicitacaoStatus status,
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim,
            @Param("buscaTexto") String buscaTexto,
            @Param("buscaTelefone") String buscaTelefone
    );
}
