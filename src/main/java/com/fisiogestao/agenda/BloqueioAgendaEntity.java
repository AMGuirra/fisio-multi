package com.fisiogestao.agenda;

import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "criado_por", length = 80)
    private String criadoPor;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
