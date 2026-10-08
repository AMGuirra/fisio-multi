package com.fisiogestao.pacientes;

import com.fisiogestao.tenant.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ClienteEntity extends TenantEntity {

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

    /**
     * Token UUID único para acesso ao portal de autoatendimento do paciente.
     * Gerado automaticamente na criação. Pode ser rotacionado via
     * {@code PacienteService#rotacionarPortalToken} caso o link seja comprometido.
     */
    @Column(name = "portal_token", nullable = false, unique = true)
    @Builder.Default
    private UUID portalToken = UUID.randomUUID();

    @Column(name = "senha_portal", length = 72)
    private String senhaPortal;

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
