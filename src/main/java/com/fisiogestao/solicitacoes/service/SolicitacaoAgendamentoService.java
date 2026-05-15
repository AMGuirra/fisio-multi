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
import com.fisiogestao.pacientes.PlanoSessaoEntity;
import com.fisiogestao.pacientes.form.PacienteForm;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.pacientes.service.PlanoSessaoService;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import com.fisiogestao.solicitacoes.SolicitacaoStatus;
import com.fisiogestao.solicitacoes.form.AgendamentoOnlineForm;
import com.fisiogestao.solicitacoes.form.AprovacaoSolicitacaoForm;
import com.fisiogestao.solicitacoes.form.EditarSolicitacaoForm;
import com.fisiogestao.solicitacoes.form.ReagendarSolicitacaoForm;
import com.fisiogestao.solicitacoes.repository.SolicitacaoAgendamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RequiredArgsConstructor
public class SolicitacaoAgendamentoService {

    private final SolicitacaoAgendamentoRepository solicitacaoAgendamentoRepository;
    private final HorarioBloqueadoRepository horarioBloqueadoRepository;
    private final SessaoRepository sessaoRepository;
    private final AgendaService agendaService;
    private final BloqueioAgendaService bloqueioAgendaService;
    private final PacienteService pacienteService;
    private final PlanoSessaoService planoSessaoService;
    private final AgendamentoEmailNotificationService agendamentoEmailNotificationService;
    private final AuditService auditService;

    public List<SolicitacaoAgendamentoEntity> listarPendentes() {
        return solicitacaoAgendamentoRepository.findByStatusOrderByCreatedAtAsc(SolicitacaoStatus.PENDENTE);
    }

