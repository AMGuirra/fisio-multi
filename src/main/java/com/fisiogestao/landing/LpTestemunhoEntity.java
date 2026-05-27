package com.fisiogestao.landing;

import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lp_testemunho")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class LpTestemunhoEntity extends BaseEntity {

    @Column(name = "nome", nullable = false, length = 160)
    private String nome;

    @Column(name = "cargo", length = 200)
    private String cargo;

    @Column(name = "depoimento", nullable = false, columnDefinition = "TEXT")
    private String depoimento;

    @Column(name = "iniciais", nullable = false, length = 4)
    private String iniciais;

    /** 1 = verde, 2 = azul, 3 = roxo */
    @Column(name = "cor_avatar", nullable = false)
    @Builder.Default
    private Integer corAvatar = 1;

    @Column(name = "estrelas", nullable = false)
    @Builder.Default
    private Integer estrelas = 5;

    @Column(name = "ordem", nullable = false)
    @Builder.Default
    private Integer ordem = 0;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
