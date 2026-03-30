package com.fisiogestao.agenda;

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

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

@Entity
@Table(name = "configuracao_clinica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ConfiguracaoClinicaEntity extends BaseEntity {

    @NonNull
    @Column(name = "nome_clinica", nullable = false, length = 150)
    private String nomeClinica;

    @NonNull
    @Column(name = "dias_funcionamento", nullable = false, length = 120)
    private String diasFuncionamento;

    @NonNull
    @Column(name = "horario_abertura", nullable = false)
    private LocalTime horarioAbertura;

    @NonNull
    @Column(name = "horario_fechamento", nullable = false)
    private LocalTime horarioFechamento;

    @Column(name = "horario_inicio_almoco")
    private LocalTime horarioInicioAlmoco;

    @Column(name = "horario_fim_almoco")
    private LocalTime horarioFimAlmoco;

    @NonNull
    @Column(name = "duracao_sessao_minutos", nullable = false)
    private Integer duracaoSessaoMinutos;

    @NonNull
    @Column(name = "timezone_id", nullable = false, length = 60)
    private String timezoneId;

    @Column(name = "telefone_whatsapp", length = 20)
    private String telefoneWhatsapp;

    @Column(name = "email_contato", length = 120)
    private String emailContato;

    @NonNull
    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    public Set<DayOfWeek> diasFuncionamentoSet() {
        EnumSet<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
        if (diasFuncionamento == null || diasFuncionamento.isBlank()) {
            return dias;
        }
        Arrays.stream(diasFuncionamento.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> DayOfWeek.valueOf(value.toUpperCase(Locale.ROOT)))
                .forEach(dias::add);
        return dias;
    }
}
