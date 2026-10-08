package com.fisiogestao.empresa;

import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Clínica/empresa cliente da plataforma (tenant). Todos os dados de negócio
 * referenciam esta tabela por {@code empresa_id}.
 */
@Entity
@Table(name = "empresa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaEntity extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String nome;

    @Column(nullable = false, length = 80, unique = true)
    private String slug;

    @Column(name = "nome_responsavel", length = 160)
    private String nomeResponsavel;

    @Column(length = 11)
    private String cpf;

    @Column(length = 14)
    private String cnpj;

    @Column(length = 160)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmpresaStatus status;

    @Column(name = "motivo_bloqueio", columnDefinition = "TEXT")
    private String motivoBloqueio;

    @Column(name = "data_inicio_teste")
    private OffsetDateTime dataInicioTeste;

    @Column(name = "data_fim_teste")
    private OffsetDateTime dataFimTeste;

    @Column(name = "quantidade_dias_uteis_teste")
    private Integer quantidadeDiasUteisTeste;

    @Column(name = "alerta_vencimento_teste_em")
    private OffsetDateTime alertaVencimentoTesteEm;

    @Column(name = "alerta_expiracao_teste_em")
    private OffsetDateTime alertaExpiracaoTesteEm;

    /** Controle otimista: duas alterações simultâneas no Gestor não se sobrescrevem silenciosamente. */
    @Version
    @Column(nullable = false)
    private Long versao;

    public boolean isEmTeste() {
        return status == EmpresaStatus.TESTE;
    }
}
