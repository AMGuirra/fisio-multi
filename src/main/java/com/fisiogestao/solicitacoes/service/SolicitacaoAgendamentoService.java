package com.fisiogestao.solicitacoes.service;

import com.fisiogestao.agenda.HorarioBloqueadoEntity;
import com.fisiogestao.agenda.HorarioBloqueadoStatus;
import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.BloqueioAgendaOrigem;
import com.fisiogestao.agenda.form.BloqueioAgendaCadastroForm;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.agenda.service.BloqueioAgendaService;
import com.fisiogestao.agenda.repository.HorarioBloqueadoRepository;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.email.service.AgendamentoEmailNotificationService;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.PacoteSessaoEntity;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.pacientes.service.PacoteSessaoService;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import com.fisiogestao.solicitacoes.SolicitacaoStatus;
import com.fisiogestao.solicitacoes.form.AgendamentoOnlineForm;
import com.fisiogestao.solicitacoes.form.AprovacaoSolicitacaoForm;
import com.fisiogestao.solicitacoes.form.ReagendarSolicitacaoForm;
import com.fisiogestao.solicitacoes.repository.SolicitacaoAgendamentoRepository;
import com.fisiogestao.whatsapp.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SolicitacaoAgendamentoService {

    private final SolicitacaoAgendamentoRepository solicitacaoAgendamentoRepository;
    private final HorarioBloqueadoRepository horarioBloqueadoRepository;
    private final SessaoRepository sessaoRepository;
    private final AgendaService agendaService;
    private final BloqueioAgendaService bloqueioAgendaService;
    private final PacienteService pacienteService;
    private final PacoteSessaoService pacoteSessaoService;
    private final WhatsAppService whatsAppService;
    private final AgendamentoEmailNotificationService agendamentoEmailNotificationService;
    private final AuditService auditService;

    public List<SolicitacaoAgendamentoEntity> listarPendentes() {
        return solicitacaoAgendamentoRepository.findByStatusOrderByCreatedAtAsc(SolicitacaoStatus.PENDENTE);
    }

    public List<SolicitacaoAgendamentoEntity> listarTodas() {
        return solicitacaoAgendamentoRepository.findAllByOrderByCreatedAtDesc();
    }

    public SolicitacaoAgendamentoEntity buscarPorId(UUID id) {
        return solicitacaoAgendamentoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Solicitação não encontrada."));
    }

    public List<SolicitacaoAgendamentoEntity> listarPendentesNoPeriodo(LocalDate inicioInclusive, LocalDate fimExclusive) {
        IntervaloPeriodo periodo = periodo(inicioInclusive, fimExclusive);
        return solicitacaoAgendamentoRepository.findByStatusAndDataHoraInicioBetweenOrderByDataHoraInicioAsc(
                SolicitacaoStatus.PENDENTE,
                periodo.inicio(),
                periodo.fim()
        );
    }

    public List<SolicitacaoAgendamentoEntity> listarNoPeriodo(LocalDate inicioInclusive, LocalDate fimExclusive) {
        IntervaloPeriodo periodo = periodo(inicioInclusive, fimExclusive);
        return solicitacaoAgendamentoRepository.findByDataHoraInicioBetweenOrderByDataHoraInicioAsc(
                periodo.inicio(),
                periodo.fim()
        );
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SolicitacaoAgendamentoEntity criarSolicitacaoPublica(AgendamentoOnlineForm form) {
        LocalTime horario = parseHorario(form.getHorario());
        AgendaService.IntervaloAgenda intervalo = agendaService.montarIntervalo(form.getData(), horario);
        agendaService.validarHorarioDisponivel(intervalo.inicio(), intervalo.fim(), null, null);

        SolicitacaoAgendamentoEntity solicitacao = SolicitacaoAgendamentoEntity.builder()
                .nomePaciente(form.getNomePaciente().trim())
                .telefone(form.getTelefone().replaceAll("\\D", ""))
                .email(texto(form.getEmail()))
                .dataHoraInicio(intervalo.inicio())
                .dataHoraFim(intervalo.fim())
                .observacoes(texto(form.getObservacoes()))
                .status(SolicitacaoStatus.PENDENTE)
                .build();
        SolicitacaoAgendamentoEntity salva = solicitacaoAgendamentoRepository.save(solicitacao);
        auditService.recordCreate(
                AuditModule.SOLICITACOES,
                "solicitacao_agendamento",
                salva,
                null,
                "Solicitação pública de agendamento criada.",
                AuditEventOrigin.ONLINE
        );

        HorarioBloqueadoEntity bloqueio = HorarioBloqueadoEntity.builder()
                .dataHoraInicio(intervalo.inicio())
                .dataHoraFim(intervalo.fim())
                .status(HorarioBloqueadoStatus.PENDENTE)
                .origem("AGENDAMENTO_ONLINE")
                .motivo("Reserva provisória criada pelo agendamento online")
                .solicitacao(salva)
                .build();
        horarioBloqueadoRepository.save(bloqueio);

        whatsAppService.enfileirar(
                agendaService.obterConfiguracaoAtiva().getTelefoneWhatsapp(),
                "Nova solicitação de agendamento para " + salva.getNomePaciente() + " às " + form.getHorario() + ".",
                "SOLICITACAO:" + salva.getId()
        );
        agendamentoEmailNotificationService.notificarAguardandoConfirmacao(salva);
        return salva;
    }

    @Transactional
    public SessaoEntity aprovarSolicitacao(UUID solicitacaoId, AprovacaoSolicitacaoForm form) {
        SolicitacaoAgendamentoEntity solicitacao = solicitacaoAgendamentoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new BusinessException("Solicitação não encontrada."));
        var snapshotSolicitacaoAnterior = auditService.snapshotOf(solicitacao);
        if (solicitacao.getStatus() != SolicitacaoStatus.PENDENTE) {
            throw new BusinessException("A solicitação não está pendente.");
        }

        HorarioBloqueadoEntity bloqueio = horarioBloqueadoRepository.findBySolicitacaoId(solicitacaoId)
                .orElseThrow(() -> new BusinessException("Bloqueio temporário da solicitação não encontrado."));
        if (bloqueio.getStatus() != HorarioBloqueadoStatus.PENDENTE) {
            throw new BusinessException("O bloqueio temporário da solicitação não está mais disponível.");
        }

        agendaService.validarHorarioDisponivel(
                solicitacao.getDataHoraInicio(),
                solicitacao.getDataHoraFim(),
                bloqueio.getId(),
                null
        );

        ClienteEntity cliente = pacienteService.localizarOuCriar(
                solicitacao.getNomePaciente(),
                solicitacao.getTelefone(),
                solicitacao.getEmail()
        );

        PacoteSessaoEntity pacote = pacoteSessaoService.resolverPacoteParaSessao(
                cliente.getId(),
                form != null ? form.getPacoteSessaoId() : null,
                null
        );
        pacoteSessaoService.reservarSessao(pacote.getId());

        SessaoEntity sessao = SessaoEntity.builder()
                .cliente(cliente)
                .pacoteSessao(pacote)
                .dataHoraInicio(solicitacao.getDataHoraInicio())
                .dataHoraFim(solicitacao.getDataHoraFim())
                .status(SessaoStatus.AGENDADA)
                .tipoAtendimento("Fisioterapia")
                .observacoes(solicitacao.getObservacoes())
                .pacoteConsumoRegistrado(Boolean.TRUE)
                .build();
        SessaoEntity salva = sessaoRepository.save(sessao);
        auditService.recordCreate(
                AuditModule.AGENDA,
                "sessao",
                salva,
                null,
                "Sessão criada a partir da aprovação de solicitação online.",
                AuditEventOrigin.MANUAL
        );

        solicitacao.setCliente(cliente);
        solicitacao.setStatus(SolicitacaoStatus.APROVADA);
        SolicitacaoAgendamentoEntity solicitacaoSalva = solicitacaoAgendamentoRepository.save(solicitacao);
        auditService.recordManual(
                AuditModule.SOLICITACOES,
                "solicitacao_agendamento",
                solicitacaoSalva.getId().toString(),
                auditService.describeOf(solicitacaoSalva),
                AuditActionType.CONFIRM,
                snapshotSolicitacaoAnterior,
                auditService.snapshotOf(solicitacaoSalva),
                null,
                "Solicitação aprovada e convertida em sessão.",
                AuditEventOrigin.MANUAL
        );

        bloqueio.setStatus(HorarioBloqueadoStatus.CONFIRMADO);
        horarioBloqueadoRepository.save(bloqueio);

        whatsAppService.enfileirar(
                solicitacao.getTelefone(),
                "Seu agendamento foi aprovado para " + solicitacao.getDataHoraInicio().toLocalDate() + " às "
                        + solicitacao.getDataHoraInicio().toLocalTime().withSecond(0).withNano(0) + ".",
                "APROVACAO:" + solicitacao.getId()
        );
        agendamentoEmailNotificationService.notificarConfirmadoPorSolicitacao(solicitacao);
        return salva;
    }

    @Transactional
    public void rejeitarSolicitacao(UUID solicitacaoId) {
        SolicitacaoAgendamentoEntity solicitacao = solicitacaoAgendamentoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new BusinessException("Solicitação não encontrada."));
        var snapshotAnterior = auditService.snapshotOf(solicitacao);
        if (solicitacao.getStatus() != SolicitacaoStatus.PENDENTE) {
            throw new BusinessException("Somente solicitações pendentes podem ser rejeitadas.");
        }

        horarioBloqueadoRepository.findBySolicitacaoId(solicitacaoId).ifPresent(bloqueio -> {
            bloqueio.setStatus(HorarioBloqueadoStatus.CANCELADO);
            horarioBloqueadoRepository.save(bloqueio);
        });

        solicitacao.setStatus(SolicitacaoStatus.REJEITADA);
        SolicitacaoAgendamentoEntity salva = solicitacaoAgendamentoRepository.save(solicitacao);
        auditService.recordManual(
                AuditModule.SOLICITACOES,
                "solicitacao_agendamento",
                salva.getId().toString(),
                auditService.describeOf(salva),
                AuditActionType.REJECT,
                snapshotAnterior,
                auditService.snapshotOf(salva),
                null,
                "Solicitação rejeitada pela clínica.",
                AuditEventOrigin.MANUAL
        );

        whatsAppService.enfileirar(
                solicitacao.getTelefone(),
                "Seu pedido de agendamento não pôde ser confirmado. Entre em contato para escolher outro horário.",
                "REJEICAO:" + solicitacao.getId()
        );
        agendamentoEmailNotificationService.notificarRecusado(solicitacao);
    }

    @Transactional
    public SolicitacaoAgendamentoEntity reagendarSolicitacao(UUID solicitacaoId, ReagendarSolicitacaoForm form) {
        SolicitacaoAgendamentoEntity solicitacao = solicitacaoAgendamentoRepository.findByIdAndStatus(solicitacaoId, SolicitacaoStatus.PENDENTE)
                .orElseThrow(() -> new BusinessException("Somente solicitações pendentes podem ser reagendadas."));
        var snapshotAnterior = auditService.snapshotOf(solicitacao);

        HorarioBloqueadoEntity bloqueio = horarioBloqueadoRepository.findBySolicitacaoId(solicitacaoId)
                .orElseThrow(() -> new BusinessException("Bloqueio temporário da solicitação não encontrado."));

        AgendaService.IntervaloAgenda novoIntervalo = agendaService.montarIntervalo(form.getData(), form.getHorario());
        agendaService.validarHorarioDisponivel(novoIntervalo.inicio(), novoIntervalo.fim(), bloqueio.getId(), null);

        solicitacao.setDataHoraInicio(novoIntervalo.inicio());
        solicitacao.setDataHoraFim(novoIntervalo.fim());
        if (texto(form.getMotivo()) != null) {
            String historico = solicitacao.getObservacoes() == null ? "" : solicitacao.getObservacoes().trim() + "\n";
            solicitacao.setObservacoes((historico + "Reagendado: " + form.getMotivo().trim()).trim());
        }
        SolicitacaoAgendamentoEntity salva = solicitacaoAgendamentoRepository.save(solicitacao);
        auditService.recordManual(
                AuditModule.SOLICITACOES,
                "solicitacao_agendamento",
                salva.getId().toString(),
                auditService.describeOf(salva),
                AuditActionType.RESCHEDULE,
                snapshotAnterior,
                auditService.snapshotOf(salva),
                texto(form.getMotivo()),
                "Solicitação online reagendada.",
                AuditEventOrigin.MANUAL
        );

        bloqueio.setDataHoraInicio(novoIntervalo.inicio());
        bloqueio.setDataHoraFim(novoIntervalo.fim());
        bloqueio.setStatus(HorarioBloqueadoStatus.PENDENTE);
        bloqueio.setMotivo("Reserva provisória reagendada");
        horarioBloqueadoRepository.save(bloqueio);

        whatsAppService.enfileirar(
                solicitacao.getTelefone(),
                "Seu pedido de agendamento foi reagendado para "
                        + solicitacao.getDataHoraInicio().toLocalDate() + " às "
                        + solicitacao.getDataHoraInicio().toLocalTime().withSecond(0).withNano(0) + ".",
                "REAGENDAMENTO:" + solicitacao.getId()
        );
        return solicitacao;
    }

    @Transactional
    public void bloquearPeriodoDaSolicitacao(UUID solicitacaoId, String motivo, String usuario) {
        SolicitacaoAgendamentoEntity solicitacao = solicitacaoAgendamentoRepository.findByIdAndStatus(solicitacaoId, SolicitacaoStatus.PENDENTE)
                .orElseThrow(() -> new BusinessException("Somente solicitações pendentes podem ser bloqueadas."));
        var snapshotAnterior = auditService.snapshotOf(solicitacao);

        horarioBloqueadoRepository.findBySolicitacaoId(solicitacaoId).ifPresent(bloqueio -> {
            bloqueio.setStatus(HorarioBloqueadoStatus.CANCELADO);
            horarioBloqueadoRepository.save(bloqueio);
        });

        String motivoBloqueio = texto(motivo);
        if (motivoBloqueio == null) {
            motivoBloqueio = "Bloqueio originado da solicitação de " + solicitacao.getNomePaciente();
        }

        BloqueioAgendaCadastroForm form = new BloqueioAgendaCadastroForm();
        form.setDataInicial(solicitacao.getDataHoraInicio().toLocalDate());
        form.setDataFinal(solicitacao.getDataHoraInicio().toLocalDate());
        form.setHoraInicio(solicitacao.getDataHoraInicio().toLocalTime());
        form.setHoraFim(solicitacao.getDataHoraFim().toLocalTime());
        form.setBloqueioDiaInteiro(false);
        form.setMotivo(motivoBloqueio);
        form.setObservacao("Bloqueio criado a partir da solicitação online de " + solicitacao.getNomePaciente() + ".");
        bloqueioAgendaService.criar(form, usuario, BloqueioAgendaOrigem.ONLINE);

        solicitacao.setStatus(SolicitacaoStatus.CANCELADA);
        SolicitacaoAgendamentoEntity salva = solicitacaoAgendamentoRepository.save(solicitacao);
        auditService.recordManual(
                AuditModule.SOLICITACOES,
                "solicitacao_agendamento",
                salva.getId().toString(),
                auditService.describeOf(salva),
                AuditActionType.CANCEL,
                snapshotAnterior,
                auditService.snapshotOf(salva),
                motivoBloqueio,
                "Solicitação cancelada por indisponibilidade do horário.",
                AuditEventOrigin.MANUAL
        );

        whatsAppService.enfileirar(
                solicitacao.getTelefone(),
                "Seu pedido de agendamento foi cancelado pois o horário ficou indisponível. Entre em contato para escolher outra opção.",
                "CANCELAMENTO:" + solicitacao.getId()
        );
    }

    private IntervaloPeriodo periodo(LocalDate inicioInclusive, LocalDate fimExclusive) {
        ZoneId zoneId = ZoneId.of(agendaService.obterConfiguracaoAtiva().getTimezoneId());
        OffsetDateTime inicio = inicioInclusive.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime fim = fimExclusive.atStartOfDay(zoneId).toOffsetDateTime();
        return new IntervaloPeriodo(inicio, fim);
    }

    private LocalTime parseHorario(String horario) {
        try {
            return LocalTime.parse(horario);
        } catch (DateTimeParseException ex) {
            throw new BusinessException("Horário inválido.");
        }
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private record IntervaloPeriodo(OffsetDateTime inicio, OffsetDateTime fim) {}
}
