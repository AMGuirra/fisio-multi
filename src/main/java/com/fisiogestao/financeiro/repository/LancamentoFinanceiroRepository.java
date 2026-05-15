package com.fisiogestao.financeiro.repository;

import com.fisiogestao.financeiro.LancamentoFinanceiroEntity;
import com.fisiogestao.financeiro.StatusLancamentoFinanceiro;
import com.fisiogestao.financeiro.TipoLancamentoFinanceiro;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LancamentoFinanceiroRepository extends JpaRepository<LancamentoFinanceiroEntity, UUID> {

    boolean existsByClienteId(UUID clienteId);

    @EntityGraph(attributePaths = {"cliente", "planoSessao"})
    List<LancamentoFinanceiroEntity> findByPlanoSessaoId(UUID planoSessaoId);

    @EntityGraph(attributePaths = {"cliente", "planoSessao"})
    List<LancamentoFinanceiroEntity> findByClienteIdAndTipoAndStatusOrderByDataPagamentoDescDataCompetenciaDescCreatedAtDesc(
            UUID clienteId, TipoLancamentoFinanceiro tipo, StatusLancamentoFinanceiro status);

    @EntityGraph(attributePaths = {"cliente", "planoSessao"})
    Optional<LancamentoFinanceiroEntity> findByIdAndClienteIdAndTipoAndStatus(
            UUID id, UUID clienteId, TipoLancamentoFinanceiro tipo, StatusLancamentoFinanceiro status);

    boolean existsByPlanoSessaoIdAndStatus(UUID planoSessaoId, StatusLancamentoFinanceiro status);

    long countByTipoAndStatus(TipoLancamentoFinanceiro tipo, StatusLancamentoFinanceiro status);

    @EntityGraph(attributePaths = {"cliente", "planoSessao"})
    List<LancamentoFinanceiroEntity> findByDataCompetenciaBetweenOrderByDataCompetenciaDescCreatedAtDesc(
            OffsetDateTime inicio, OffsetDateTime fim);

    /**
     * Soma os valores no banco diretamente, evitando carregar todos os registros em memória.
     */
    @Query("SELECT COALESCE(SUM(l.valor), 0) FROM LancamentoFinanceiroEntity l WHERE l.tipo = :tipo AND l.status = :status")
    BigDecimal sumValorByTipoAndStatus(
            @Param("tipo") TipoLancamentoFinanceiro tipo,
            @Param("status") StatusLancamentoFinanceiro status);

    @Query("""
            SELECT COALESCE(SUM(l.valor), 0)
            FROM LancamentoFinanceiroEntity l
            WHERE l.dataCompetencia BETWEEN :inicio AND :fim
              AND l.tipo = :tipo
              AND l.status = :status
            """)
    BigDecimal sumValorByPeriodoAndTipoAndStatus(
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim,
            @Param("tipo") TipoLancamentoFinanceiro tipo,
            @Param("status") StatusLancamentoFinanceiro status);

    long countByDataCompetenciaBetweenAndTipoAndStatus(
            OffsetDateTime inicio,
            OffsetDateTime fim,
            TipoLancamentoFinanceiro tipo,
            StatusLancamentoFinanceiro status);

    long countByDataCompetenciaBetweenAndStatusNot(
            OffsetDateTime inicio,
            OffsetDateTime fim,
            StatusLancamentoFinanceiro status);
}