    /**
     * Retorna todas as solicitações sem filtro de data.
     * Prefira {@link #listarNoPeriodo} ou {@link #filtrarHistorico} com datas
     * quando o volume histórico for grande — este método faz full table scan.
     */
    public List<SolicitacaoAgendamentoEntity> listarTodas() {
        return solicitacaoAgendamentoRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<SolicitacaoAgendamentoEntity> listarHistorico() {
        return filtrarHistorico(null, null, null, null);
    }

    public List<SolicitacaoAgendamentoEntity> filtrarHistorico(
            SolicitacaoStatus status,
            LocalDate inicioInclusive,
            LocalDate fimInclusive,
            String buscaPaciente) {
        String buscaTexto = texto(buscaPaciente);
        String buscaTelefone = normalizarBuscaTelefone(buscaTexto);
        ZoneId zoneId = ZoneId.of(agendaService.obterConfiguracaoAtiva().getTimezoneId());

        // Quando há filtro de datas, usamos uma query delimitada por updatedAt para
        // evitar
        // full table scan. Sem datas, carrega somente os não-PENDENTE ordenados.
        List<SolicitacaoAgendamentoEntity> candidatos;
        if (inicioInclusive != null && fimInclusive != null) {
            OffsetDateTime dtInicio = inicioInclusive.atStartOfDay(zoneId).toOffsetDateTime();
            OffsetDateTime dtFim = fimInclusive.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime().minusNanos(1);
            candidatos = solicitacaoAgendamentoRepository
                    .findByStatusNotAndUpdatedAtBetweenOrderByUpdatedAtDesc(SolicitacaoStatus.PENDENTE, dtInicio,
                            dtFim);
        } else {
            candidatos = solicitacaoAgendamentoRepository
                    .findByStatusNotOrderByUpdatedAtDesc(SolicitacaoStatus.PENDENTE);
        }

        return candidatos.stream()
                .filter(sol -> status == null || sol.getStatus() == status)
                .filter(sol -> correspondeDataHistorico(sol, inicioInclusive, fimInclusive, zoneId))
                .filter(sol -> correspondeBuscaHistorico(sol, buscaTexto, buscaTelefone))
                .toList(); // já ordenado pelo banco; sem sorted() redundante
    }

    public ResumoSolicitacoes resumoMesAtual() {
        ZoneId zoneId = ZoneId.of(agendaService.obterConfiguracaoAtiva().getTimezoneId());
        LocalDate hoje = LocalDate.now(zoneId);
        OffsetDateTime inicioMes = hoje.withDayOfMonth(1).atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime inicioProximoMes = hoje.withDayOfMonth(1).plusMonths(1).atStartOfDay(zoneId).toOffsetDateTime();

        // Contagens diretas no banco — evita carregar todas as solicitações em memória.
        long totalPendentes = solicitacaoAgendamentoRepository
                .countByStatusAndCreatedAtBetween(SolicitacaoStatus.PENDENTE, inicioMes, inicioProximoMes);
        long totalAprovadas = solicitacaoAgendamentoRepository
                .countByStatusAndUpdatedAtBetween(SolicitacaoStatus.APROVADA, inicioMes, inicioProximoMes);
        long totalNegadas = solicitacaoAgendamentoRepository
                .countByStatusAndUpdatedAtBetween(SolicitacaoStatus.REJEITADA, inicioMes, inicioProximoMes);
        long totalGeral = solicitacaoAgendamentoRepository
                .countByCreatedAtBetween(inicioMes, inicioProximoMes);

        long totalProcessadas = totalAprovadas + totalNegadas;
        int taxaAprovacao = totalProcessadas > 0
                ? (int) Math.round((totalAprovadas * 100.0) / totalProcessadas)
                : 0;

        return new ResumoSolicitacoes(totalPendentes, totalAprovadas, totalNegadas, totalGeral, taxaAprovacao);
    }

    public SolicitacaoAgendamentoEntity buscarPorId(UUID id) {
        return solicitacaoAgendamentoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Solicitação não encontrada."));
    }

    public List<SolicitacaoAgendamentoEntity> listarPendentesNoPeriodo(LocalDate inicioInclusive,
            LocalDate fimExclusive) {
        IntervaloPeriodo periodo = periodo(inicioInclusive, fimExclusive);
        return solicitacaoAgendamentoRepository.findByStatusAndDataHoraInicioBetweenOrderByDataHoraInicioAsc(
                SolicitacaoStatus.PENDENTE,
                periodo.inicio(),
                periodo.fim());
    }

    public List<SolicitacaoAgendamentoEntity> listarNoPeriodo(LocalDate inicioInclusive, LocalDate fimExclusive) {
        IntervaloPeriodo periodo = periodo(inicioInclusive, fimExclusive);
        return solicitacaoAgendamentoRepository.findByDataHoraInicioBetweenOrderByDataHoraInicioAsc(
                periodo.inicio(),
                periodo.fim());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SolicitacaoAgendamentoEntity criarSolicitacaoPublica(AgendamentoOnlineForm form) {
        validarFormularioPublico(form);

        LocalTime horario = parseHorario(form.getHorario());
        AgendaService.IntervaloAgenda intervalo = agendaService.montarIntervalo(form.getData(), horario);
        agendaService.validarHorarioDisponivel(intervalo.inicio(), intervalo.fim(), null, null);

        ClienteEntity clienteExistente = pacienteService.buscarPorCpfTelefoneOuEmail(
                form.getCpf(),
                form.getTelefone(),
                form.getEmail())
                .orElse(null);

        DadosEndereco endereco = resolverEnderecoSolicitacao(form, clienteExistente);
        String localAtendimento = normalizarLocalAtendimento(form.getLocalAtendimento());

        SolicitacaoAgendamentoEntity solicitacao = SolicitacaoAgendamentoEntity.builder()
                .nomePaciente(resolverTexto(form.getNomePaciente(),
                        clienteExistente != null ? clienteExistente.getNomeCompleto() : null))
                .telefone(form.getTelefone().replaceAll("\\D", ""))
                .email(texto(form.getEmail()))
                .cpf(normalizarDocumento(form.getCpf(), 11, "Informe um CPF válido com 11 dígitos."))
                .dataHoraInicio(intervalo.inicio())
                .dataHoraFim(intervalo.fim())
                .observacoes(texto(form.getObservacoes()))
                .localAtendimento(localAtendimento)
                .cep(endereco.cep())
                .rua(endereco.rua())
                .numero(endereco.numero())
                .bairro(endereco.bairro())
                .cidade(endereco.cidade())
                .estado(endereco.estado())
                .status(SolicitacaoStatus.PENDENTE)
                .cliente(clienteExistente)
                .build();
        SolicitacaoAgendamentoEntity salva = solicitacaoAgendamentoRepository.save(solicitacao);
        auditService.recordCreate(
                AuditModule.SOLICITACOES,
                "solicitacao_agendamento",
                salva,
                null,
                "Solicitação pública de agendamento criada.",
                AuditEventOrigin.ONLINE);

        HorarioBloqueadoEntity bloqueio = HorarioBloqueadoEntity.builder()
                .dataHoraInicio(intervalo.inicio())
                .dataHoraFim(intervalo.fim())
                .status(HorarioBloqueadoStatus.PENDENTE)
                .origem("AGENDAMENTO_ONLINE")
                .motivo("Reserva provisória criada pelo agendamento online")
                .solicitacao(salva)
                .build();
        horarioBloqueadoRepository.save(bloqueio);

        agendamentoEmailNotificationService.notificarAguardandoConfirmacao(salva);
        return salva;
    }

    @Transactional
    public ClienteEntity cadastrarPaciente(UUID solicitacaoId, AprovacaoSolicitacaoForm form) {
        SolicitacaoAgendamentoEntity solicitacao = solicitacaoAgendamentoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new BusinessException("Solicitação não encontrada."));
        if (solicitacao.getStatus() != SolicitacaoStatus.PENDENTE) {
            throw new BusinessException("A solicitação não está pendente.");
        }
        if (solicitacao.getCliente() != null) {
            throw new BusinessException("O paciente desta solicitação já está cadastrado. Agora basta aprovar.");
        }
        pacienteService.validarRelacionamentoSolicitacaoClienteUuid();

        var snapshotSolicitacaoAnterior = auditService.snapshotOf(solicitacao);
        PacienteForm pacienteForm = montarFormularioPaciente(solicitacao, form);
        ClienteEntity cliente = pacienteService.localizarOuCriar(
                pacienteForm,
                AuditEventOrigin.MANUAL,
                "Cadastro atualizado a partir da solicitação online.",
                "Paciente criado a partir da solicitação online.");

        solicitacao.setCliente(cliente);
        SolicitacaoAgendamentoEntity solicitacaoSalva = solicitacaoAgendamentoRepository.save(solicitacao);
        try {
            auditService.recordManual(
                    AuditModule.SOLICITACOES,
                    "solicitacao_agendamento",
                    solicitacaoSalva.getId().toString(),
                    auditService.describeOf(solicitacaoSalva),
                    AuditActionType.UPDATE,
                    snapshotSolicitacaoAnterior,
                    auditService.snapshotOf(solicitacaoSalva),
                    null,
                    "Paciente cadastrado e vinculado à solicitação online.",
                    AuditEventOrigin.MANUAL);
        } catch (Exception ex) {
            log.warn("Falha ao registrar auditoria de cadastro para a solicitacao {}.", solicitacaoSalva.getId(), ex);
        }
        return cliente;
    }

    @Transactional
    public SessaoEntity aprovarSolicitacao(UUID solicitacaoId, AprovacaoSolicitacaoForm form) {
        SolicitacaoAgendamentoEntity solicitacao = solicitacaoAgendamentoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new BusinessException("Solicitação não encontrada."));
        var snapshotSolicitacaoAnterior = auditService.snapshotOf(solicitacao);
        if (solicitacao.getStatus() != SolicitacaoStatus.PENDENTE) {
            throw new BusinessException("A solicitação não está pendente.");
        }
        if (solicitacao.getCliente() == null) {
            throw new BusinessException("Cadastre o paciente antes de aprovar a solicitação.");
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
                null,
                true);

        ClienteEntity cliente = solicitacao.getCliente();

        PlanoSessaoEntity plano = planoSessaoService.resolverPlanoParaSessao(
                cliente.getId(),
                form != null ? form.getPlanoSessaoId() : null,
                null);
        planoSessaoService.reservarSessao(plano.getId());

        SessaoEntity sessao = SessaoEntity.builder()
                .cliente(cliente)
                .planoSessao(plano)
                .dataHoraInicio(solicitacao.getDataHoraInicio())
                .dataHoraFim(solicitacao.getDataHoraFim())
                .status(SessaoStatus.AGENDADA)
                .tipoAtendimento(solicitacao.getLocalAtendimentoDescricao())
                .observacoes(montarObservacoesSessao(solicitacao))
                .planoConsumoRegistrado(Boolean.TRUE)
                .build();
        SessaoEntity salva = sessaoRepository.save(sessao);
        auditService.recordCreate(
                AuditModule.AGENDA,
                "sessao",
                salva,
                null,
                "Sessão criada a partir da aprovação de solicitação online.",
                AuditEventOrigin.MANUAL);

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
                AuditEventOrigin.MANUAL);

