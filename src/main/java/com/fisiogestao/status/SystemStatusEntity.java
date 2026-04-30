package com.fisiogestao.status;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "system_status")
public class SystemStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SystemStatusType status;

    @Column(name = "mensagem", columnDefinition = "TEXT")
    private String mensagem;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public SystemStatusEntity() {
    }

    public SystemStatusEntity(SystemStatusType status, String mensagem) {
        this.status = status;
        this.mensagem = mensagem;
        this.atualizadoEm = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SystemStatusType getStatus() {
        return status;
    }

    public void setStatus(SystemStatusType status) {
        this.status = status;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}