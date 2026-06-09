package com.fisiogestao.landing;

import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lp_beneficio")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class LpBeneficioEntity extends BaseEntity {

    /** Chave do ícone: BELL, LIST, CHART, CARD, MOBILE, LOCK, CLOCK, USERS, CALENDAR, STAR */
    @Column(name = "icone", nullable = false, length = 30)
    @Builder.Default
    private String icone = "STAR";

    /** 1 = verde, 2 = azul, 3 = amarelo */
    @Column(name = "cor", nullable = false)
    @Builder.Default
    private Integer cor = 1;

    @Column(name = "titulo", nullable = false, length = 160)
    private String titulo;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "ordem", nullable = false)
    @Builder.Default
    private Integer ordem = 0;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
