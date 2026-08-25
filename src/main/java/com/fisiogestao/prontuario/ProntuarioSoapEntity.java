package com.fisiogestao.prontuario;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.pacientes.ClienteEntity;
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

    /**
     * Discrimina se este registro é a Avaliação principal do paciente (única, editável)
     * ou uma entrada de Evolução (diário clínico, um registro novo por salvamento).
     * Registros legados (anteriores à V50) são tratados como EVOLUCAO.
     */
    @Column(name = "tipo_registro", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TipoRegistroProntuario tipoRegistro = TipoRegistroProntuario.EVOLUCAO;

    /**
     * Texto livre do fluxo simplificado de Avaliação/Evolução.
     * Registros SOAP legados não usam este campo — continuam representados por
     * subjetivo/objetivo/avaliacao/plano.
     */
    @Column(name = "texto_registro", columnDefinition = "TEXT")
    private String textoRegistro;

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

    /** Queixa principal relatada na avaliação inicial. */
    @Column(name = "queixa_principal", columnDefinition = "TEXT")
    private String queixaPrincipal;

    /** Achados da avaliação física inicial. */
    @Column(name = "avaliacao_fisica", columnDefinition = "TEXT")
    private String avaliacaoFisica;

    /** Achados da avaliação funcional inicial. */
    @Column(name = "avaliacao_funcional", columnDefinition = "TEXT")
    private String avaliacaoFuncional;

    /** Objetivos terapêuticos definidos para o tratamento. */
    @Column(name = "objetivos_tratamento", columnDefinition = "TEXT")
    private String objetivosTratamento;

    /** Plano terapêutico inicial. */
    @Column(name = "plano_terapeutico", columnDefinition = "TEXT")
    private String planoTerapeutico;

    /** Observações complementares da avaliação inicial. */
    @Column(name = "observacoes_avaliacao", columnDefinition = "TEXT")
    private String observacoesAvaliacao;

    /** Indica se a avaliação já usa o formato estruturado introduzido na V52. */
    public boolean isEstruturada() {
        return queixaPrincipal != null || avaliacaoFisica != null || avaliacaoFuncional != null
                || objetivosTratamento != null || planoTerapeutico != null || observacoesAvaliacao != null;
    }
}
