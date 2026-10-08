package com.fisiogestao.suporte;

import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Pedido de suporte enviado pela página pública; tratado apenas pelo SUPER_ADMIN. */
@Entity
@Table(name = "solicitacao_suporte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitacaoSuporteEntity extends BaseEntity {

    public static final int LIMITE_DESCRICAO = 500;

    /** Empresa identificada pelo CPF/CNPJ informado, quando houver correspondência única. */
    @Column(name = "empresa_id")
    private UUID empresaId;

    @Column(nullable = false, length = 160)
    private String nome;

    @Column(length = 11)
    private String cpf;

    @Column(length = 14)
    private String cnpj;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(nullable = false, length = LIMITE_DESCRICAO)
    private String descricao;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSolicitacaoSuporte status = StatusSolicitacaoSuporte.PENDENTE;

    @Column(name = "observacao_gestor", columnDefinition = "TEXT")
    private String observacaoGestor;

    @Column(name = "ip_origem", length = 64)
    private String ipOrigem;
}
