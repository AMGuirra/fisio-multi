package com.fisiogestao.agenda.service;

import com.fisiogestao.agenda.BloqueioAgendaEntity;
import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.HorarioBloqueadoEntity;
import com.fisiogestao.agenda.HorarioBloqueadoStatus;
import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.dto.SlotDisponivelDto;
import com.fisiogestao.agenda.form.AgendaManualForm;
import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.agenda.repository.HorarioBloqueadoRepository;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.email.service.AgendamentoEmailNotificationService;
import com.fisiogestao.pacientes.PacoteSessaoEntity;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.pacientes.service.PacoteSessaoService;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgendaService {

    private static final List<SessaoStatus> STATUS_SESSAO_OCUPADA = List.of(SessaoStatus.AGENDADA, SessaoStatus.REALIZADA);
    private static final List<HorarioBloqueadoStatus> STATUS_BLOQUEIO_OCUPADO = List.of(
            HorarioBloqueadoStatus.PENDENTE,
            HorarioBloqueadoStatus.CONFIRMADO
    );

    private final ConfiguracaoClinicaRepository configuracaoClinicaRepository;
    private final SessaoRepository sessaoRepository;
    private final HorarioBloqueadoRepository horarioBloqueadoRepository;
    private final PacienteService pacienteService;
    private final PacoteSessaoService pacoteSessaoService;
    private final AgendamentoEmailNotificationService agendamentoEmailNotificationService;
    private final BloqueioAgendaService bloqueioAgendaService;
    private final AuditService auditService;

    public record IntervaloAgenda(OffsetDateTime inicio, OffsetDateTime fim) {
    }

    public ConfiguracaoClinicaEntity obterConfiguracaoAtiva() {
        return configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc()
                .orElseThrow(() -> new BusinessException("Configure a clínica antes de usar a agenda."));
    }

    public List<SlotDisponivelDto> gerarSlotsDisponiveis(LocalDate data) {
        return gerarSlotsDisponiveis(data, true);
    }

    public List<SlotDisponivelDto> gerarSlotsDisponiveis(LocalDate data, boolean filtrarPassados) {
        ConfiguracaoClinicaEntity configuracao = obterConfiguracaoAtiva();
        if (!configuracao.diasFuncionamentoSet().contains(data.getDayOfWeek())) {
            return List.of();
        }

        IntervaloAgenda janelaDia = janelaDoDia(data, configuracao);
        List<SessaoEntity> sessoes = sessaoRepository.findByDataHoraInicioBetweenOrderByDataHoraInicioAsc(
                janelaDia.inicio(), janelaDia.fim()
        );
        List<HorarioBloqueadoEntity> bloqueiosTemporarios = horarioBloqueadoRepository
                .findByStatusInAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
                        STATUS_BLOQUEIO_OCUPADO,
                        janelaDia.fim(),
                        janelaDia.inicio()
                );
        List<BloqueioAgendaEntity> bloqueiosAgenda = bloqueioAgendaService.listarAtivosDoDia(data);

        ZoneId zoneId = zoneId(configuracao);
        OffsetDateTime agora = OffsetDateTime.now(zoneId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        java.util.ArrayList<SlotDisponivelDto> slots = new java.util.ArrayList<>();
        LocalTime cursor = configuracao.getHorarioAbertura();
        while (!cursor.plusMinutes(configuracao.getDuracaoSessaoMinutos()).isAfter(configuracao.getHorarioFechamento())) {
            LocalTime fim = cursor.plusMinutes(configuracao.getDuracaoSessaoMinutos());
            OffsetDateTime inicioSlot = data.atTime(cursor).atZone(zoneId).toOffsetDateTime();
            OffsetDateTime fimSlot = data.atTime(fim).atZone(zoneId).toOffsetDateTime();

            if (filtrarPassados && inicioSlot.isBefore(agora)) {
                cursor = fim;
                continue;
            }
            if (intersectaAlmoco(cursor, fim, configuracao)) {
                cursor = fim;
                continue;
            }

            boolean ocupadoPorSessao = sessoes.stream()
                    .filter(sessao -> STATUS_SESSAO_OCUPADA.contains(sessao.getStatus()))
                    .anyMatch(sessao -> overlap(inicioSlot, fimSlot, sessao.getDataHoraInicio(), sessao.getDataHoraFim()));
            boolean ocupadoPorBloqueioTemporario = bloqueiosTemporarios.stream()
                    .anyMatch(bloqueio -> overlap(inicioSlot, fimSlot, bloqueio.getDataHoraInicio(), bloqueio.getDataHoraFim()));
            boolean ocupadoPorBloqueioAgenda = bloqueiosAgenda.stream()
                    .anyMatch(bloqueio -> overlap(inicioSlot, fimSlot, bloqueio.getDataHoraInicio(), bloqueio.getDataHoraFim()));

            if (!ocupadoPorSessao && !ocupadoPorBloqueioTemporario && !ocupadoPorBloqueioAgenda) {
                slots.add(new SlotDisponivelDto(
                        cursor.format(formatter),
                        fim.format(formatter),
                        cursor.format(formatter)
                ));
            }

            cursor = fim;
        }
        return slots;
    }

    public IntervaloAgenda montarIntervalo(LocalDate data, LocalTime horarioInicio) {
        ConfiguracaoClinicaEntity configuracao = obterConfiguracaoAtiva();
        ZoneId zoneId = zoneId(configuracao);
        ZonedDateTime inicioZoned = data.atTime(horarioInicio).atZone(zoneId);
        OffsetDateTime inicio = inicioZoned.toOffsetDateTime();
        OffsetDateTime fim = inicio.plusMinutes(configuracao.getDuracaoSessaoMinutos());
        return new IntervaloAgenda(inicio, fim);
    }

    public void validarHorarioDisponivel(
            OffsetDateTime inicio,
            OffsetDateTime fim,
            UUID horarioBloqueadoIgnoradoId,
            UUID sessaoIgnoradaId
    ) {
        validarHorarioDisponivel(inicio, fim, horarioBloqueadoIgnoradoId, sessaoIgnoradaId, false);
    }

    public void validarHorarioDisponivel(
            OffsetDateTime inicio,
            OffsetDateTime fim,
            UUID horarioBloqueadoIgnoradoId,
            UUID sessaoIgnoradaId,
            boolean permitirHorarioPassado
    ) {
        ConfiguracaoClinicaEntity configuracao = obterConfiguracaoAtiva();
        if (!inicio.isBefore(fim)) {
            throw new BusinessException("O intervalo da agenda é inválido.");
        }

        ZoneId zoneId = zoneId(configuracao);
        if (!configuracao.diasFuncionamentoSet().contains(inicio.atZoneSameInstant(zoneId).getDayOfWeek())) {
            throw new BusinessException("A clínica não funciona nesse dia.");
        }

        if (!permitirHorarioPassado && inicio.isBefore(OffsetDateTime.now(zoneId))) {
            throw new BusinessException("Não é permitido agendar horários no passado.");
        }

        LocalTime horaInicio = inicio.atZoneSameInstant(zoneId).toLocalTime();
        LocalTime horaFim = fim.atZoneSameInstant(zoneId).toLocalTime();
        if (horaInicio.isBefore(configuracao.getHorarioAbertura()) || horaFim.isAfter(configuracao.getHorarioFechamento())) {
            throw new BusinessException("O horário está fora do expediente da clínica.");
        }
        if (intersectaAlmoco(horaInicio, horaFim, configuracao)) {
            throw new BusinessException("O horário escolhido está dentro do intervalo de almoço.");
        }

        List<SessaoEntity> conflitosSessao = sessaoRepository.findByStatusInAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
                STATUS_SESSAO_OCUPADA,
                fim,
                inicio
        );
        boolean conflitoSessao = conflitosSessao.stream()
                .anyMatch(sessao -> sessaoIgnoradaId == null || !sessaoIgnoradaId.equals(sessao.getId()));
        if (conflitoSessao) {
            throw new BusinessException("Já existe sessão confirmada nesse horário.");
        }

        List<HorarioBloqueadoEntity> conflitosBloqueio = horarioBloqueadoRepository
                .findByStatusInAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
                        STATUS_BLOQUEIO_OCUPADO,
                        fim,
                        inicio
                );
        boolean conflitoBloqueioTemporario = conflitosBloqueio.stream()
                .anyMatch(bloqueio -> horarioBloqueadoIgnoradoId == null || !horarioBloqueadoIgnoradoId.equals(bloqueio.getId()));
        if (conflitoBloqueioTemporario) {
            throw new BusinessException("Esse horário acabou de ser reservado por outro atendimento.");
        }

        boolean conflitoBloqueioAgenda = bloqueioAgendaService.existeBloqueioAtivo(inicio, fim, null);
        if (conflitoBloqueioAgenda) {
            throw new BusinessException("Este horário está indisponível por bloqueio da clínica.");
        }
    }

    public List<SessaoEntity> listarSessoesDoDia(LocalDate data) {
        IntervaloAgenda janela = janelaDoDia(data, obterConfiguracaoAtiva());
        return sessaoRepository.findByDataHoraInicioBetweenOrderByDataHoraInicioAsc(janela.inicio(), janela.fim());
    }

    public List<SessaoEntity> listarSessoesDoPaciente(UUID clienteId) {
        return sessaoRepository.findByClienteIdOrderByDataHoraInicioDesc(clienteId);
    }

    public SessaoEntity buscarSessao(UUID sessaoId) {
        return sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new BusinessException("Sessão não encontrada."));
    }

    public List<SessaoEntity> listarSessoesNoPeriodo(LocalDate inicioInclusive, LocalDate fimExclusive) {
        ConfiguracaoClinicaEntity configuracao = obterConfiguracaoAtiva();
        ZoneId zoneId = zoneId(configuracao);
        OffsetDateTime inicio = inicioInclusive.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime fim = fimExclusive.atStartOfDay(zoneId).toOffsetDateTime();
        return sessaoRepository.findByDataHoraInicioBetweenOrderByDataHoraInicioAsc(inicio, fim);
    }

    public List<HorarioBloqueadoEntity> listarHorariosBloqueadosDoDia(LocalDate data) {
        IntervaloAgenda janela = janelaDoDia(data, obterConfiguracaoAtiva());
        return horarioBloqueadoRepository.findByStatusInAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
                STATUS_BLOQUEIO_OCUPADO,
                janela.fim(),
                janela.inicio()
        );
    }

    public List<BloqueioAgendaEntity> listarBloqueiosAgendaDoDia(LocalDate data) {
        return bloqueioAgendaService.listarAtivosDoDia(data);
    }

    public List<BloqueioAgendaEntity> listarBloqueiosAgendaNoPeriodo(LocalDate inicioInclusive, LocalDate fimExclusive) {
        return bloqueioAgendaService.listarAtivosNoPeriodo(inicioInclusive, fimExclusive);
    }

    @Transactional
    public SessaoEntity agendarSessaoManual(AgendaManualForm form, String profissionalResponsavel) {
        IntervaloAgenda intervalo = montarIntervalo(form.getData(), form.getHorario());
        boolean permitirRetroativo = true;
        validarHorarioDisponivel(intervalo.inicio(), intervalo.fim(), null, null, permitirRetroativo);

        PacoteSessaoEntity pacote = pacoteSessaoService.resolverPacoteParaSessao(form.getClienteId(), form.getPacoteSessaoId(), null);
        pacoteSessaoService.reservarSessao(pacote.getId());

        SessaoEntity sessao = SessaoEntity.builder()
                .cliente(pacienteService.buscarPorId(form.getClienteId()))
                .pacoteSessao(pacote)
                .dataHoraInicio(intervalo.inicio())
                .dataHoraFim(intervalo.fim())
                .status(SessaoStatus.AGENDADA)
                .tipoAtendimento(texto(form.getTipoAtendimento()))
                .observacoes(texto(form.getObservacoes()))
                .pacoteConsumoRegistrado(Boolean.TRUE)
                .build();
        SessaoEntity salva = sessaoRepository.save(sessao);
        auditService.recordCreate(
                AuditModule.AGENDA,
                "sessao",
                salva,
                null,
                "Agendamento manual criado na agenda.",
                AuditEventOrigin.MANUAL
        );
        agendamentoEmailNotificationService.notificarConfirmadoPorSessao(salva, profissionalResponsavel);
        return salva;
    }

    @Transactional
    public SessaoEntity atualizarSessaoManual(UUID sessaoId, AgendaManualForm form, String profissionalResponsavel) {
        SessaoEntity sessao = buscarSessao(sessaoId);
        var snapshotAnterior = auditService.snapshotOf(sessao);

        if (sessao.getStatus() == SessaoStatus.CANCELADA) {
            throw new BusinessException("Sessões canceladas não podem ser editadas. Marque como realizada se quiser reativar o atendimento.");
        }

        OffsetDateTime inicioAnterior = sessao.getDataHoraInicio();
        OffsetDateTime fimAnterior = sessao.getDataHoraFim();
        UUID pacoteAnteriorId = sessao.getPacoteSessao() != null ? sessao.getPacoteSessao().getId() : null;
        boolean consumoAnteriorRegistrado = Boolean.TRUE.equals(sessao.getPacoteConsumoRegistrado());
        IntervaloAgenda intervalo = montarIntervalo(form.getData(), form.getHorario());
        boolean permitirRetroativo = sessao.getStatus() == SessaoStatus.REALIZADA || !form.getData().isAfter(LocalDate.now());
        validarHorarioDisponivel(
                intervalo.inicio(),
                intervalo.fim(),
                null,
                sessaoId,
                permitirRetroativo
        );

        PacoteSessaoEntity novoPacote = pacoteSessaoService.resolverPacoteParaSessao(
                form.getClienteId(),
                form.getPacoteSessaoId(),
                pacoteAnteriorId
        );
        sincronizarPacoteDaSessao(sessao, novoPacote, pacoteAnteriorId, consumoAnteriorRegistrado);

        sessao.setCliente(pacienteService.buscarPorId(form.getClienteId()));
        sessao.setPacoteSessao(novoPacote);
        sessao.setDataHoraInicio(intervalo.inicio());
        sessao.setDataHoraFim(intervalo.fim());
        sessao.setTipoAtendimento(texto(form.getTipoAtendimento()));
        sessao.setObservacoes(texto(form.getObservacoes()));

        SessaoEntity salva = sessaoRepository.save(sessao);
        AuditActionType actionType = (!inicioAnterior.equals(salva.getDataHoraInicio()) || !fimAnterior.equals(salva.getDataHoraFim()))
                ? AuditActionType.RESCHEDULE
                : AuditActionType.UPDATE;
        auditService.recordManual(
                AuditModule.AGENDA,
                "sessao",
                salva.getId().toString(),
                auditService.describeOf(salva),
                actionType,
                snapshotAnterior,
                auditService.snapshotOf(salva),
                null,
                actionType == AuditActionType.RESCHEDULE ? "Reagendamento manual de sessão." : "Edição manual de sessão.",
                AuditEventOrigin.MANUAL
        );
        if (!inicioAnterior.equals(salva.getDataHoraInicio()) || !fimAnterior.equals(salva.getDataHoraFim())) {
            agendamentoEmailNotificationService.notificarReagendado(salva, inicioAnterior, fimAnterior, profissionalResponsavel);
        }
        return salva;
    }

    @Transactional
    public SessaoEntity marcarComoRealizada(UUID sessaoId, String observacoes) {
        SessaoEntity sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new BusinessException("Sessão não encontrada."));
        var snapshotAnterior = auditService.snapshotOf(sessao);

        if (sessao.getStatus() == SessaoStatus.REALIZADA) {
            return sessao;
        }

        if (sessao.getPacoteSessao() != null && !Boolean.TRUE.equals(sessao.getPacoteConsumoRegistrado())) {
            pacoteSessaoService.reservarSessao(sessao.getPacoteSessao().getId());
            sessao.setPacoteConsumoRegistrado(Boolean.TRUE);
        }

        sessao.setStatus(SessaoStatus.REALIZADA);
        if (texto(observacoes) != null) {
            sessao.setObservacoes(texto(observacoes));
        }
        SessaoEntity salva = sessaoRepository.save(sessao);
        auditService.recordManual(
                AuditModule.AGENDA,
                "sessao",
                salva.getId().toString(),
                auditService.describeOf(salva),
                AuditActionType.COMPLETE,
                snapshotAnterior,
                auditService.snapshotOf(salva),
                null,
                "Sessão marcada como realizada.",
                AuditEventOrigin.MANUAL
        );
        return salva;
    }

    @Transactional
    public SessaoEntity reagendarSessao(UUID sessaoId, LocalDate data, LocalTime horario, String profissionalResponsavel) {
        SessaoEntity sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new BusinessException("Sessão não encontrada."));
        var snapshotAnterior = auditService.snapshotOf(sessao);

        if (sessao.getStatus() != SessaoStatus.AGENDADA) {
            throw new BusinessException("Apenas sessões agendadas podem ser reagendadas.");
        }

        OffsetDateTime inicioAnterior = sessao.getDataHoraInicio();
        OffsetDateTime fimAnterior = sessao.getDataHoraFim();
        IntervaloAgenda intervalo = montarIntervalo(data, horario);
        validarHorarioDisponivel(intervalo.inicio(), intervalo.fim(), null, sessaoId);

        sessao.setDataHoraInicio(intervalo.inicio());
        sessao.setDataHoraFim(intervalo.fim());
        SessaoEntity salva = sessaoRepository.save(sessao);
        auditService.recordManual(
                AuditModule.AGENDA,
                "sessao",
                salva.getId().toString(),
                auditService.describeOf(salva),
                AuditActionType.RESCHEDULE,
                snapshotAnterior,
                auditService.snapshotOf(salva),
                null,
                "Sessão reagendada.",
                AuditEventOrigin.MANUAL
        );
        agendamentoEmailNotificationService.notificarReagendado(salva, inicioAnterior, fimAnterior, profissionalResponsavel);
        return salva;
    }

    @Transactional
    public SessaoEntity cancelarSessao(UUID sessaoId, String observacoes) {
        SessaoEntity sessao = buscarSessao(sessaoId);
        var snapshotAnterior = auditService.snapshotOf(sessao);
        if (sessao.getStatus() == SessaoStatus.CANCELADA) {
            return sessao;
        }
        if (sessao.getPacoteSessao() != null && Boolean.TRUE.equals(sessao.getPacoteConsumoRegistrado())) {
            pacoteSessaoService.estornarSessao(sessao.getPacoteSessao().getId());
            sessao.setPacoteConsumoRegistrado(Boolean.FALSE);
        }
        sessao.setStatus(SessaoStatus.CANCELADA);
        if (texto(observacoes) != null) {
            String historico = texto(sessao.getObservacoes());
            sessao.setObservacoes(historico == null
                    ? "Cancelamento: " + texto(observacoes)
                    : (historico + System.lineSeparator() + "Cancelamento: " + texto(observacoes)));
        }
        SessaoEntity salva = sessaoRepository.save(sessao);
        auditService.recordManual(
                AuditModule.AGENDA,
                "sessao",
                salva.getId().toString(),
                auditService.describeOf(salva),
                AuditActionType.CANCEL,
                snapshotAnterior,
                auditService.snapshotOf(salva),
                texto(observacoes),
                "Sessão cancelada.",
                AuditEventOrigin.MANUAL
        );
        return salva;
    }

    private IntervaloAgenda janelaDoDia(LocalDate data, ConfiguracaoClinicaEntity configuracao) {
        ZoneId zoneId = zoneId(configuracao);
        OffsetDateTime inicio = data.atStartOfDay(zoneId).toOffsetDateTime();
        return new IntervaloAgenda(inicio, inicio.plusDays(1));
    }

    private ZoneId zoneId(ConfiguracaoClinicaEntity configuracao) {
        return ZoneId.of(configuracao.getTimezoneId());
    }

    private boolean overlap(OffsetDateTime inicioA, OffsetDateTime fimA, OffsetDateTime inicioB, OffsetDateTime fimB) {
        return inicioA.isBefore(fimB) && fimA.isAfter(inicioB);
    }

    private boolean intersectaAlmoco(LocalTime inicio, LocalTime fim, ConfiguracaoClinicaEntity configuracao) {
        if (configuracao.getHorarioInicioAlmoco() == null || configuracao.getHorarioFimAlmoco() == null) {
            return false;
        }
        return inicio.isBefore(configuracao.getHorarioFimAlmoco())
                && fim.isAfter(configuracao.getHorarioInicioAlmoco());
    }

    private void sincronizarPacoteDaSessao(
            SessaoEntity sessao,
            PacoteSessaoEntity novoPacote,
            UUID pacoteAnteriorId,
            boolean consumoAnteriorRegistrado
    ) {
        UUID novoPacoteId = novoPacote != null ? novoPacote.getId() : null;
        boolean mudouPacote = pacoteAnteriorId == null
                ? novoPacoteId != null
                : !pacoteAnteriorId.equals(novoPacoteId);

        if (consumoAnteriorRegistrado && pacoteAnteriorId != null && mudouPacote) {
            pacoteSessaoService.estornarSessao(pacoteAnteriorId);
            sessao.setPacoteConsumoRegistrado(Boolean.FALSE);
        }

        if (novoPacoteId != null && (!consumoAnteriorRegistrado || mudouPacote)) {
            pacoteSessaoService.reservarSessao(novoPacoteId);
            sessao.setPacoteConsumoRegistrado(Boolean.TRUE);
        }
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
