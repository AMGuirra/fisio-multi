package com.fisiogestao.landing;

import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "lp_plano")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class LpPlanoEntity extends BaseEntity {

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "preco", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal preco = BigDecimal.ZERO;

    @Column(name = "periodo", nullable = false, length = 30)
    @Builder.Default
    private String periodo = "/mês";

    /**
     * Recursos: um por linha.
     * Linhas iniciadas com '+' = incluído; '-' = não incluído.
     */
    @Column(name = "recursos", columnDefinition = "TEXT")
    private String recursos;

    @Column(name = "destaque", nullable = false)
    @Builder.Default
    private Boolean destaque = false;

    @Column(name = "badge_texto", length = 60)
    private String badgeTexto;

    @Column(name = "cta_texto", nullable = false, length = 80)
    @Builder.Default
    private String ctaTexto = "Começar agora";

    @Column(name = "cta_link", nullable = false, length = 200)
    @Builder.Default
    private String ctaLink = "/login";

    @Column(name = "ordem", nullable = false)
    @Builder.Default
    private Integer ordem = 0;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
