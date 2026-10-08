package com.fisiogestao.cancelamento;

import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pedido de saída da plataforma feito pela própria clínica. Não altera o status da empresa
 * nem apaga dados: a decisão é sempre do SUPER_ADMIN, na área Gestor.
 */
@Entity
@Table(name = "solicitacao_cancelamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitacaoCancelamentoEntity extends BaseEntity {

    public static final int LIMITE_DESCRICAO = 500;

    @Column(name = "empresa_id")
    private UUID empresaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigemSolicitacaoCancelamento origem;

    @Column(name = "solicitante_usuario_id")
    private UUID solicitanteUsuarioId;

    @Column(name = "nome_solicitante", nullable = false, length = 160)
    private String nomeSolicitante;

    @Column(name = "email_contato", nullable = false, length = 160)
    private String emailContato;

    @Column(length = 11)
    private String cpf;

    @Column(length = 14)
    private String cnpj;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MotivoCancelamento motivo;

    @Column(length = LIMITE_DESCRICAO)
    private String descricao;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusSolicitacaoCancelamento status = StatusSolicitacaoCancelamento.PENDENTE;

    @Column(name = "data_cancelamento_agendada")
    private LocalDate dataCancelamentoAgendada;

    @Column(name = "observacao_gestor", columnDefinition = "TEXT")
    private String observacaoGestor;

    @Column(name = "concluida_em")
    private OffsetDateTime concluidaEm;

    @Column(name = "ip_origem", length = 64)
    private String ipOrigem;

    @Builder.Default
    @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<SolicitacaoCancelamentoHistoricoEntity> historico = new ArrayList<>();

    public void registrarHistorico(StatusSolicitacaoCancelamento anterior, String observacao, String usuarioNome) {
        SolicitacaoCancelamentoHistoricoEntity item = SolicitacaoCancelamentoHistoricoEntity.builder()
                .solicitacao(this)
                .statusAnterior(anterior)
                .statusNovo(status)
                .observacao(observacao)
                .usuarioNome(usuarioNome)
                .build();
        historico.add(item);
    }
}
