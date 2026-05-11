package com.fisiogestao.prontuario;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "prontuario_soap")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ProntuarioSoapEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEntity cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessao_id")
    private SessaoEntity sessao;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String subjetivo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String objetivo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String avaliacao;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String plano;

    @Column(name = "criado_por", nullable = false, length = 80)
    private String criadoPor;

    // --- Campos específicos de fisioterapia (V19) ---

    /**
     * Escala Visual Analógica de dor (EVA): 0 (sem dor) a 10 (pior dor imaginável).
     */
    @Column(name = "escala_eva_dor")
    private Integer escalaEvaDor;

    /**
     * Região corporal principal tratada (ex.: "Coluna lombar", "Joelho D").
     */
    @Column(name = "regiao_corporal", length = 120)
    private String regiaoCorporal;

    /**
     * Técnicas e recursos terapêuticos utilizados (ex.: "RPG, TENS 80Hz, crioterapia").
     */
    @Column(name = "tecnicas_utilizadas", columnDefinition = "TEXT")
    private String tecnicasUtilizadas;

    /**
     * Observações de evolução comparadas com sessões anteriores.
     */
    @Column(name = "obs_evolucao", columnDefinition = "TEXT")
    private String obsEvolucao;
}
