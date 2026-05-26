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

    @Query("SELECT COUNT(s) FROM SessaoEntity s WHERE s.status <> 'CANCELADA' AND s.dataHoraInicio >= :inicio AND s.dataHoraInicio < :fim")
    long countNaoCanceladasNoPeriodo(@Param("inicio") OffsetDateTime inicio, @Param("fim") OffsetDateTime fim);

    @Modifying
    @Query("DELETE FROM SessaoEntity s WHERE s.planoSessao.id = :planoId AND s.status = :status AND s.dataHoraInicio > :agora")
    int deletarFuturasPorPlanoEStatus(
            @Param("planoId") UUID planoId,
            @Param("status") SessaoStatus status,
            @Param("agora") OffsetDateTime agora);

    // --- Reconciliação de consumo ---

    @Query("""
            SELECT COUNT(s) FROM SessaoEntity s
            WHERE s.planoSessao.id = :planoId
              AND s.status IN (
                  com.fisiogestao.agenda.SessaoStatus.REALIZADA,
                  com.fisiogestao.agenda.SessaoStatus.FALTA_FATURADA
              )
              AND s.planoConsumoRegistrado = false
            """)
    long countNaoRegistradasPorPlano(@Param("planoId") UUID planoId);

    @Query("""
            SELECT COUNT(s) FROM SessaoEntity s
            WHERE s.planoSessao.id = :planoId
              AND s.status IN (
                  com.fisiogestao.agenda.SessaoStatus.REALIZADA,
                  com.fisiogestao.agenda.SessaoStatus.FALTA_FATURADA
              )
            """)
    long countContabilizaveisPorPlano(@Param("planoId") UUID planoId);

    @Modifying
    @Query("""
            UPDATE SessaoEntity s
            SET s.planoConsumoRegistrado = true
            WHERE s.planoSessao.id = :planoId
              AND s.status IN (
                  com.fisiogestao.agenda.SessaoStatus.REALIZADA,
                  com.fisiogestao.agenda.SessaoStatus.FALTA_FATURADA
              )
              AND s.planoConsumoRegistrado = false
            """)
    int marcarComoRegistradasPorPlano(@Param("planoId") UUID planoId);

    @Query("""
            SELECT COUNT(s) FROM SessaoEntity s
            WHERE s.planoSessao.id = :planoId
              AND s.status = com.fisiogestao.agenda.SessaoStatus.AGENDADA
            """)
    long countAgendadasPorPlano(@Param("planoId") UUID planoId);

    // Sessões de plano do paciente no período sem vínculo ao plano (órfãs)
    @Query("""
            SELECT COUNT(s) FROM SessaoEntity s
            WHERE s.cliente.id = :clienteId
              AND s.tipoAgendamento = com.fisiogestao.agenda.TipoAgendamento.PLANO
              AND s.status IN (
                  com.fisiogestao.agenda.SessaoStatus.REALIZADA,
                  com.fisiogestao.agenda.SessaoStatus.FALTA_FATURADA
              )
              AND s.planoSessao IS NULL
              AND s.dataHoraInicio >= :inicio
              AND s.dataHoraInicio <= :fim
            """)
    long countOrfasRealizadasNoPeriodo(
            @Param("clienteId") UUID clienteId,
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim);

    @Modifying
    @Query(value = """
            UPDATE sessao
               SET plano_sessao_id = :planoId,
                   plano_consumo_registrado = false
             WHERE cliente_id = :clienteId
               AND tipo_agendamento = 'PLANO'
               AND status IN ('REALIZADA', 'FALTA_FATURADA')
               AND plano_sessao_id IS NULL
               AND data_hora_inicio >= :inicio
               AND data_hora_inicio <= :fim
            """, nativeQuery = true)
    int vincularOrfasAoPlano(
            @Param("planoId") UUID planoId,
            @Param("clienteId") UUID clienteId,
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim);
}
