package com.fisiogestao.financeiro.repository;

import com.fisiogestao.financeiro.LancamentoFinanceiroEntity;
import com.fisiogestao.financeiro.MetodoPagamento;
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

    @EntityGraph(attributePaths = {"cliente", "planoSessao"})
    List<LancamentoFinanceiroEntity> findByTipoAndStatusOrderByDataCompetenciaAscCreatedAtAsc(
            TipoLancamentoFinanceiro tipo,
            StatusLancamentoFinanceiro status);

    @EntityGraph(attributePaths = {"cliente", "planoSessao"})
    List<LancamentoFinanceiroEntity> findByDataCompetenciaBetweenAndTipoAndStatusOrderByDataCompetenciaDescCreatedAtDesc(
            OffsetDateTime inicio,
            OffsetDateTime fim,
            TipoLancamentoFinanceiro tipo,
            StatusLancamentoFinanceiro status);

    long countByTipoAndStatusAndDataCompetenciaBefore(
            TipoLancamentoFinanceiro tipo,
            StatusLancamentoFinanceiro status,
            OffsetDateTime limite);

    @EntityGraph(attributePaths = {"cliente", "planoSessao"})
    Optional<LancamentoFinanceiroEntity> findFirstByTipoAndStatusAndDataCompetenciaBeforeOrderByDataCompetenciaAscCreatedAtAsc(
            TipoLancamentoFinanceiro tipo,
            StatusLancamentoFinanceiro status,
            OffsetDateTime limite);

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
            WHERE l.tipo = :tipo
              AND l.status = :status
              AND l.dataCompetencia < :limite
            """)
    BigDecimal sumValorByTipoAndStatusAndDataCompetenciaBefore(
            @Param("tipo") TipoLancamentoFinanceiro tipo,
            @Param("status") StatusLancamentoFinanceiro status,
            @Param("limite") OffsetDateTime limite);

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

    @Query("""
            SELECT l.categoria AS nome,
                   COUNT(l.id) AS quantidade,
                   COALESCE(SUM(l.valor), 0) AS total
            FROM LancamentoFinanceiroEntity l
            WHERE l.dataCompetencia BETWEEN :inicio AND :fim
              AND l.tipo = :tipo
              AND l.status = :status
            GROUP BY l.categoria
            ORDER BY COALESCE(SUM(l.valor), 0) DESC, l.categoria ASC
            """)
    List<TotalPorTextoProjection> agruparFaturamentoPorCategoria(
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim,
            @Param("tipo") TipoLancamentoFinanceiro tipo,
            @Param("status") StatusLancamentoFinanceiro status);

    @Query("""
            SELECT COALESCE(p.nomePlano, 'Sem plano') AS nome,
                   COUNT(l.id) AS quantidade,
                   COALESCE(SUM(l.valor), 0) AS total
            FROM LancamentoFinanceiroEntity l
            LEFT JOIN l.planoSessao p
            WHERE l.dataCompetencia BETWEEN :inicio AND :fim
              AND l.tipo = :tipo
              AND l.status = :status
            GROUP BY p.nomePlano
            ORDER BY COALESCE(SUM(l.valor), 0) DESC, COALESCE(p.nomePlano, 'Sem plano') ASC
            """)
    List<TotalPorTextoProjection> agruparFaturamentoPorPlano(
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim,
            @Param("tipo") TipoLancamentoFinanceiro tipo,
            @Param("status") StatusLancamentoFinanceiro status);

    @Query("""
            SELECT COALESCE(l.profissional, 'Sem profissional') AS nome,
                   COUNT(l.id) AS quantidade,
                   COALESCE(SUM(l.valor), 0) AS total
            FROM LancamentoFinanceiroEntity l
            WHERE l.dataCompetencia BETWEEN :inicio AND :fim
              AND l.tipo = :tipo
              AND l.status = :status
            GROUP BY l.profissional
            ORDER BY COALESCE(SUM(l.valor), 0) DESC, COALESCE(l.profissional, 'Sem profissional') ASC
            """)
    List<TotalPorTextoProjection> agruparFaturamentoPorProfissional(
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim,
            @Param("tipo") TipoLancamentoFinanceiro tipo,
            @Param("status") StatusLancamentoFinanceiro status);

    @Query("""
            SELECT l.metodoPagamento AS metodoPagamento,
                   COUNT(l.id) AS quantidade,
                   COALESCE(SUM(l.valor), 0) AS total
            FROM LancamentoFinanceiroEntity l
            WHERE l.dataCompetencia BETWEEN :inicio AND :fim
              AND l.tipo = :tipo
              AND l.status = :status
            GROUP BY l.metodoPagamento
            ORDER BY COALESCE(SUM(l.valor), 0) DESC
            """)
    List<TotalPorMetodoProjection> agruparFaturamentoPorMetodoPagamento(
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim,
            @Param("tipo") TipoLancamentoFinanceiro tipo,
            @Param("status") StatusLancamentoFinanceiro status);

    long countByDataCompetenciaBetweenAndStatusNot(
            OffsetDateTime inicio,
            OffsetDateTime fim,
            StatusLancamentoFinanceiro status);

    interface TotalPorTextoProjection {
        String getNome();
        Long getQuantidade();
        BigDecimal getTotal();
    }

    interface TotalPorMetodoProjection {
        MetodoPagamento getMetodoPagamento();
        Long getQuantidade();
        BigDecimal getTotal();
    }
}
