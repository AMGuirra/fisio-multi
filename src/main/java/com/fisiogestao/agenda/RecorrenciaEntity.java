package com.fisiogestao.agenda;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recorrencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecorrenciaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Frequencia frequencia;

    @Column(name = "dias_semana", length = 50)
    private String diasSemana;

    @Column(nullable = false)
    @Builder.Default
    private Integer intervalo = 1;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "max_ocorrencias")
    @Builder.Default
    private Integer maxOcorrencias = 52;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }
}
