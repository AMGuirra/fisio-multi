package com.fisiogestao.agenda;

import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bloqueio_agenda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class BloqueioAgendaEntity extends BaseEntity {

    @NonNull
    @Column(name = "data_hora_inicio", nullable = false)
    private OffsetDateTime dataHoraInicio;

    @NonNull
    @Column(name = "data_hora_fim", nullable = false)
    private OffsetDateTime dataHoraFim;

    @NonNull
    @Column(nullable = false, length = 255)
    private String motivo;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BloqueioAgendaStatus status = BloqueioAgendaStatus.ATIVO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BloqueioAgendaOrigem origem = BloqueioAgendaOrigem.MANUAL;

    @Column(name = "dia_inteiro", nullable = false)
    @Builder.Default
    private Boolean diaInteiro = false;

    @Column(name = "criado_por", length = 80)
    private String criadoPor;

    @Column(name = "atualizado_por", length = 80)
    private String atualizadoPor;

    @Column(name = "grupo_referencia")
    private UUID grupoReferencia;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    public LocalDate getDataInicial() {
        return dataHoraInicio != null ? dataHoraInicio.toLocalDate() : null;
    }

    public LocalDate getDataFinal() {
        return dataHoraFim != null ? dataHoraFim.toLocalDate() : null;
    }

    public LocalTime getHoraInicio() {
        return dataHoraInicio != null ? dataHoraInicio.toLocalTime() : null;
    }

    public LocalTime getHoraFim() {
        return dataHoraFim != null ? dataHoraFim.toLocalTime() : null;
    }

    public boolean isCancelado() {
        return status == BloqueioAgendaStatus.CANCELADO || Boolean.FALSE.equals(ativo);
    }
}