        bloqueio.setStatus(HorarioBloqueadoStatus.CONFIRMADO);
        horarioBloqueadoRepository.save(bloqueio);

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
                AuditEventOrigin.MANUAL);

        agendamentoEmailNotificationService.notificarRecusado(solicitacao);
    }

    @Transactional
    public SolicitacaoAgendamentoEntity editarSolicitacao(UUID solicitacaoId, EditarSolicitacaoForm form) {
        SolicitacaoAgendamentoEntity solicitacao = solicitacaoAgendamentoRepository
                .findByIdAndStatus(solicitacaoId, SolicitacaoStatus.PENDENTE)
                .orElseThrow(() -> new BusinessException("Apenas solicitações pendentes podem ser editadas."));
        var snapshotAnterior = auditService.snapshotOf(solicitacao);

        HorarioBloqueadoEntity bloqueio = horarioBloqueadoRepository.findBySolicitacaoId(solicitacaoId)
                .orElseThrow(() -> new BusinessException("Bloqueio temporário da solicitação não encontrado."));

        AgendaService.IntervaloAgenda novoIntervalo = agendaService.montarIntervalo(form.getData(), form.getHorario());
        boolean horarioAlterado = !novoIntervalo.inicio().equals(solicitacao.getDataHoraInicio());
        if (horarioAlterado) {
            agendaService.validarHorarioDisponivel(novoIntervalo.inicio(), novoIntervalo.fim(), bloqueio.getId(), null);
        }

        String novoNome = texto(form.getNomePaciente());
        if (novoNome == null)
            throw new BusinessException("Informe o nome do paciente.");
        String novoTelefone = form.getTelefone() == null ? null : form.getTelefone().replaceAll("\\D", "");
        if (novoTelefone == null || novoTelefone.isBlank())
            throw new BusinessException("Informe o telefone.");

        solicitacao.setNomePaciente(novoNome);
        solicitacao.setTelefone(novoTelefone);
        solicitacao.setEmail(texto(form.getEmail()));
        solicitacao.setLocalAtendimento(normalizarLocalAtendimento(form.getLocalAtendimento()));
        solicitacao.setObservacoes(texto(form.getObservacoes()));
        solicitacao.setDataHoraInicio(novoIntervalo.inicio());
        solicitacao.setDataHoraFim(novoIntervalo.fim());

        if (horarioAlterado && texto(form.getMotivo()) != null) {
            String historico = solicitacao.getObservacoes() == null ? "" : solicitacao.getObservacoes().trim() + "\n";
            solicitacao.setObservacoes((historico + "Reagendado: " + form.getMotivo().trim()).trim());
        }

        SolicitacaoAgendamentoEntity salva = solicitacaoAgendamentoRepository.save(solicitacao);
        auditService.recordManual(
                AuditModule.SOLICITACOES,
                "solicitacao_agendamento",
                salva.getId().toString(),
                auditService.describeOf(salva),
                AuditActionType.UPDATE,
                snapshotAnterior,
                auditService.snapshotOf(salva),
                texto(form.getMotivo()),
                "Solicitação editada manualmente.",
                AuditEventOrigin.MANUAL);

        if (horarioAlterado) {
            bloqueio.setDataHoraInicio(novoIntervalo.inicio());
            bloqueio.setDataHoraFim(novoIntervalo.fim());
            bloqueio.setStatus(HorarioBloqueadoStatus.PENDENTE);
            bloqueio.setMotivo("Reserva provisória editada");
            horarioBloqueadoRepository.save(bloqueio);

        }
        return salva;
    }

    @Transactional
    public SolicitacaoAgendamentoEntity reagendarSolicitacao(UUID solicitacaoId, ReagendarSolicitacaoForm form) {
        SolicitacaoAgendamentoEntity solicitacao = solicitacaoAgendamentoRepository
                .findByIdAndStatus(solicitacaoId, SolicitacaoStatus.PENDENTE)
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
                AuditEventOrigin.MANUAL);

        bloqueio.setDataHoraInicio(novoIntervalo.inicio());
        bloqueio.setDataHoraFim(novoIntervalo.fim());
        bloqueio.setStatus(HorarioBloqueadoStatus.PENDENTE);
        bloqueio.setMotivo("Reserva provisória reagendada");
        horarioBloqueadoRepository.save(bloqueio);

        return solicitacao;
    }

    @Transactional
    public void cancelarSolicitacaoAprovada(UUID solicitacaoId) {
        SolicitacaoAgendamentoEntity solicitacao = solicitacaoAgendamentoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new BusinessException("Solicitação não encontrada."));
        if (solicitacao.getStatus() != SolicitacaoStatus.APROVADA) {
            throw new BusinessException("Apenas solicitações aprovadas podem ser canceladas por esta ação.");
        }
        var snapshotAnterior = auditService.snapshotOf(solicitacao);

        if (solicitacao.getCliente() != null) {
            sessaoRepository.findFirstByClienteIdAndDataHoraInicioAndStatus(
                    solicitacao.getCliente().getId(),
                    solicitacao.getDataHoraInicio(),
                    com.fisiogestao.agenda.SessaoStatus.AGENDADA).ifPresent(sessao -> {
                        if (Boolean.TRUE.equals(sessao.getPlanoConsumoRegistrado())
                                && sessao.getPlanoSessao() != null) {
                            planoSessaoService.estornarSessao(sessao.getPlanoSessao().getId());
                        }
                        sessao.setStatus(com.fisiogestao.agenda.SessaoStatus.CANCELADA);
                        sessaoRepository.save(sessao);
                    });
        }

        horarioBloqueadoRepository.findBySolicitacaoId(solicitacaoId).ifPresent(bloqueio -> {
            bloqueio.setStatus(HorarioBloqueadoStatus.CANCELADO);
            horarioBloqueadoRepository.save(bloqueio);
        });

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
                null,
                "Solicitação aprovada cancelada manualmente.",
                AuditEventOrigin.MANUAL);

        agendamentoEmailNotificationService.notificarCancelado(solicitacao);
    }

    @Transactional
    public void restaurarSolicitacao(UUID solicitacaoId) {
        SolicitacaoAgendamentoEntity solicitacao = solicitacaoAgendamentoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new BusinessException("Solicitação não encontrada."));
        if (solicitacao.getStatus() != SolicitacaoStatus.REJEITADA
                && solicitacao.getStatus() != SolicitacaoStatus.CANCELADA) {
            throw new BusinessException("Apenas solicitações rejeitadas ou canceladas podem ser restauradas.");
        }
        var snapshotAnterior = auditService.snapshotOf(solicitacao);

        agendaService.validarHorarioDisponivel(solicitacao.getDataHoraInicio(), solicitacao.getDataHoraFim(), null,
                null);

        horarioBloqueadoRepository.findBySolicitacaoId(solicitacaoId).ifPresentOrElse(bloqueio -> {
            bloqueio.setDataHoraInicio(solicitacao.getDataHoraInicio());
            bloqueio.setDataHoraFim(solicitacao.getDataHoraFim());
            bloqueio.setStatus(HorarioBloqueadoStatus.PENDENTE);
            bloqueio.setMotivo("Reserva reativada pelo administrador");
            horarioBloqueadoRepository.save(bloqueio);
        }, () -> {
            HorarioBloqueadoEntity novo = HorarioBloqueadoEntity.builder()
                    .dataHoraInicio(solicitacao.getDataHoraInicio())
                    .dataHoraFim(solicitacao.getDataHoraFim())
                    .status(HorarioBloqueadoStatus.PENDENTE)
                    .origem("AGENDAMENTO_ONLINE")
                    .motivo("Reserva provisória reativada pelo administrador")
                    .solicitacao(solicitacao)
                    .build();
            horarioBloqueadoRepository.save(novo);
        });

        solicitacao.setStatus(SolicitacaoStatus.PENDENTE);
        SolicitacaoAgendamentoEntity salva = solicitacaoAgendamentoRepository.save(solicitacao);
        auditService.recordManual(
                AuditModule.SOLICITACOES,
                "solicitacao_agendamento",
                salva.getId().toString(),
                auditService.describeOf(salva),
                AuditActionType.RESTORE,
                snapshotAnterior,
                auditService.snapshotOf(salva),
                null,
                "Solicitação restaurada para pendente.",
                AuditEventOrigin.MANUAL);

    }

    @Transactional
    public void excluirSolicitacao(UUID solicitacaoId) {
        SolicitacaoAgendamentoEntity solicitacao = solicitacaoAgendamentoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new BusinessException("Solicitação não encontrada."));
        if (solicitacao.getStatus() == SolicitacaoStatus.APROVADA) {
            throw new BusinessException("Não é possível excluir uma solicitação aprovada. Cancele-a primeiro.");
        }
        if (solicitacao.getStatus() == SolicitacaoStatus.PENDENTE) {
            throw new BusinessException("Não é possível excluir uma solicitação pendente. Rejeite-a primeiro.");
        }

        horarioBloqueadoRepository.findBySolicitacaoId(solicitacaoId)
                .ifPresent(horarioBloqueadoRepository::delete);

        auditService.recordManual(
                AuditModule.SOLICITACOES,
                "solicitacao_agendamento",
                solicitacao.getId().toString(),
                auditService.describeOf(solicitacao),
                AuditActionType.DELETE_PHYSICAL,
                auditService.snapshotOf(solicitacao),
                null,
                null,
                "Solicitação excluída permanentemente.",
                AuditEventOrigin.MANUAL);

        solicitacaoAgendamentoRepository.delete(solicitacao);
    }

    @Transactional
    public void bloquearPeriodoDaSolicitacao(UUID solicitacaoId, String motivo, String usuario) {
        SolicitacaoAgendamentoEntity solicitacao = solicitacaoAgendamentoRepository
                .findByIdAndStatus(solicitacaoId, SolicitacaoStatus.PENDENTE)
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
                AuditEventOrigin.MANUAL);

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

    private String normalizarBuscaTelefone(String buscaTexto) {
        if (buscaTexto == null) {
            return null;
        }
        String somenteDigitos = buscaTexto.replaceAll("\\D", "");
        return somenteDigitos.isBlank() ? null : somenteDigitos;
    }

    private boolean correspondeDataHistorico(
            SolicitacaoAgendamentoEntity solicitacao,
            LocalDate inicioInclusive,
            LocalDate fimInclusive,
            ZoneId zoneId) {
        if (inicioInclusive == null && fimInclusive == null) {
            return true;
        }

        OffsetDateTime referencia = solicitacao.getUpdatedAt() != null
                ? solicitacao.getUpdatedAt()
                : solicitacao.getCreatedAt();
        if (referencia == null) {
            return false;
        }

        LocalDate dataReferencia = referencia.atZoneSameInstant(zoneId).toLocalDate();
        if (inicioInclusive != null && dataReferencia.isBefore(inicioInclusive)) {
            return false;
        }
        return fimInclusive == null || !dataReferencia.isAfter(fimInclusive);
    }

    private boolean correspondeBuscaHistorico(
            SolicitacaoAgendamentoEntity solicitacao,
            String buscaTexto,
            String buscaTelefone) {
        if (buscaTexto == null) {
            return true;
        }

        String nomePaciente = texto(solicitacao.getNomePaciente());
        boolean nomeCorresponde = nomePaciente != null && nomePaciente.toLowerCase().contains(buscaTexto.toLowerCase());
        boolean telefoneCorresponde = buscaTelefone != null
                && solicitacao.getTelefone() != null
                && solicitacao.getTelefone().contains(buscaTelefone);
        return nomeCorresponde || telefoneCorresponde;
    }

    private void validarFormularioPublico(AgendamentoOnlineForm form) {
        String localAtendimento = normalizarLocalAtendimento(form.getLocalAtendimento());
        normalizarDocumento(form.getCpf(), 11, "Informe um CPF válido com 11 dígitos.");
        if ("DOMICILIO".equals(localAtendimento)) {
            String rua = resolverTexto(form.getRua(), null);
            String numero = resolverTexto(form.getNumero(), null);
            String bairro = resolverTexto(form.getBairro(), null);
            String cidade = resolverTexto(form.getCidade(), null);
            String estado = normalizarEstado(form.getEstado());

            if (rua == null || numero == null || bairro == null || cidade == null || estado == null) {
                throw new BusinessException("Informe o endereço completo para atendimento em domicílio.");
            }
        }
    }

    private PacienteForm montarFormularioPaciente(SolicitacaoAgendamentoEntity solicitacao,
            AprovacaoSolicitacaoForm form) {
        PacienteForm pacienteForm = new PacienteForm();
        pacienteForm.setNomeCompleto(
                resolverTexto(form != null ? form.getNomeCompleto() : null, solicitacao.getNomePaciente()));
        pacienteForm.setTelefone(resolverTexto(form != null ? form.getTelefone() : null, solicitacao.getTelefone()));
        pacienteForm.setEmail(resolverTexto(form != null ? form.getEmail() : null, solicitacao.getEmail()));
        pacienteForm.setCpf(resolverTexto(form != null ? form.getCpf() : null, solicitacao.getCpf()));
        pacienteForm.setCep(resolverTexto(form != null ? form.getCep() : null, solicitacao.getCep()));
        pacienteForm.setRua(resolverTexto(form != null ? form.getRua() : null, solicitacao.getRua()));
        pacienteForm.setNumero(resolverTexto(form != null ? form.getNumero() : null, solicitacao.getNumero()));
        pacienteForm.setBairro(resolverTexto(form != null ? form.getBairro() : null, solicitacao.getBairro()));
        pacienteForm.setCidade(resolverTexto(form != null ? form.getCidade() : null, solicitacao.getCidade()));
        pacienteForm.setEstado(resolverTexto(form != null ? form.getEstado() : null, solicitacao.getEstado()));
        pacienteForm.setObservacoes(solicitacao.getObservacoes());
        return pacienteForm;
    }

    private DadosEndereco resolverEnderecoSolicitacao(AgendamentoOnlineForm form, ClienteEntity clienteExistente) {
        String localAtendimento = normalizarLocalAtendimento(form.getLocalAtendimento());
        if (!"DOMICILIO".equals(localAtendimento)) {
            return new DadosEndereco(null, null, null, null, null, null);
        }

        String cep = resolverDocumento(form.getCep(), clienteExistente != null ? clienteExistente.getCep() : null, 8);
        String rua = resolverTexto(form.getRua(), clienteExistente != null ? clienteExistente.getRua() : null);
        String numero = resolverTexto(form.getNumero(), clienteExistente != null ? clienteExistente.getNumero() : null);
        String bairro = resolverTexto(form.getBairro(), clienteExistente != null ? clienteExistente.getBairro() : null);
        String cidade = resolverTexto(form.getCidade(), clienteExistente != null ? clienteExistente.getCidade() : null);
        String estado = resolverTexto(
                normalizarEstado(form.getEstado()),
                clienteExistente != null ? clienteExistente.getEstado() : null);

        if (rua == null || numero == null || bairro == null || cidade == null || estado == null) {
            throw new BusinessException("Informe o endereço completo para atendimento em domicílio.");
        }

        return new DadosEndereco(cep, rua, numero, bairro, cidade, estado);
    }

    private String resolverTexto(String valorPrincipal, String fallback) {
        String principal = texto(valorPrincipal);
        if (principal != null) {
            return principal;
        }
        return texto(fallback);
    }

    private String resolverDocumento(String valorPrincipal, String fallback, int tamanho) {
        String principal = normalizarDocumentoOpcional(valorPrincipal, tamanho);
        if (principal != null) {
            return principal;
        }
        return normalizarDocumentoOpcional(fallback, tamanho);
    }

    private String normalizarDocumento(String valor, int tamanho, String mensagemErro) {
        String digits = normalizarDocumentoOpcional(valor, tamanho);
        if (digits == null) {
            return null;
        }
        if (digits.length() != tamanho) {
            throw new BusinessException(mensagemErro);
        }
        return digits;
    }

    private String normalizarDocumentoOpcional(String valor, int tamanho) {
        if (valor == null) {
            return null;
        }
        String digits = valor.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }
        if (digits.length() != tamanho) {
            return null;
        }
        return digits;
    }

    private String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        String valor = estado.trim().toUpperCase();
        if (valor.length() != 2) {
            throw new BusinessException("Selecione um estado válido.");
        }
        return valor;
    }

    private String normalizarLocalAtendimento(String valor) {
        String local = texto(valor);
        if (local == null) {
            throw new BusinessException("Selecione onde deseja o atendimento.");
        }

        String normalizado = local.trim().toUpperCase()
                .replace("Í", "I")
                .replace("Ç", "C")
                .replace("Ã", "A");

        if ("CLINICA".equals(normalizado)) {
            return "CLINICA";
        }
        if ("DOMICILIO".equals(normalizado)) {
            return "DOMICILIO";
        }
        throw new BusinessException("Selecione um local de atendimento válido.");
    }

    private String montarObservacoesSessao(SolicitacaoAgendamentoEntity solicitacao) {
        String observacoes = texto(solicitacao.getObservacoes());
        if (!solicitacao.isAtendimentoDomiciliar()) {
            return observacoes;
        }

        String endereco = texto(solicitacao.getEnderecoResumo());
        if (endereco == null) {
            return observacoes;
        }
        if (observacoes == null) {
            return "Endereço do atendimento: " + endereco;
        }
        return observacoes + "\nEndereço do atendimento: " + endereco;
    }

    private record IntervaloPeriodo(OffsetDateTime inicio, OffsetDateTime fim) {
    }

    private record DadosEndereco(String cep, String rua, String numero, String bairro, String cidade, String estado) {
    }

    public record ResumoSolicitacoes(
            long totalPendentes,
            long totalAprovadas,
            long totalNegadas,
            long totalGeral,
            int taxaAprovacao) {
    }
}
