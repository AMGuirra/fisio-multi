package com.fisiogestao.whatsapp;

import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "mensagem_whatsapp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class MensagemWhatsAppEntity extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String destinatario;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusMensagemWhatsApp status = StatusMensagemWhatsApp.PENDENTE;

    @Column(length = 120)
    private String referencia;

    @Column(name = "agendada_para")
    private OffsetDateTime agendadaPara;

    @Column(name = "enviada_em")
    private OffsetDateTime enviadaEm;

    @Column(nullable = false)
    @Builder.Default
    private Integer tentativas = 0;

    @Column(columnDefinition = "TEXT")
    private String erro;
}
