package com.fisiogestao.agenda;

import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.PacoteSessaoEntity;
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
@Table(name = "sessao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class SessaoEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEntity cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pacote_sessao_id")
    private PacoteSessaoEntity pacoteSessao;

    @Column(name = "data_hora_inicio", nullable = false)
    private OffsetDateTime dataHoraInicio;

    @Column(name = "data_hora_fim", nullable = false)
    private OffsetDateTime dataHoraFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SessaoStatus status = SessaoStatus.AGENDADA;

    @Column(name = "tipo_atendimento", length = 80)
    private String tipoAtendimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_agendamento", nullable = false, length = 20)
    @Builder.Default
    private TipoAgendamento tipoAgendamento = TipoAgendamento.PACOTE;

    @Column(name = "valor_avulso", precision = 10, scale = 2)
    private BigDecimal valorAvulso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorrencia_id")
    private RecorrenciaEntity recorrencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorrencia_pai_id")
    private SessaoEntity recorrenciaPai;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "pacote_consumo_registrado", nullable = false)
    @Builder.Default
    private Boolean pacoteConsumoRegistrado = Boolean.FALSE;

    /** True quando o paciente confirmou presença via portal de autoatendimento (V21). */
    @Column(name = "confirmado_pelo_paciente", nullable = false)
    @Builder.Default
    private Boolean confirmadoPeloPaciente = Boolean.FALSE;
}
