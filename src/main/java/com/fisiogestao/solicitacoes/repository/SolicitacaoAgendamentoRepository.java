package com.fisiogestao.solicitacoes.repository;

import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import com.fisiogestao.solicitacoes.SolicitacaoStatus;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoAgendamentoRepository extends JpaRepository<SolicitacaoAgendamentoEntity, UUID>,
        JpaSpecificationExecutor<SolicitacaoAgendamentoEntity> {

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

    List<SolicitacaoAgendamentoEntity> findByClienteIdOrderByCreatedAtDesc(UUID clienteId);

    void deleteByClienteId(UUID clienteId);
}
