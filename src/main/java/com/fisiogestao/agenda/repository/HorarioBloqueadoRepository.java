package com.fisiogestao.agenda.repository;

import com.fisiogestao.agenda.HorarioBloqueadoEntity;
import com.fisiogestao.agenda.HorarioBloqueadoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HorarioBloqueadoRepository extends JpaRepository<HorarioBloqueadoEntity, UUID> {

    boolean existsByStatusInAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
            Collection<HorarioBloqueadoStatus> statuses,
            OffsetDateTime dataHoraFim,
            OffsetDateTime dataHoraInicio
    );

    List<HorarioBloqueadoEntity> findByStatusInAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
            Collection<HorarioBloqueadoStatus> statuses,
            OffsetDateTime dataHoraFim,
            OffsetDateTime dataHoraInicio
    );

    List<HorarioBloqueadoEntity> findByDataHoraInicioBetweenOrderByDataHoraInicioAsc(
            OffsetDateTime inicio,
            OffsetDateTime fim
    );

    Optional<HorarioBloqueadoEntity> findBySolicitacaoId(UUID solicitacaoId);
}
