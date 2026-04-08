package com.fisiogestao.financeiro.repository;

import com.fisiogestao.financeiro.LancamentoFinanceiroEntity;
import com.fisiogestao.financeiro.StatusLancamentoFinanceiro;
import com.fisiogestao.financeiro.TipoLancamentoFinanceiro;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LancamentoFinanceiroRepository extends JpaRepository<LancamentoFinanceiroEntity, UUID> {

    @EntityGraph(attributePaths = {"cliente", "pacoteSessao"})
    List<LancamentoFinanceiroEntity> findAllByOrderByDataCompetenciaDescCreatedAtDesc();

    boolean existsByClienteId(UUID clienteId);

    @EntityGraph(attributePaths = {"cliente", "pacoteSessao"})
    List<LancamentoFinanceiroEntity> findByPacoteSessaoId(UUID pacoteSessaoId);

    @Override
    @EntityGraph(attributePaths = {"cliente", "pacoteSessao"})
    Optional<LancamentoFinanceiroEntity> findById(UUID id);

    boolean existsByPacoteSessaoIdAndStatus(UUID pacoteSessaoId, StatusLancamentoFinanceiro status);

    long countByTipoAndStatus(TipoLancamentoFinanceiro tipo, StatusLancamentoFinanceiro status);

    @EntityGraph(attributePaths = {"cliente", "pacoteSessao"})
    List<LancamentoFinanceiroEntity> findByDataCompetenciaBetweenOrderByDataCompetenciaDescCreatedAtDesc(
            OffsetDateTime inicio, OffsetDateTime fim);
}
