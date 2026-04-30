package com.fisiogestao.agenda.service;

import com.fisiogestao.agenda.BloqueioAgendaEntity;
import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.HorarioBloqueadoEntity;
import com.fisiogestao.agenda.HorarioBloqueadoStatus;
import com.fisiogestao.agenda.RecorrenciaEntity;
import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.TipoAgendamento;
import com.fisiogestao.agenda.dto.SlotDisponivelDto;
import com.fisiogestao.agenda.form.AgendaManualForm;
import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.agenda.repository.HorarioBloqueadoRepository;
import com.fisiogestao.agenda.repository.RecorrenciaRepository;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.email.service.AgendamentoEmailNotificationService;
import com.fisiogestao.pacientes.PacoteSessaoEntity;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.pacientes.service.PacoteSessaoService;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AgendaService {

    private static final List<SessaoStatus> STATUS_SESSAO_OCUPADA = List.of(SessaoStatus.AGENDADA, SessaoStatus.REALIZADA);
    private static final List<HorarioBloqueadoStatus> STATUS_BLOQUEIO_OCUPADO = List.of(
            HorarioBloqueadoStatus.PENDENTE,
            HorarioBloqueadoStatus.CONFIRMADO
    );

    private final ConfiguracaoClinicaRepository configuracaoClinicaRepository;
    private final SessaoRepository sessaoRepository;
    private final HorarioBloqueadoRepository horarioBloqueadoRepository;
    private final RecorrenciaRepository recorrenciaRepository;
    private final PacienteService pacienteService;
    private final PacoteSessaoService pacoteSessaoService;
    private final AgendamentoEmailNotificationService agendamentoEmailNotificationService;
    private final BloqueioAgendaService bloqueioAgendaService;
    private final RecorrenciaService recorrenciaService;
    private final AuditService auditService;

    public record IntervaloAgenda(OffsetDateTime inicio, OffsetDateTime fim) {
    }

    public ConfiguracaoClinicaEntity obterConfiguracaoAtiva() {
        return configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc()
                .orElseThrow(() -> new BusinessException("Configure a clínica antes de usar a agenda."));
    }

    public List<SlotDisponivelDto> gerarSlotsDisponiveis(LocalDate data) {
        return gerarSlotsDisponiveis(data, true, 0);
    }

    public List<SlotDisponivelDto> gerarSlotsDisponiveis(LocalDate data, boolean filtrarPassados) {
        return gerarSlotsDisponiveis(data, filtrarPassados, 0);
    }

    public List<SlotDisponivelDto> gerarSlotsDisponiveis(LocalDate data, boolean filtrarPassados, int gapMinutos) {
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

        ArrayList<SlotDisponivelDto> slots = new ArrayList<>();
        LocalTime cursor = configuracao.getHorarioAbertura();
        // When gap is set (public API), snap cursor to next full hour so slots land on 07:00, 08:00, 09:00...
        if (gapMinutos > 0 && cursor.getMinute() != 0) {
            cursor = cursor.withMinute(0).withSecond(0).withNano(0).plusHours(1);
        }
        while (!cursor.plusMinutes(configuracao.getDuracaoSessaoMinutos()).isAfter(configuracao.getHorarioFechamento())) {
            LocalTime fim = cursor.plusMinutes(configuracao.getDuracaoSessaoMinutos());
            OffsetDateTime inicioSlot = data.atTime(cursor).atZone(zoneId).toOffsetDateTime();
            OffsetDateTime fimSlot = data.atTime(fim).atZone(zoneId).toOffsetDateTime();

            if (filtrarPassados && inicioSlot.isBefore(agora)) {
                cursor = fim.plusMinutes(gapMinutos);
                continue;
            }
            if (intersectaAlmoco(cursor, fim, configuracao)) {
                cursor = fim.plusMinutes(gapMinutos);
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

            cursor = fim.plusMinutes(gapMinutos);
            // Keep full-hour alignment on each step
            if (gapMinutos > 0 && cursor.getMinute() != 0) {
                cursor = cursor.withMinute(0).withSecond(0).withNano(0).plusHours(1);
            }
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
        if (Boolean.TRUE.equals(form.getRecorrente())) {
            List<SessaoEntity> sessoes = agendarSessoesRecorrentes(form, profissionalResponsavel);
            return sessoes.get(0);
        }

        IntervaloAgenda intervalo = montarIntervalo(form.getData(), form.getHorario());
        boolean permitirRetroativo = true;
        validarHorarioDisponivel(intervalo.inicio(), intervalo.fim(), null, null, permitirRetroativo);

        TipoAgendamento tipoAgendamento = tipoAgendamento(form);
        PacoteSessaoEntity pacote = resolverPacoteParaCriacao(form, tipoAgendamento);
        if (tipoAgendamento == TipoAgendamento.PACOTE) {
            pacoteSessaoService.reservarSessao(pacote.getId());
        }

        SessaoEntity sessao = SessaoEntity.builder()
                .cliente(pacienteService.buscarPorId(form.getClienteId()))
                .pacoteSessao(pacote)
                .dataHoraInicio(intervalo.inicio())
                .dataHoraFim(intervalo.fim())
                .status(SessaoStatus.AGENDADA)
                .tipoAtendimento(texto(form.getTipoAtendimento()))
                .tipoAgendamento(tipoAgendamento)
                .valorAvulso(tipoAgendamento == TipoAgendamento.AVULSO ? form.getValorAvulso() : null)
                .observacoes(texto(form.getObservacoes()))
                .pacoteConsumoRegistrado(tipoAgendamento == TipoAgendamento.PACOTE)
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
    public List<SessaoEntity> agendarSessoesRecorrentes(AgendaManualForm form, String profissionalResponsavel) {
        TipoAgendamento tipoAgendamento = tipoAgendamento(form);
        PacoteSessaoEntity pacote = resolverPacoteParaCriacao(form, tipoAgendamento);
        List<OffsetDateTime> inicios = gerarIniciosRecorrentes(form).stream()
                .sorted()
                .toList();
        if (inicios.isEmpty()) {
            throw new BusinessException("A regra de recorrência não gerou nenhuma ocorrência.");
        }

        for (OffsetDateTime inicio : inicios) {
            OffsetDateTime fim = inicio.plusMinutes(duracaoDaSessao(form));
            validarHorarioDisponivel(inicio, fim, null, null, true);
        }

        RecorrenciaEntity recorrencia = recorrenciaRepository.save(RecorrenciaEntity.builder()
                .frequencia(form.getRecorrencia().getFrequencia())
                .diasSemana(diasSemanaComoTexto(form))
                .intervalo(form.getRecorrencia().getIntervalo() == null ? 1 : form.getRecorrencia().getIntervalo())
                .dataFim(form.getRecorrencia().getDataFim())
                .maxOcorrencias(form.getRecorrencia().getMaxOcorrencias() == null ? 52 : form.getRecorrencia().getMaxOcorrencias())
                .build());

        ArrayList<SessaoEntity> salvas = new ArrayList<>();
        SessaoEntity pai = null;
        for (OffsetDateTime inicio : inicios) {
            if (tipoAgendamento == TipoAgendamento.PACOTE) {
                pacoteSessaoService.reservarSessao(pacote.getId());
            }

            SessaoEntity sessao = SessaoEntity.builder()
                    .cliente(pacienteService.buscarPorId(form.getClienteId()))
                    .pacoteSessao(pacote)
                    .dataHoraInicio(inicio)
                    .dataHoraFim(inicio.plusMinutes(duracaoDaSessao(form)))
                    .status(SessaoStatus.AGENDADA)
                    .tipoAtendimento(texto(form.getTipoAtendimento()))
                    .tipoAgendamento(tipoAgendamento)
                    .valorAvulso(tipoAgendamento == TipoAgendamento.AVULSO ? form.getValorAvulso() : null)
                    .recorrencia(pai == null ? recorrencia : null)
                    .recorrenciaPai(pai)
                    .observacoes(texto(form.getObservacoes()))
                    .pacoteConsumoRegistrado(tipoAgendamento == TipoAgendamento.PACOTE)
                    .build();
            SessaoEntity salva = sessaoRepository.save(sessao);
            if (pai == null) {
                pai = salva;
            }
            salvas.add(salva);
            auditService.recordCreate(
                    AuditModule.AGENDA,
                    "sessao",
                    salva,
                    null,
                    "Agendamento recorrente criado na agenda.",
                    AuditEventOrigin.MANUAL
            );
        }

        agendamentoEmailNotificationService.notificarConfirmadoPorSessao(salvas.get(0), profissionalResponsavel);
        return salvas;
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

        TipoAgendamento novoTipo = tipoAgendamento(form);
        PacoteSessaoEntity novoPacote = resolverPacoteParaEdicao(form, novoTipo, pacoteAnteriorId);
        sincronizarPacoteDaSessao(sessao, novoTipo, novoPacote, pacoteAnteriorId, consumoAnteriorRegistrado);

        sessao.setCliente(pacienteService.buscarPorId(form.getClienteId()));
        sessao.setPacoteSessao(novoPacote);
        sessao.setDataHoraInicio(intervalo.inicio());
        sessao.setDataHoraFim(intervalo.fim());
        sessao.setTipoAtendimento(texto(form.getTipoAtendimento()));
        sessao.setTipoAgendamento(novoTipo);
        sessao.setValorAvulso(novoTipo == TipoAgendamento.AVULSO ? form.getValorAvulso() : null);
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
    public SessaoEntity reverterRealizacao(UUID sessaoId) {
        SessaoEntity sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new BusinessException("Sessão não encontrada."));
        var snapshotAnterior = auditService.snapshotOf(sessao);

        if (sessao.getStatus() != SessaoStatus.REALIZADA) {
            throw new BusinessException("Apenas sessões realizadas podem ser revertidas para agendada.");
        }

        sessao.setStatus(SessaoStatus.AGENDADA);
        SessaoEntity salva = sessaoRepository.save(sessao);
        auditService.recordManual(
                AuditModule.AGENDA,
                "sessao",
                salva.getId().toString(),
                auditService.describeOf(salva),
                AuditActionType.RESTORE,
                snapshotAnterior,
                auditService.snapshotOf(salva),
                null,
                "Realização desfeita — sessão reaberta como agendada.",
                AuditEventOrigin.MANUAL
        );
        return salva;
    }

    @Transactional
    public SessaoEntity reabrirSessaoCancelada(UUID sessaoId) {
        SessaoEntity sessao = buscarSessao(sessaoId);
        var snapshotAnterior = auditService.snapshotOf(sessao);

        if (sessao.getStatus() != SessaoStatus.CANCELADA) {
            throw new BusinessException("Apenas sessões canceladas podem voltar para pendente.");
        }

        if (sessao.getPacoteSessao() != null && !Boolean.TRUE.equals(sessao.getPacoteConsumoRegistrado())) {
            pacoteSessaoService.reservarSessao(sessao.getPacoteSessao().getId());
            sessao.setPacoteConsumoRegistrado(Boolean.TRUE);
        }

        sessao.setStatus(SessaoStatus.AGENDADA);
        SessaoEntity salva = sessaoRepository.save(sessao);
        auditService.recordManual(
                AuditModule.AGENDA,
                "sessao",
                salva.getId().toString(),
                auditService.describeOf(salva),
                AuditActionType.RESTORE,
                snapshotAnterior,
                auditService.snapshotOf(salva),
                null,
                "Sessão reaberta como pendente.",
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

    @Transactional
    public void excluirSessao(UUID sessaoId) {
        SessaoEntity sessao = buscarSessao(sessaoId);
        var snapshotAnterior = auditService.snapshotOf(sessao);

        if (sessao.getPacoteSessao() != null && Boolean.TRUE.equals(sessao.getPacoteConsumoRegistrado())) {
            pacoteSessaoService.estornarSessao(sessao.getPacoteSessao().getId());
            sessao.setPacoteConsumoRegistrado(Boolean.FALSE);
        }

        try {
            sessaoRepository.delete(sessao);
            sessaoRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Esta sessão possui registros vinculados e não pode ser excluída.");
        }

        auditService.recordManual(
                AuditModule.AGENDA,
                "sessao",
                sessao.getId().toString(),
                auditService.describeOf(sessao),
                AuditActionType.DELETE_PHYSICAL,
                snapshotAnterior,
                null,
                null,
                "Exclusão física de sessão.",
                AuditEventOrigin.MANUAL
        );
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
            TipoAgendamento novoTipo,
            PacoteSessaoEntity novoPacote,
            UUID pacoteAnteriorId,
            boolean consumoAnteriorRegistrado
    ) {
        if (novoTipo == TipoAgendamento.AVULSO) {
            if (consumoAnteriorRegistrado && pacoteAnteriorId != null) {
                pacoteSessaoService.estornarSessao(pacoteAnteriorId);
            }
            sessao.setPacoteConsumoRegistrado(Boolean.FALSE);
            return;
        }

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

    private TipoAgendamento tipoAgendamento(AgendaManualForm form) {
        return form.getTipoAgendamento() == null ? TipoAgendamento.PACOTE : form.getTipoAgendamento();
    }

    private PacoteSessaoEntity resolverPacoteParaCriacao(AgendaManualForm form, TipoAgendamento tipoAgendamento) {
        validarVinculoAgendamento(form, tipoAgendamento);
        if (tipoAgendamento == TipoAgendamento.AVULSO) {
            return null;
        }
        return pacoteSessaoService.resolverPacoteParaSessao(form.getClienteId(), form.getPacoteSessaoId(), null);
    }

    private PacoteSessaoEntity resolverPacoteParaEdicao(AgendaManualForm form, TipoAgendamento tipoAgendamento, UUID pacoteAtualId) {
        validarVinculoAgendamento(form, tipoAgendamento);
        if (tipoAgendamento == TipoAgendamento.AVULSO) {
            return null;
        }
        return pacoteSessaoService.resolverPacoteParaSessao(form.getClienteId(), form.getPacoteSessaoId(), pacoteAtualId);
    }

    private void validarVinculoAgendamento(AgendaManualForm form, TipoAgendamento tipoAgendamento) {
        if (tipoAgendamento == TipoAgendamento.PACOTE) {
            return;
        }
        if (form.getPacoteSessaoId() != null) {
            throw new BusinessException("Consulta avulsa não deve possuir plano de tratamento vinculado.");
        }
        if (form.getValorAvulso() != null && form.getValorAvulso().signum() < 0) {
            throw new BusinessException("O valor da consulta avulsa não pode ser negativo.");
        }
    }

    private List<OffsetDateTime> gerarIniciosRecorrentes(AgendaManualForm form) {
        ConfiguracaoClinicaEntity configuracao = obterConfiguracaoAtiva();
        ZoneId zoneId = zoneId(configuracao);
        return recorrenciaService.gerarOcorrencias(form.getData().atTime(form.getHorario()), form.getRecorrencia()).stream()
                .map(dataHora -> dataHora.atZone(zoneId).toOffsetDateTime())
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private int duracaoDaSessao(AgendaManualForm form) {
        if (form.getDuracaoMinutos() != null && form.getDuracaoMinutos() > 0) {
            return form.getDuracaoMinutos();
        }
        return obterConfiguracaoAtiva().getDuracaoSessaoMinutos();
    }

    private String diasSemanaComoTexto(AgendaManualForm form) {
        if (form.getRecorrencia().getDiasSemana() == null || form.getRecorrencia().getDiasSemana().isEmpty()) {
            return null;
        }
        return form.getRecorrencia().getDiasSemana().stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }
}
