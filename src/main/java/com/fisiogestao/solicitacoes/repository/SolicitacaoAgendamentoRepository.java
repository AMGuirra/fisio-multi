package com.fisiogestao.solicitacoes.repository;

import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import com.fisiogestao.solicitacoes.SolicitacaoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
