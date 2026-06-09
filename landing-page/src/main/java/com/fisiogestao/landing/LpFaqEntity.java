package com.fisiogestao.landing;

import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lp_faq")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class LpFaqEntity extends BaseEntity {

    @Column(name = "pergunta", nullable = false, length = 500)
    private String pergunta;

    @Column(name = "resposta", nullable = false, columnDefinition = "TEXT")
    private String resposta;

    @Column(name = "ordem", nullable = false)
    @Builder.Default
    private Integer ordem = 0;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
