package com.fisiogestao.agenda;

import com.fisiogestao.shared.persistence.BaseEntity;
import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "horario_bloqueado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class HorarioBloqueadoEntity extends BaseEntity {

    @NonNull
    @Column(name = "data_hora_inicio", nullable = false)
    private OffsetDateTime dataHoraInicio;

    @NonNull
    @Column(name = "data_hora_fim", nullable = false)
    private OffsetDateTime dataHoraFim;

    @NonNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private HorarioBloqueadoStatus status = HorarioBloqueadoStatus.PENDENTE;

    @NonNull
    @Column(nullable = false, length = 50)
    private String origem;

    @Column(length = 255)
    private String motivo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitacao_id", unique = true)
    private SolicitacaoAgendamentoEntity solicitacao;
}
