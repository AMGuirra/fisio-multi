package com.fisiogestao.agenda.repository;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessaoRepository extends JpaRepository<SessaoEntity, UUID> {

    boolean existsByStatusInAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
            Collection<SessaoStatus> statuses,
            OffsetDateTime dataHoraFim,
            OffsetDateTime dataHoraInicio
    );

    List<SessaoEntity> findByStatusInAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
            Collection<SessaoStatus> statuses,
            OffsetDateTime dataHoraFim,
            OffsetDateTime dataHoraInicio
    );

    @EntityGraph(attributePaths = {"cliente", "pacoteSessao", "recorrencia", "recorrenciaPai"})
    List<SessaoEntity> findByDataHoraInicioBetweenOrderByDataHoraInicioAsc(
            OffsetDateTime inicio,
            OffsetDateTime fim
    );

    @EntityGraph(attributePaths = {"cliente", "pacoteSessao", "recorrencia", "recorrenciaPai"})
    List<SessaoEntity> findByClienteIdOrderByDataHoraInicioDesc(UUID clienteId);

    @Override
    @EntityGraph(attributePaths = {"cliente", "pacoteSessao", "recorrencia", "recorrenciaPai"})
    Optional<SessaoEntity> findById(UUID id);

    boolean existsByClienteId(UUID clienteId);

    boolean existsByPacoteSessaoId(UUID pacoteSessaoId);

    boolean existsByPacoteSessaoIdAndStatusIn(UUID pacoteSessaoId, Collection<SessaoStatus> statuses);

    long countByStatus(SessaoStatus status);

    Optional<SessaoEntity> findFirstByClienteIdAndDataHoraInicioAndStatus(UUID clienteId, OffsetDateTime dataHoraInicio, SessaoStatus status);

    // --- Métodos para o portal do paciente ---

    List<SessaoEntity> findByClienteIdAndStatusAndDataHoraInicioAfterOrderByDataHoraInicioAsc(
            UUID clienteId, SessaoStatus status, OffsetDateTime depois);

    List<SessaoEntity> findTop10ByClienteIdAndStatusNotOrderByDataHoraInicioDesc(
            UUID clienteId, SessaoStatus statusExcluido);

    Optional<SessaoEntity> findByIdAndClienteIdAndStatus(UUID id, UUID clienteId, SessaoStatus status);
}
