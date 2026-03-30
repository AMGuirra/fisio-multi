package com.fisiogestao.prontuario;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Entity
@Table(name = "prontuario_soap")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ProntuarioSoapEntity extends BaseEntity {

    @NonNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEntity cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessao_id")
    private SessaoEntity sessao;

    @NonNull
    @Column(nullable = false, columnDefinition = "TEXT")
    private String subjetivo;

    @NonNull
    @Column(nullable = false, columnDefinition = "TEXT")
    private String objetivo;

    @NonNull
    @Column(nullable = false, columnDefinition = "TEXT")
    private String avaliacao;

    @NonNull
    @Column(nullable = false, columnDefinition = "TEXT")
    private String plano;

    @NonNull
    @Column(name = "criado_por", nullable = false, length = 80)
    private String criadoPor;
}
