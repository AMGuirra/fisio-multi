package com.fisiogestao.email;

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

import java.util.UUID;

@Entity
@Table(name = "email_configuracao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class EmailConfiguracaoEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 30)
    @Builder.Default
    private EmailScopeType scopeType = EmailScopeType.GLOBAL;

    @Column(name = "scope_id")
    private UUID scopeId;

    @Column(name = "nome_remetente", nullable = false, length = 120)
    private String nomeRemetente;

    @Column(name = "email_remetente", nullable = false, length = 160)
    private String emailRemetente;

    @Column(name = "senha_app_criptografada", nullable = false, columnDefinition = "TEXT")
    private String senhaAppCriptografada;

    @Column(name = "smtp_host", nullable = false, length = 180)
    private String smtpHost;

    @Column(name = "smtp_port", nullable = false)
    private Integer smtpPort;

    @Column(name = "usa_autenticacao", nullable = false)
    @Builder.Default
    private Boolean usaAutenticacao = true;

    @Column(name = "usa_tls", nullable = false)
    @Builder.Default
    private Boolean usaTls = true;

    @Column(name = "usa_ssl", nullable = false)
    @Builder.Default
    private Boolean usaSsl = false;

    @Column(name = "reply_to", length = 160)
    private String replyTo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = false;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    public String senhaMascarada() {
        if (senhaAppCriptografada == null || senhaAppCriptografada.isBlank()) {
            return "";
        }
        return "••••••••••";
    }
}
