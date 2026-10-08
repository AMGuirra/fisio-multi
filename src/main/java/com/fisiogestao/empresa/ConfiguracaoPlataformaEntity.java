package com.fisiogestao.empresa;

import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Configurações globais da plataforma, editadas apenas pelo SUPER_ADMIN (registro único). */
@Entity
@Table(name = "configuracao_plataforma")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracaoPlataformaEntity extends BaseEntity {

    public static final UUID ID_UNICO = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    public static final int DIAS_UTEIS_TESTE_PADRAO = 15;
    public static final int DIAS_UTEIS_AVISO_PADRAO = 3;

    @Column(name = "dias_uteis_teste_padrao", nullable = false)
    private Integer diasUteisTestePadrao;

    @Column(name = "dias_uteis_aviso_vencimento", nullable = false)
    private Integer diasUteisAvisoVencimento;

    @Column(name = "email_notificacoes", length = 160)
    private String emailNotificacoes;
}
