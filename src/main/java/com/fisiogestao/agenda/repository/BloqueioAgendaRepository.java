package com.fisiogestao.agenda.repository;

import com.fisiogestao.agenda.BloqueioAgendaEntity;
import com.fisiogestao.agenda.BloqueioAgendaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BloqueioAgendaRepository extends JpaRepository<BloqueioAgendaEntity, UUID> {

    boolean existsByStatusAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
            BloqueioAgendaStatus status,
            OffsetDateTime dataHoraFim,
            OffsetDateTime dataHoraInicio
    );

    List<BloqueioAgendaEntity> findByStatusAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
            BloqueioAgendaStatus status,
            OffsetDateTime dataHoraFim,
            OffsetDateTime dataHoraInicio
    );

    List<BloqueioAgendaEntity> findByStatusAndDataHoraInicioBetweenOrderByDataHoraInicioAsc(
            BloqueioAgendaStatus status,
            OffsetDateTime inicio,
            OffsetDateTime fim
    );

    List<BloqueioAgendaEntity> findAllByOrderByDataHoraInicioDesc();

    Optional<BloqueioAgendaEntity> findByIdAndStatus(UUID id, BloqueioAgendaStatus status);

    List<BloqueioAgendaEntity> findByGrupoReferenciaOrderByDataHoraInicioAsc(UUID grupoReferencia);
}
