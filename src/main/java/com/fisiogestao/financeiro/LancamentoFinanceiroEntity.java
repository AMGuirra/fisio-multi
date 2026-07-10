package com.fisiogestao.financeiro;

import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.PlanoSessaoEntity;
import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "lancamento_financeiro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class LancamentoFinanceiroEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoLancamentoFinanceiro tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusLancamentoFinanceiro status = StatusLancamentoFinanceiro.PENDENTE;

    @Column(nullable = false, length = 180)
    private String descricao;

    @Column(nullable = false, length = 80)
    private String categoria;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_recebimento", nullable = false)
    private OffsetDateTime dataRecebimento;

    @Column(name = "data_pagamento")
    private OffsetDateTime dataPagamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private ClienteEntity cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_sessao_id")
    private PlanoSessaoEntity planoSessao;

    @Column(length = 120)
    private String profissional;

    /** Forma de pagamento registrada no momento da baixa (V32). */
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pagamento", length = 30)
    private MetodoPagamento metodoPagamento;
}
