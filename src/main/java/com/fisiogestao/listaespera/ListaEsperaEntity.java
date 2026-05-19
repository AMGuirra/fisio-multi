package com.fisiogestao.listaespera;

import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "lista_espera")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ListaEsperaEntity extends BaseEntity {

    /** Paciente cadastrado no sistema. Nullable para entradas do formulário público. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private ClienteEntity cliente;

    /** Nome informado diretamente no formulário público (quando não há cliente cadastrado). */
    @Column(name = "nome_publico", length = 200)
    private String nomePublico;

    @Column(name = "telefone_publico", length = 20)
    private String telefonePublico;

    @Column(name = "email_publico", length = 150)
    private String emailPublico;

    @Enumerated(EnumType.STRING)
    @Column(name = "periodo_preferido", nullable = false, length = 20)
    @Builder.Default
    private PeriodoPreferido periodoPreferido = PeriodoPreferido.QUALQUER;

    /** Dias da semana preferidos, ex: "SEGUNDA,QUARTA,SEXTA" */
    @Column(name = "dias_semana", length = 100)
    private String diasSemana;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusListaEspera status = StatusListaEspera.AGUARDANDO;

    public String getNomeExibicao() {
        if (cliente != null) return cliente.getNomeCompleto();
        return nomePublico != null ? nomePublico : "(sem nome)";
    }

    public String getInicialExibicao() {
        String nome = getNomeExibicao();
        if (nome == null || nome.isBlank()) {
            return "P";
        }
        return nome.trim().substring(0, 1).toUpperCase();
    }

    public String getTelefoneExibicao() {
        if (cliente != null) return cliente.getTelefone();
        return telefonePublico;
    }

    @Column(name = "notificado_em")
    private OffsetDateTime notificadoEm;
}
