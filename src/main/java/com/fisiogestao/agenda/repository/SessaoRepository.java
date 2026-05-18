package com.fisiogestao.agenda.repository;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @EntityGraph(attributePaths = {"cliente", "planoSessao", "recorrencia", "recorrenciaPai"})
    List<SessaoEntity> findByDataHoraInicioBetweenOrderByDataHoraInicioAsc(
            OffsetDateTime inicio,
            OffsetDateTime fim
    );

    long countByDataHoraInicioBetween(OffsetDateTime inicio, OffsetDateTime fim);

    @EntityGraph(attributePaths = {"cliente", "planoSessao", "recorrencia", "recorrenciaPai"})
    List<SessaoEntity> findByClienteIdOrderByDataHoraInicioDesc(UUID clienteId);

    @Override
    @EntityGraph(attributePaths = {"cliente", "planoSessao", "recorrencia", "recorrenciaPai"})
    Optional<SessaoEntity> findById(UUID id);

    boolean existsByClienteId(UUID clienteId);

    boolean existsByPlanoSessaoId(UUID planoSessaoId);

    long countByClienteIdAndStatus(UUID clienteId, SessaoStatus status);

    @EntityGraph(attributePaths = {"cliente", "planoSessao", "recorrencia", "recorrenciaPai"})
    List<SessaoEntity> findByPlanoSessaoIdOrderByDataHoraInicioDesc(UUID planoSessaoId);

    boolean existsByPlanoSessaoIdAndStatusIn(UUID planoSessaoId, Collection<SessaoStatus> statuses);

    long countByStatus(SessaoStatus status);

    Optional<SessaoEntity> findFirstByClienteIdAndDataHoraInicioAndStatus(UUID clienteId, OffsetDateTime dataHoraInicio, SessaoStatus status);

    // --- Métodos para o portal do paciente ---

    List<SessaoEntity> findByClienteIdAndStatusAndDataHoraInicioAfterOrderByDataHoraInicioAsc(
            UUID clienteId, SessaoStatus status, OffsetDateTime depois);

    List<SessaoEntity> findTop10ByClienteIdAndStatusNotOrderByDataHoraInicioDesc(
            UUID clienteId, SessaoStatus statusExcluido);

    Optional<SessaoEntity> findByIdAndClienteIdAndStatus(UUID id, UUID clienteId, SessaoStatus status);

    @EntityGraph(attributePaths = {"cliente"})
    @Query("""
            SELECT s FROM SessaoEntity s
            WHERE s.status = com.fisiogestao.agenda.SessaoStatus.AGENDADA
              AND s.lembreteEnviado = false
              AND s.dataHoraInicio >= :de
              AND s.dataHoraInicio < :ate
            """)
    List<SessaoEntity> findSessoesParaLembrete(
            @Param("de") OffsetDateTime de,
            @Param("ate") OffsetDateTime ate);

    @Modifying
    @Query("DELETE FROM SessaoEntity s WHERE s.planoSessao.id = :planoId AND s.status = :status AND s.dataHoraInicio > :agora")
    int deletarFuturasPorPlanoEStatus(
            @Param("planoId") UUID planoId,
            @Param("status") SessaoStatus status,
            @Param("agora") OffsetDateTime agora);
}
