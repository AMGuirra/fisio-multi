package com.fisiogestao.pacientes;

import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ClienteEntity extends BaseEntity {

    @NonNull
    @Column(name = "nome_completo", nullable = false, length = 160)
    private String nomeCompleto;

    @Column(length = 20)
    private String telefone;

    @Column(length = 120)
    private String email;

    @Column(length = 11)
    private String cpf;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

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

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    public String getEnderecoCompleto() {
        StringBuilder builder = new StringBuilder();
        appendEndereco(builder, rua);
        appendEndereco(builder, numero);
        appendEndereco(builder, bairro);

        String cidadeEstado = juntarCidadeEstado();
        appendEndereco(builder, cidadeEstado);

        if (cep != null && !cep.isBlank()) {
            appendEndereco(builder, "CEP " + cep);
        }
        return builder.toString();
    }

    private void appendEndereco(StringBuilder builder, String valor) {
        if (valor == null || valor.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(", ");
        }
        builder.append(valor.trim());
    }

    private String juntarCidadeEstado() {
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
