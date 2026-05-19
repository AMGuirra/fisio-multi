package com.fisiogestao.solicitacoes.repository;

import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import com.fisiogestao.solicitacoes.SolicitacaoStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoAgendamentoRepository extends JpaRepository<SolicitacaoAgendamentoEntity, UUID> {

    @EntityGraph(attributePaths = {"cliente"})
    List<SolicitacaoAgendamentoEntity> findByStatusOrderByCreatedAtAsc(SolicitacaoStatus status);

    List<SolicitacaoAgendamentoEntity> findAllByOrderByCreatedAtDesc();

    long countByStatus(SolicitacaoStatus status);

    /** Conta solicitações de um status cujo createdAt está no intervalo (ex.: pendentes abertas no mês). */
    long countByStatusAndCreatedAtBetween(SolicitacaoStatus status, OffsetDateTime inicio, OffsetDateTime fim);

    /** Conta solicitações de um status cujo updatedAt está no intervalo (ex.: aprovadas/rejeitadas no mês). */
    long countByStatusAndUpdatedAtBetween(SolicitacaoStatus status, OffsetDateTime inicio, OffsetDateTime fim);

    /** Conta todas as solicitações cujo createdAt está no intervalo, independente de status. */
    long countByCreatedAtBetween(OffsetDateTime inicio, OffsetDateTime fim);

    List<SolicitacaoAgendamentoEntity> findByStatusAndDataHoraInicioBetweenOrderByDataHoraInicioAsc(
            SolicitacaoStatus status,
            OffsetDateTime inicio,
            OffsetDateTime fim
    );

    List<SolicitacaoAgendamentoEntity> findByDataHoraInicioBetweenOrderByDataHoraInicioAsc(
            OffsetDateTime inicio,
            OffsetDateTime fim
    );

    /**
     * Solicitações processadas (status ≠ PENDENTE) ordenadas por updatedAt desc.
     * Usado pelo histórico quando não há filtro de data.
     */
    List<SolicitacaoAgendamentoEntity> findByStatusNotOrderByUpdatedAtDesc(SolicitacaoStatus statusExcluido);

    /**
     * Solicitações processadas (status ≠ PENDENTE) cujo updatedAt está no intervalo, ordenadas por updatedAt desc.
     * Usado pelo histórico quando o usuário filtra por período — evita full table scan.
     */
    List<SolicitacaoAgendamentoEntity> findByStatusNotAndUpdatedAtBetweenOrderByUpdatedAtDesc(
            SolicitacaoStatus statusExcluido, OffsetDateTime inicio, OffsetDateTime fim);

    Optional<SolicitacaoAgendamentoEntity> findByIdAndStatus(UUID id, SolicitacaoStatus status);

    boolean existsByClienteId(UUID clienteId);

    @Query("SELECT s FROM SolicitacaoAgendamentoEntity s WHERE " +
           "s.status <> :statusExcluido " +
           "AND (:status IS NULL OR s.status = :status) " +
           "AND (:inicio IS NULL OR s.updatedAt >= :inicio) " +
           "AND (:fim IS NULL OR s.updatedAt <= :fim) " +
           "AND (:buscaTexto IS NULL OR " +
           "     LOWER(s.nomePaciente) LIKE LOWER(CONCAT('%', :buscaTexto, '%')) OR " +
           "     s.telefone LIKE CONCAT('%', :buscaTelefone, '%')" +
           ") " +
           "ORDER BY s.updatedAt DESC")
    List<SolicitacaoAgendamentoEntity> filtrarHistoricoNoBanco(
            @Param("statusExcluido") SolicitacaoStatus statusExcluido,
            @Param("status") SolicitacaoStatus status,
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim,
            @Param("buscaTexto") String buscaTexto,
            @Param("buscaTelefone") String buscaTelefone
    );
}
