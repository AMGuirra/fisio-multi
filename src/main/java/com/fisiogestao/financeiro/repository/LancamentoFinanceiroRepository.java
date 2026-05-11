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
import java.util.UUID;

public interface LancamentoFinanceiroRepository extends JpaRepository<LancamentoFinanceiroEntity, UUID> {

    @EntityGraph(attributePaths = {"cliente", "pacoteSessao"})
    List<LancamentoFinanceiroEntity> findAllByOrderByDataCompetenciaDescCreatedAtDesc();

    boolean existsByClienteId(UUID clienteId);

    @EntityGraph(attributePaths = {"cliente", "pacoteSessao"})
    List<LancamentoFinanceiroEntity> findByPacoteSessaoId(UUID pacoteSessaoId);

    boolean existsByPacoteSessaoIdAndStatus(UUID pacoteSessaoId, StatusLancamentoFinanceiro status);

    long countByTipoAndStatus(TipoLancamentoFinanceiro tipo, StatusLancamentoFinanceiro status);

    @EntityGraph(attributePaths = {"cliente", "pacoteSessao"})
    List<LancamentoFinanceiroEntity> findByDataCompetenciaBetweenOrderByDataCompetenciaDescCreatedAtDesc(
            OffsetDateTime inicio, OffsetDateTime fim);

    /**
     * Soma os valores no banco diretamente, evitando carregar todos os registros em memória.
     */
    @Query("SELECT COALESCE(SUM(l.valor), 0) FROM LancamentoFinanceiroEntity l WHERE l.tipo = :tipo AND l.status = :status")
    BigDecimal sumValorByTipoAndStatus(
            @Param("tipo") TipoLancamentoFinanceiro tipo,
            @Param("status") StatusLancamentoFinanceiro status);
}
