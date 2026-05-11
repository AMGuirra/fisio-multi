package com.fisiogestao.agenda.service;

import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.form.ConfiguracaoClinicaForm;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.config.ClinicTimezoneHolder;
import com.fisiogestao.config.TimezoneConfig;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConfiguracaoClinicaService {

    private static final List<TimezoneOption> TIMEZONES_SUPORTADOS = List.of(
            new TimezoneOption("America/Campo_Grande", "Campo Grande"),
            new TimezoneOption("America/Cuiaba", "Cuiabá"),
            new TimezoneOption("America/Sao_Paulo", "São Paulo"),
            new TimezoneOption("America/Manaus", "Manaus"),
            new TimezoneOption("America/Belem", "Belém"),
            new TimezoneOption("America/Recife", "Recife"),
            new TimezoneOption("America/Rio_Branco", "Rio Branco")
    );

    private final ConfiguracaoClinicaRepository configuracaoClinicaRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public ConfiguracaoClinicaForm carregarFormulario() {
        return toForm(buscarAtivaOuPadrao());
    }

    @Transactional
    public ConfiguracaoClinicaEntity salvar(ConfiguracaoClinicaForm form) {
        ConfiguracaoClinicaEntity entity = resolverEntidade(form.getId());
        boolean novoCadastro = entity.getId() == null;
        var snapshotAnterior = novoCadastro ? null : auditService.snapshotOf(entity);

        validarFormulario(form);

        entity.setNomeClinica(textoObrigatorio(form.getNomeClinica(), "Informe o nome da clínica."));
        entity.setDiasFuncionamento(serializarDias(form.getDiasFuncionamento()));
        entity.setHorarioAbertura(form.getHorarioAbertura());
        entity.setHorarioFechamento(form.getHorarioFechamento());
        entity.setHorarioInicioAlmoco(form.getHorarioInicioAlmoco());
        entity.setHorarioFimAlmoco(form.getHorarioFimAlmoco());
        entity.setDuracaoSessaoMinutos(form.getDuracaoSessaoMinutos());
        entity.setTimezoneId(normalizarTimezone(form.getTimezoneId()));
        entity.setTelefoneWhatsapp(normalizarTelefone(form.getTelefoneWhatsapp()));
        entity.setEmailContato(normalizarEmail(form.getEmailContato()));
        entity.setAtivo(true);

        ConfiguracaoClinicaEntity salva = configuracaoClinicaRepository.save(entity);
        ClinicTimezoneHolder.update(salva.getTimezoneId());
        if (novoCadastro) {
            auditService.recordCreate(
                    AuditModule.CONFIGURACOES,
                    "configuracao_clinica",
                    salva,
                    null,
                    "Cadastro da configuração operacional da clínica.",
                    AuditEventOrigin.MANUAL
            );
        } else {
            auditService.recordManual(
                    AuditModule.CONFIGURACOES,
                    "configuracao_clinica",
                    salva.getId().toString(),
                    salva.getNomeClinica(),
                    AuditActionType.UPDATE,
                    snapshotAnterior,
                    auditService.snapshotOf(salva),
                    null,
                    "Atualização da configuração operacional da clínica.",
                    AuditEventOrigin.MANUAL
            );
        }
        return salva;
    }

    public List<TimezoneOption> listarTimezonesSuportados() {
        return TIMEZONES_SUPORTADOS;
    }

    private ConfiguracaoClinicaEntity buscarAtivaOuPadrao() {
        return configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc()
                .orElseGet(this::novaConfiguracaoPadrao);
    }

    private ConfiguracaoClinicaEntity resolverEntidade(String id) {
        if (id == null || id.isBlank()) {
            return configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc()
                    .orElseGet(this::novaConfiguracaoPadrao);
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Configuração da clínica inválida.");
        }
        return configuracaoClinicaRepository.findById(uuid)
                .orElseThrow(() -> new BusinessException("Configuração da clínica não encontrada."));
    }

    private void validarFormulario(ConfiguracaoClinicaForm form) {
        if (form.getHorarioAbertura() != null
                && form.getHorarioFechamento() != null
                && !form.getHorarioAbertura().isBefore(form.getHorarioFechamento())) {
            throw new BusinessException("O horário de abertura deve ser anterior ao horário de fechamento.");
        }

        boolean almocoInformado = form.getHorarioInicioAlmoco() != null || form.getHorarioFimAlmoco() != null;
        if (almocoInformado && (form.getHorarioInicioAlmoco() == null || form.getHorarioFimAlmoco() == null)) {
            throw new BusinessException("Preencha o início e o fim do intervalo de almoço.");
        }

        if (form.getHorarioInicioAlmoco() != null && form.getHorarioFimAlmoco() != null) {
            if (!form.getHorarioInicioAlmoco().isBefore(form.getHorarioFimAlmoco())) {
                throw new BusinessException("O início do almoço deve ser anterior ao fim do almoço.");
            }
            if (form.getHorarioAbertura() != null && form.getHorarioInicioAlmoco().isBefore(form.getHorarioAbertura())) {
                throw new BusinessException("O almoço não pode começar antes da abertura da clínica.");
            }
            if (form.getHorarioFechamento() != null && form.getHorarioFimAlmoco().isAfter(form.getHorarioFechamento())) {
                throw new BusinessException("O almoço não pode terminar após o fechamento da clínica.");
            }
        }

        if (form.getDuracaoSessaoMinutos() != null) {
            int jornadaMinutos = calcularJornadaEmMinutos(form.getHorarioAbertura(), form.getHorarioFechamento());
            if (jornadaMinutos > 0 && form.getDuracaoSessaoMinutos() > jornadaMinutos) {
                throw new BusinessException("A duração da sessão não pode ser maior do que a jornada diária.");
            }
        }

        normalizarTimezone(form.getTimezoneId());
        normalizarTelefone(form.getTelefoneWhatsapp());
        normalizarEmail(form.getEmailContato());
    }

    private int calcularJornadaEmMinutos(LocalTime abertura, LocalTime fechamento) {
        if (abertura == null || fechamento == null || !abertura.isBefore(fechamento)) {
            return 0;
        }
        return (int) java.time.Duration.between(abertura, fechamento).toMinutes();
    }

    private ConfiguracaoClinicaForm toForm(ConfiguracaoClinicaEntity entity) {
        ConfiguracaoClinicaForm form = new ConfiguracaoClinicaForm();
        if (entity.getId() != null) {
            form.setId(entity.getId().toString());
        }
        form.setNomeClinica(entity.getNomeClinica());
        form.setDiasFuncionamento(new LinkedHashSet<>(entity.diasFuncionamentoSet()));
        form.setHorarioAbertura(entity.getHorarioAbertura());
        form.setHorarioFechamento(entity.getHorarioFechamento());
        form.setHorarioInicioAlmoco(entity.getHorarioInicioAlmoco());
        form.setHorarioFimAlmoco(entity.getHorarioFimAlmoco());
        form.setDuracaoSessaoMinutos(entity.getDuracaoSessaoMinutos());
        form.setTimezoneId(entity.getTimezoneId());
        form.setTelefoneWhatsapp(entity.getTelefoneWhatsapp());
        form.setEmailContato(entity.getEmailContato());
        return form;
    }

    private ConfiguracaoClinicaEntity novaConfiguracaoPadrao() {
        return ConfiguracaoClinicaEntity.builder()
                .nomeClinica("Clínica FisioGestao")
                .diasFuncionamento("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
                .horarioAbertura(LocalTime.of(7, 0))
                .horarioFechamento(LocalTime.of(19, 0))
                .horarioInicioAlmoco(LocalTime.of(12, 0))
                .horarioFimAlmoco(LocalTime.of(13, 0))
                .duracaoSessaoMinutos(60)
                .timezoneId(TimezoneConfig.DEFAULT_TIMEZONE_ID)
                .ativo(true)
                .build();
    }

    private String serializarDias(Set<DayOfWeek> diasFuncionamento) {
        if (diasFuncionamento == null || diasFuncionamento.isEmpty()) {
            throw new BusinessException("Selecione ao menos um dia de funcionamento.");
        }
        return diasFuncionamento.stream()
                .sorted(Comparator.comparingInt(DayOfWeek::getValue))
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    private String textoObrigatorio(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String normalizarTimezone(String timezoneId) {
        String timezone = textoObrigatorio(timezoneId, "Selecione o fuso horário da clínica.");
        try {
            ZoneId.of(timezone);
            return timezone;
        } catch (Exception ex) {
            throw new BusinessException("Selecione um fuso horário válido para a clínica.");
        }
    }

    private String normalizarTelefone(String telefoneWhatsapp) {
        if (telefoneWhatsapp == null || telefoneWhatsapp.isBlank()) {
            return null;
        }
        String digits = telefoneWhatsapp.replaceAll("\\D", "");
        if (digits.length() < 10) {
            throw new BusinessException("Informe um telefone/WhatsApp válido com DDD.");
        }
        if (digits.length() > 20) {
            throw new BusinessException("O telefone/WhatsApp informado é muito longo.");
        }
        return digits;
    }

    private String normalizarEmail(String emailContato) {
        if (emailContato == null || emailContato.isBlank()) {
            return null;
        }
        String email = emailContato.trim().toLowerCase(Locale.ROOT);
        if (!email.contains("@") || email.startsWith("@") || email.endsWith("@")) {
            throw new BusinessException("Informe um e-mail de contato válido.");
        }
        return email;
    }

    public record TimezoneOption(String id, String label) {
    }
}
