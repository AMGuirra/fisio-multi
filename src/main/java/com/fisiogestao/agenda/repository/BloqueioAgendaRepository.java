package com.fisiogestao.agenda.repository;

import com.fisiogestao.agenda.BloqueioAgendaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface BloqueioAgendaRepository extends JpaRepository<BloqueioAgendaEntity, UUID> {

    boolean existsByAtivoTrueAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
            OffsetDateTime dataHoraFim,
            OffsetDateTime dataHoraInicio
    );

    List<BloqueioAgendaEntity> findByAtivoTrueAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
            OffsetDateTime dataHoraFim,
            OffsetDateTime dataHoraInicio
    );

    List<BloqueioAgendaEntity> findByAtivoTrueAndDataHoraInicioBetweenOrderByDataHoraInicioAsc(
            OffsetDateTime inicio,
            OffsetDateTime fim
    );
}
