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
import com.fisiogestao.pacientes.PlanoSessaoEntity;
import com.fisiogestao.financeiro.service.FinanceiroService;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.pacientes.service.PlanoSessaoService;
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
    private final PlanoSessaoService planoSessaoService;
    private final FinanceiroService financeiroService;
    private final AgendamentoEmailNotificationService agendamentoEmailNotificationService;
    private final BloqueioAgendaService bloqueioAgendaService;
    private final RecorrenciaService recorrenciaService;
    private final AuditService auditService;

    public record IntervaloAgenda(OffsetDateTime inicio, OffsetDateTime fim) {
    }

    @Transactional(readOnly = true)
    public ConfiguracaoClinicaEntity obterConfiguracaoAtiva() {
        return configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc()
                .orElseThrow(() -> new BusinessException("Configure a clínica antes de usar a agenda."));
    }

    @Transactional(readOnly = true)
    public List<SlotDisponivelDto> gerarSlotsDisponiveis(LocalDate data) {
        return gerarSlotsDisponiveis(data, true, 0);
    }

    @Transactional(readOnly = true)
    public List<SlotDisponivelDto> gerarSlotsDisponiveis(LocalDate data, boolean filtrarPassados) {
        return gerarSlotsDisponiveis(data, filtrarPassados, 0);
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public List<SessaoEntity> listarSessoesDoDia(LocalDate data) {
        IntervaloAgenda janela = janelaDoDia(data, obterConfiguracaoAtiva());
        return sessaoRepository.findByDataHoraInicioBetweenOrderByDataHoraInicioAsc(janela.inicio(), janela.fim());
    }

    @Transactional(readOnly = true)
    public long contarSessoesDoDia(LocalDate data) {
        IntervaloAgenda janela = janelaDoDia(data, obterConfiguracaoAtiva());
        return sessaoRepository.countByDataHoraInicioBetween(janela.inicio(), janela.fim());
    }

    @Transactional(readOnly = true)
    public List<SessaoEntity> listarSessoesDoPaciente(UUID clienteId) {
        return sessaoRepository.findByClienteIdOrderByDataHoraInicioDesc(clienteId);
    }

    @Transactional(readOnly = true)
    public SessaoEntity buscarSessao(UUID sessaoId) {
        return sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new BusinessException("Sessão não encontrada."));
    }

    @Transactional(readOnly = true)
    public List<SessaoEntity> listarSessoesNoPeriodo(LocalDate inicioInclusive, LocalDate fimExclusive) {
        ConfiguracaoClinicaEntity configuracao = obterConfiguracaoAtiva();
        ZoneId zoneId = zoneId(configuracao);
        OffsetDateTime inicio = inicioInclusive.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime fim = fimExclusive.atStartOfDay(zoneId).toOffsetDateTime();
        return sessaoRepository.findByDataHoraInicioBetweenOrderByDataHoraInicioAsc(inicio, fim);
    }

    @Transactional(readOnly = true)
    public List<HorarioBloqueadoEntity> listarHorariosBloqueadosDoDia(LocalDate data) {
        IntervaloAgenda janela = janelaDoDia(data, obterConfiguracaoAtiva());
        return horarioBloqueadoRepository.findByStatusInAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
                STATUS_BLOQUEIO_OCUPADO,
                janela.fim(),
                janela.inicio()
        );
    }

    @Transactional(readOnly = true)
    public List<BloqueioAgendaEntity> listarBloqueiosAgendaDoDia(LocalDate data) {
        return bloqueioAgendaService.listarAtivosDoDia(data);
    }

    @Transactional(readOnly = true)
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
        PlanoSessaoEntity plano = resolverPlanoParaCriacao(form, tipoAgendamento);

        SessaoEntity sessao = SessaoEntity.builder()
                .cliente(pacienteService.buscarPorId(form.getClienteId()))
                .planoSessao(plano)
                .dataHoraInicio(intervalo.inicio())
                .dataHoraFim(intervalo.fim())
                .status(SessaoStatus.AGENDADA)
                .tipoAtendimento(texto(form.getTipoAtendimento()))
                .tipoAgendamento(tipoAgendamento)
                .valorAvulso(tipoAgendamento == TipoAgendamento.AVULSO ? form.getValorAvulso() : null)
                .observacoes(texto(form.getObservacoes()))
                .planoConsumoRegistrado(Boolean.FALSE)
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
        PlanoSessaoEntity plano = resolverPlanoParaCriacao(form, tipoAgendamento);
        List<OffsetDateTime> inicios = gerarIniciosRecorrentes(form).stream()
                .sorted()
                .toList();
        if (inicios.isEmpty()) {
            throw new BusinessException("A regra de recorrência não gerou nenhuma ocorrência.");
        }

        if (tipoAgendamento == TipoAgendamento.PLANO) {
            int sessoesDisponiveis = planoSessaoService.calcularSessoesDisponiveis(plano)
                    - (int) sessaoRepository.countAgendadasPorPlano(plano.getId());
            if (inicios.size() > sessoesDisponiveis) {
                throw new BusinessException("A recorrencia gera " + inicios.size()
                        + " sessoes, mas o plano selecionado possui apenas "
                        + sessoesDisponiveis + " sessoes disponiveis (descontando as ja agendadas).");
            }
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
            SessaoEntity sessao = SessaoEntity.builder()
                    .cliente(pacienteService.buscarPorId(form.getClienteId()))
                    .planoSessao(plano)
                    .dataHoraInicio(inicio)
                    .dataHoraFim(inicio.plusMinutes(duracaoDaSessao(form)))
                    .status(SessaoStatus.AGENDADA)
                    .tipoAtendimento(texto(form.getTipoAtendimento()))
                    .tipoAgendamento(tipoAgendamento)
                    .valorAvulso(tipoAgendamento == TipoAgendamento.AVULSO ? form.getValorAvulso() : null)
                    .recorrencia(pai == null ? recorrencia : null)
                    .recorrenciaPai(pai)
                    .observacoes(texto(form.getObservacoes()))
                    .planoConsumoRegistrado(Boolean.FALSE)
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
        UUID planoAnteriorId = sessao.getPlanoSessao() != null ? sessao.getPlanoSessao().getId() : null;
        boolean consumoAnteriorRegistrado = Boolean.TRUE.equals(sessao.getPlanoConsumoRegistrado());
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
        PlanoSessaoEntity novoPlano = resolverPlanoParaEdicao(form, novoTipo, planoAnteriorId);
        sincronizarPlanoDaSessao(sessao, novoTipo, novoPlano, planoAnteriorId, consumoAnteriorRegistrado);

        sessao.setCliente(pacienteService.buscarPorId(form.getClienteId()));
        sessao.setPlanoSessao(novoPlano);
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
        return marcarComoRealizada(sessaoId, observacoes, null);
    }

    @Transactional
    public SessaoEntity marcarComoRealizada(UUID sessaoId, String observacoes, String profissionalResponsavel) {
        SessaoEntity sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new BusinessException("Sessão não encontrada."));
        var snapshotAnterior = auditService.snapshotOf(sessao);

        if (sessao.getStatus() == SessaoStatus.REALIZADA) {
            return sessao;
        }

        registrarConsumoPlanoSeNecessario(sessao);

        if (sessao.getTipoAgendamento() == TipoAgendamento.AVULSO) {
            financeiroService.registrarReceitaAvulso(sessao, profissionalResponsavel);
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
    public SessaoEntity registrarFaltaFaturada(UUID sessaoId) {
        return registrarFaltaFaturada(sessaoId, null);
    }

    @Transactional
    public SessaoEntity registrarFaltaFaturada(UUID sessaoId, String profissionalResponsavel) {
        SessaoEntity sessao = buscarSessao(sessaoId);
        var snapshotAnterior = auditService.snapshotOf(sessao);

        if (sessao.getStatus() == SessaoStatus.FALTA_FATURADA) {
            return sessao;
        }
        if (sessao.getStatus() != SessaoStatus.AGENDADA) {
            throw new BusinessException("Somente sessões pendentes podem ser registradas como falta faturada.");
        }

        registrarConsumoPlanoSeNecessario(sessao);
        if (sessao.getTipoAgendamento() == TipoAgendamento.AVULSO) {
            financeiroService.registrarReceitaAvulso(sessao, profissionalResponsavel);
        }

        sessao.setStatus(SessaoStatus.FALTA_FATURADA);
        SessaoEntity salva = sessaoRepository.save(sessao);
        auditService.recordManual(
                AuditModule.AGENDA,
                "sessao",
                salva.getId().toString(),
                auditService.describeOf(salva),
                AuditActionType.NO_SHOW,
                snapshotAnterior,
                auditService.snapshotOf(salva),
                null,
                "Sessão registrada como falta faturada — crédito consumido e receita reconhecida.",
                AuditEventOrigin.MANUAL
        );
        return salva;
    }

    @Transactional
    public SessaoEntity registrarFaltaNaoFaturada(UUID sessaoId, String profissionalResponsavel) {
        SessaoEntity sessao = buscarSessao(sessaoId);
        var snapshotAnterior = auditService.snapshotOf(sessao);

        if (sessao.getStatus() == SessaoStatus.FALTA) {
            return sessao;
        }
        if (sessao.getStatus() != SessaoStatus.AGENDADA) {
            throw new BusinessException("Somente sessões pendentes podem ser registradas como falta.");
        }

        sessao.setStatus(SessaoStatus.FALTA);
        SessaoEntity salva = sessaoRepository.save(sessao);
        auditService.recordManual(
                AuditModule.AGENDA,
                "sessao",
                salva.getId().toString(),
                auditService.describeOf(salva),
                AuditActionType.NO_SHOW,
                snapshotAnterior,
                auditService.snapshotOf(salva),
                null,
                "Sessão registrada como falta — sem cobrança.",
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
        if (sessao.getPlanoSessao() != null && Boolean.TRUE.equals(sessao.getPlanoConsumoRegistrado())) {
            planoSessaoService.estornarSessao(sessao.getPlanoSessao().getId());
            sessao.setPlanoConsumoRegistrado(Boolean.FALSE);
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

        if (sessao.getPlanoSessao() != null && Boolean.TRUE.equals(sessao.getPlanoConsumoRegistrado())) {
            planoSessaoService.estornarSessao(sessao.getPlanoSessao().getId());
            sessao.setPlanoConsumoRegistrado(Boolean.FALSE);
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

    private void sincronizarPlanoDaSessao(
            SessaoEntity sessao,
            TipoAgendamento novoTipo,
            PlanoSessaoEntity novoPlano,
            UUID planoAnteriorId,
            boolean consumoAnteriorRegistrado
    ) {
        if (novoTipo == TipoAgendamento.AVULSO) {
            if (consumoAnteriorRegistrado && planoAnteriorId != null) {
                planoSessaoService.estornarSessao(planoAnteriorId);
            }
            sessao.setPlanoConsumoRegistrado(Boolean.FALSE);
            return;
        }

        UUID novoPlanoId = novoPlano != null ? novoPlano.getId() : null;
        boolean mudouPlano = planoAnteriorId == null
                ? novoPlanoId != null
                : !planoAnteriorId.equals(novoPlanoId);
        boolean consumoEstornado = false;

        if (consumoAnteriorRegistrado && planoAnteriorId != null && mudouPlano) {
            planoSessaoService.estornarSessao(planoAnteriorId);
            sessao.setPlanoConsumoRegistrado(Boolean.FALSE);
            consumoEstornado = true;
        }

        if (novoPlanoId != null && sessaoConsomePlano(sessao) && (!consumoAnteriorRegistrado || mudouPlano)) {
            planoSessaoService.reservarSessaoParaConclusao(novoPlanoId);
            sessao.setPlanoConsumoRegistrado(Boolean.TRUE);
        } else if (!sessaoConsomePlano(sessao)) {
            if (consumoAnteriorRegistrado && planoAnteriorId != null && !consumoEstornado) {
                planoSessaoService.estornarSessao(planoAnteriorId);
            }
            sessao.setPlanoConsumoRegistrado(Boolean.FALSE);
        }
    }

    private boolean sessaoConsomePlano(SessaoEntity sessao) {
        return sessao.getStatus() == SessaoStatus.REALIZADA || sessao.getStatus() == SessaoStatus.FALTA_FATURADA;
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private TipoAgendamento tipoAgendamento(AgendaManualForm form) {
        return form.getTipoAgendamento() == null ? TipoAgendamento.PLANO : form.getTipoAgendamento();
    }

    private PlanoSessaoEntity resolverPlanoParaCriacao(AgendaManualForm form, TipoAgendamento tipoAgendamento) {
        validarVinculoAgendamento(form, tipoAgendamento);
        if (tipoAgendamento == TipoAgendamento.AVULSO) {
            return null;
        }
        // Agendamento retroativo registra um fato ja ocorrido: a vigencia do plano nao deve
        // bloquear o registro, apenas novos agendamentos para o futuro.
        boolean considerarVigencia = !isRetroativo(form.getData());
        PlanoSessaoEntity plano = form.getPlanoSessaoId() == null
                ? planoSessaoService.buscarPrimeiroLivreParaAgenda(form.getClienteId(), considerarVigencia)
                        .orElseThrow(() -> new BusinessException(
                                "O paciente não possui plano de tratamento com saldo livre para agenda. Cadastre ou renove um plano antes de confirmar a sessão."
                        ))
                : planoSessaoService.resolverPlanoParaSessao(form.getClienteId(), form.getPlanoSessaoId(), null, considerarVigencia);
        validarSaldoLivreParaNovoAgendamento(plano, 1, considerarVigencia);
        return plano;
    }

    private boolean isRetroativo(LocalDate data) {
        return data.isBefore(LocalDate.now(zoneId(obterConfiguracaoAtiva())));
    }

    private PlanoSessaoEntity resolverPlanoParaEdicao(AgendaManualForm form, TipoAgendamento tipoAgendamento, UUID planoAtualId) {
        validarVinculoAgendamento(form, tipoAgendamento);
        if (tipoAgendamento == TipoAgendamento.AVULSO) {
            return null;
        }
        boolean considerarVigencia = !isRetroativo(form.getData());
        PlanoSessaoEntity plano = planoSessaoService.resolverPlanoParaSessao(form.getClienteId(), form.getPlanoSessaoId(), planoAtualId, considerarVigencia);
        if (planoAtualId == null || !planoAtualId.equals(plano.getId())) {
            validarSaldoLivreParaNovoAgendamento(plano, 1, considerarVigencia);
        }
        return plano;
    }

    private void validarVinculoAgendamento(AgendaManualForm form, TipoAgendamento tipoAgendamento) {
        if (tipoAgendamento == TipoAgendamento.PLANO) {
            return;
        }
        if (form.getPlanoSessaoId() != null) {
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

    private void validarSaldoLivreParaNovoAgendamento(PlanoSessaoEntity plano, int novasSessoes, boolean considerarVigencia) {
        int sessoesLivres = planoSessaoService.calcularSessoesDisponiveisParaAgenda(plano, considerarVigencia);
        if (novasSessoes > sessoesLivres) {
            throw new BusinessException("O plano selecionado não possui saldo livre para novos agendamentos. "
                    + "Sessões já agendadas bloqueiam créditos futuros, mas só serão consumidas quando realizadas ou marcadas como falta faturada.");
        }
    }

    /**
     * Registra o consumo de uma sessão no plano de tratamento, caso ainda não tenha sido registrado.
     * <p>
     * Sessões do tipo AVULSO nunca consomem plano. Para sessões do tipo PLANO que não possuam
     * vínculo direto com um plano (planoSessao == null), tenta localizar o primeiro plano
     * disponível do paciente e vincula retroativamente.
     * </p>
     */
    private void registrarConsumoPlanoSeNecessario(SessaoEntity sessao) {
        if (Boolean.TRUE.equals(sessao.getPlanoConsumoRegistrado())) {
            return; // consumo já registrado (pré-consumo no agendamento ou ação anterior)
        }
        if (sessao.getTipoAgendamento() == TipoAgendamento.AVULSO) {
            return; // avulso não consome plano
        }
        PlanoSessaoEntity plano = sessao.getPlanoSessao();
        if (plano == null) {
            // fallback: sessão sem vínculo direto — tenta encontrar plano ativo do paciente
            plano = planoSessaoService.buscarPrimeiroDisponivel(sessao.getCliente().getId())
                    .orElse(null);
            if (plano != null) {
                sessao.setPlanoSessao(plano);
            }
        }
        if (plano != null) {
            planoSessaoService.reservarSessaoParaConclusao(plano.getId());
            sessao.setPlanoConsumoRegistrado(Boolean.TRUE);
        }
    }
}
