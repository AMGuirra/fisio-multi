package com.fisiogestao.agenda.service;

import com.fisiogestao.agenda.BloqueioAgendaEntity;
import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.HorarioBloqueadoEntity;
import com.fisiogestao.agenda.HorarioBloqueadoStatus;
import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.dto.SlotDisponivelDto;
import com.fisiogestao.agenda.form.AgendaManualForm;
import com.fisiogestao.agenda.form.BloqueioAgendaForm;
import com.fisiogestao.agenda.repository.BloqueioAgendaRepository;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.agenda.repository.HorarioBloqueadoRepository;
import com.fisiogestao.agenda.repository.SessaoRepository;
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
    private final BloqueioAgendaRepository bloqueioAgendaRepository;
    private final PacienteService pacienteService;
    private final PacoteSessaoService pacoteSessaoService;

    public record IntervaloAgenda(OffsetDateTime inicio, OffsetDateTime fim) {
    }

    public ConfiguracaoClinicaEntity obterConfiguracaoAtiva() {
        return configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc()
                .orElseThrow(() -> new BusinessException("Configure a clínica antes de usar a agenda."));
    }

    public List<SlotDisponivelDto> gerarSlotsDisponiveis(LocalDate data) {
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
        List<BloqueioAgendaEntity> bloqueiosAgenda = bloqueioAgendaRepository
                .findByAtivoTrueAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(janelaDia.fim(), janelaDia.inicio());

        ZoneId zoneId = zoneId(configuracao);
        OffsetDateTime agora = OffsetDateTime.now(zoneId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        java.util.ArrayList<SlotDisponivelDto> slots = new java.util.ArrayList<>();
        LocalTime cursor = configuracao.getHorarioAbertura();
        while (!cursor.plusMinutes(configuracao.getDuracaoSessaoMinutos()).isAfter(configuracao.getHorarioFechamento())) {
            LocalTime fim = cursor.plusMinutes(configuracao.getDuracaoSessaoMinutos());
            OffsetDateTime inicioSlot = data.atTime(cursor).atZone(zoneId).toOffsetDateTime();
            OffsetDateTime fimSlot = data.atTime(fim).atZone(zoneId).toOffsetDateTime();

            if (inicioSlot.isBefore(agora)) {
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
        OffsetDateTime inicio = data.atTime(horarioInicio).atZone(zoneId).toOffsetDateTime();
        OffsetDateTime fim = inicio.plusMinutes(configuracao.getDuracaoSessaoMinutos());
        return new IntervaloAgenda(inicio, fim);
    }

    public void validarHorarioDisponivel(
            OffsetDateTime inicio,
            OffsetDateTime fim,
            UUID horarioBloqueadoIgnoradoId,
            UUID sessaoIgnoradaId
    ) {
        ConfiguracaoClinicaEntity configuracao = obterConfiguracaoAtiva();
        if (!inicio.isBefore(fim)) {
            throw new BusinessException("O intervalo da agenda é inválido.");
        }

        ZoneId zoneId = zoneId(configuracao);
        if (!configuracao.diasFuncionamentoSet().contains(inicio.atZoneSameInstant(zoneId).getDayOfWeek())) {
            throw new BusinessException("A clínica não funciona nesse dia.");
        }

        if (inicio.isBefore(OffsetDateTime.now(zoneId))) {
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

        boolean conflitoBloqueioAgenda = bloqueioAgendaRepository
                .existsByAtivoTrueAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(fim, inicio);
        if (conflitoBloqueioAgenda) {
            throw new BusinessException("Esse horário está bloqueado na agenda da clínica.");
        }
    }

    public List<SessaoEntity> listarSessoesDoDia(LocalDate data) {
        IntervaloAgenda janela = janelaDoDia(data, obterConfiguracaoAtiva());
        return sessaoRepository.findByDataHoraInicioBetweenOrderByDataHoraInicioAsc(janela.inicio(), janela.fim());
    }

    public List<SessaoEntity> listarSessoesDoPaciente(UUID clienteId) {
        return sessaoRepository.findByClienteIdOrderByDataHoraInicioDesc(clienteId);
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
        IntervaloAgenda janela = janelaDoDia(data, obterConfiguracaoAtiva());
        return bloqueioAgendaRepository.findByAtivoTrueAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
                janela.fim(),
                janela.inicio()
        );
    }

    public List<BloqueioAgendaEntity> listarBloqueiosAgendaNoPeriodo(LocalDate inicioInclusive, LocalDate fimExclusive) {
        ConfiguracaoClinicaEntity configuracao = obterConfiguracaoAtiva();
        ZoneId zoneId = zoneId(configuracao);
        OffsetDateTime inicio = inicioInclusive.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime fim = fimExclusive.atStartOfDay(zoneId).toOffsetDateTime();
        return bloqueioAgendaRepository.findByAtivoTrueAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(fim, inicio);
    }

    @Transactional
    public SessaoEntity agendarSessaoManual(AgendaManualForm form) {
        IntervaloAgenda intervalo = montarIntervalo(form.getData(), form.getHorario());
        validarHorarioDisponivel(intervalo.inicio(), intervalo.fim(), null, null);

        PacoteSessaoEntity pacote = null;
        if (form.getPacoteSessaoId() != null) {
            pacote = pacoteSessaoService.validarDisponivel(form.getPacoteSessaoId());
        }

        SessaoEntity sessao = SessaoEntity.builder()
                .cliente(pacienteService.buscarPorId(form.getClienteId()))
                .pacoteSessao(pacote)
                .dataHoraInicio(intervalo.inicio())
                .dataHoraFim(intervalo.fim())
                .status(SessaoStatus.AGENDADA)
                .tipoAtendimento(texto(form.getTipoAtendimento()))
                .observacoes(texto(form.getObservacoes()))
                .build();
        return sessaoRepository.save(sessao);
    }

    @Transactional
    public SessaoEntity marcarComoRealizada(UUID sessaoId, String observacoes) {
        SessaoEntity sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new BusinessException("Sessão não encontrada."));

        if (sessao.getStatus() == SessaoStatus.CANCELADA) {
            throw new BusinessException("Não é possível concluir uma sessão cancelada.");
        }
        if (sessao.getStatus() == SessaoStatus.REALIZADA) {
            return sessao;
        }

        if (sessao.getPacoteSessao() != null) {
            pacoteSessaoService.consumirSessao(sessao.getPacoteSessao().getId());
        }

        sessao.setStatus(SessaoStatus.REALIZADA);
        if (texto(observacoes) != null) {
            sessao.setObservacoes(texto(observacoes));
        }
        return sessaoRepository.save(sessao);
    }

    @Transactional
    public SessaoEntity reagendarSessao(UUID sessaoId, LocalDate data, LocalTime horario) {
        SessaoEntity sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new BusinessException("Sessão não encontrada."));

        if (sessao.getStatus() != SessaoStatus.AGENDADA) {
            throw new BusinessException("Apenas sessões agendadas podem ser reagendadas.");
        }

        IntervaloAgenda intervalo = montarIntervalo(data, horario);
        validarHorarioDisponivel(intervalo.inicio(), intervalo.fim(), null, sessaoId);

        sessao.setDataHoraInicio(intervalo.inicio());
        sessao.setDataHoraFim(intervalo.fim());
        return sessaoRepository.save(sessao);
    }

    @Transactional
    public BloqueioAgendaEntity criarBloqueioAgenda(BloqueioAgendaForm form, String usuario) {
        ConfiguracaoClinicaEntity configuracao = obterConfiguracaoAtiva();
        ZoneId zoneId = zoneId(configuracao);
        OffsetDateTime inicio = form.getData().atTime(form.getHoraInicio()).atZone(zoneId).toOffsetDateTime();
        OffsetDateTime fim = form.getData().atTime(form.getHoraFim()).atZone(zoneId).toOffsetDateTime();

        if (!inicio.isBefore(fim)) {
            throw new BusinessException("O bloqueio precisa terminar depois do horário inicial.");
        }
        validarHorarioDisponivel(inicio, fim, null, null);

        BloqueioAgendaEntity bloqueio = BloqueioAgendaEntity.builder()
                .dataHoraInicio(inicio)
                .dataHoraFim(fim)
                .motivo(form.getMotivo().trim())
                .criadoPor(usuario)
                .ativo(true)
                .build();
        return bloqueioAgendaRepository.save(bloqueio);
    }

    @Transactional
    public List<BloqueioAgendaEntity> criarBloqueioPeriodo(
            LocalDate dataInicio,
            LocalDate dataFim,
            LocalTime horaInicio,
            LocalTime horaFim,
            boolean diaInteiro,
            String motivo,
            String usuario
    ) {
        if (dataInicio == null || dataFim == null) {
            throw new BusinessException("Informe o período inicial e final do bloqueio.");
        }
        if (dataFim.isBefore(dataInicio)) {
            throw new BusinessException("A data final não pode ser anterior à data inicial.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new BusinessException("Informe o motivo do bloqueio.");
        }

        ConfiguracaoClinicaEntity configuracao = obterConfiguracaoAtiva();
        ZoneId zoneId = zoneId(configuracao);

        LocalTime inicioHoraReal = diaInteiro ? configuracao.getHorarioAbertura() : horaInicio;
        LocalTime fimHoraReal = diaInteiro ? configuracao.getHorarioFechamento() : horaFim;

        if (inicioHoraReal == null || fimHoraReal == null || !inicioHoraReal.isBefore(fimHoraReal)) {
            throw new BusinessException("Horário de bloqueio inválido.");
        }

        ArrayList<BloqueioAgendaEntity> criados = new ArrayList<>();

        LocalDate cursor = dataInicio;
        while (!cursor.isAfter(dataFim)) {
            OffsetDateTime inicio = cursor.atTime(inicioHoraReal).atZone(zoneId).toOffsetDateTime();
            OffsetDateTime fim = cursor.atTime(fimHoraReal).atZone(zoneId).toOffsetDateTime();

            boolean possuiSessaoNoPeriodo = sessaoRepository.existsByStatusInAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
                    STATUS_SESSAO_OCUPADA, fim, inicio
            );
            if (possuiSessaoNoPeriodo) {
                throw new BusinessException("Há sessão agendada no dia " + cursor + ". Reagende/cancele as sessões antes de bloquear.");
            }

            boolean jaExisteBloqueio = bloqueioAgendaRepository.existsByAtivoTrueAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
                    fim, inicio
            );
            if (!jaExisteBloqueio) {
                BloqueioAgendaEntity bloqueio = BloqueioAgendaEntity.builder()
                        .dataHoraInicio(inicio)
                        .dataHoraFim(fim)
                        .motivo(motivo.trim())
                        .criadoPor(usuario)
                        .ativo(true)
                        .build();
                criados.add(bloqueioAgendaRepository.save(bloqueio));
            }

            cursor = cursor.plusDays(1);
        }

        if (criados.isEmpty()) {
            throw new BusinessException("Nenhum bloqueio foi criado. O período já estava bloqueado.");
        }
        return criados;
    }

    @Transactional
    public void desativarBloqueioAgenda(UUID bloqueioId) {
        BloqueioAgendaEntity bloqueio = bloqueioAgendaRepository.findById(bloqueioId)
                .orElseThrow(() -> new BusinessException("Bloqueio não encontrado."));
        bloqueio.setAtivo(false);
        bloqueioAgendaRepository.save(bloqueio);
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

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
