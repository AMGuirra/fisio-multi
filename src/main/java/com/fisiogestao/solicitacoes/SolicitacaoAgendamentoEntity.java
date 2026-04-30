package com.fisiogestao.solicitacoes;

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
@Table(name = "solicitacao_agendamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class SolicitacaoAgendamentoEntity extends BaseEntity {

    @Column(name = "nome_paciente", nullable = false, length = 160)
    private String nomePaciente;

    @Column(nullable = false, length = 20)
    private String telefone;

    @Column(length = 120)
    private String email;

    @Column(length = 11)
    private String cpf;

    @Column(name = "data_hora_inicio", nullable = false)
    private OffsetDateTime dataHoraInicio;

    @Column(name = "data_hora_fim", nullable = false)
    private OffsetDateTime dataHoraFim;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "local_atendimento", nullable = false, length = 20)
    @Builder.Default
    private String localAtendimento = "CLINICA";

    @Column(length = 8)
    private String cep;

    @Column(length = 160)
    private String rua;

    @Column(length = 20)
    private String numero;

    @Column(length = 120)
    private String bairro;

    @Column(length = 120)
    private String cidade;

    @Column(length = 2)
    private String estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SolicitacaoStatus status = SolicitacaoStatus.PENDENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private ClienteEntity cliente;

    public boolean isAtendimentoDomiciliar() {
        return "DOMICILIO".equalsIgnoreCase(localAtendimento);
    }

    public String getLocalAtendimentoDescricao() {
        return isAtendimentoDomiciliar() ? "Em domicílio" : "Clínica";
    }

    public String getEnderecoResumo() {
        if (!isAtendimentoDomiciliar()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        appendParte(builder, rua);
        appendParte(builder, numero);
        appendParte(builder, bairro);

        String cidadeEstado = cidadeEstado();
        appendParte(builder, cidadeEstado);

        if (cep != null && !cep.isBlank()) {
            appendParte(builder, "CEP " + cep);
        }

        return builder.toString();
    }

    public String getIniciaisPaciente() {
        if (nomePaciente == null || nomePaciente.isBlank()) {
            return "PA";
        }

        String[] partes = nomePaciente.trim().split("\\s+");
        if (partes.length == 1) {
            return limitarInicial(partes[0], 2);
        }
        return (limitarInicial(partes[0], 1) + limitarInicial(partes[partes.length - 1], 1)).toUpperCase();
    }

    private String limitarInicial(String texto, int tamanho) {
        String valor = texto == null ? "" : texto.trim();
        if (valor.isEmpty()) {
            return "";
        }
        return valor.substring(0, Math.min(valor.length(), tamanho)).toUpperCase();
    }

    private void appendParte(StringBuilder builder, String valor) {
        if (valor == null || valor.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(", ");
        }
        builder.append(valor.trim());
    }

    private String cidadeEstado() {
        if ((cidade == null || cidade.isBlank()) && (estado == null || estado.isBlank())) {
            return null;
        }
        if (cidade == null || cidade.isBlank()) {
            return estado.trim();
        }
        if (estado == null || estado.isBlank()) {
            return cidade.trim();
        }
        return cidade.trim() + "/" + estado.trim();
    }
}
