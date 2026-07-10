package com.fisiogestao.pacientes.service;

import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.TipoAgendamento;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.email.service.PacienteEmailNotificationService;
import com.fisiogestao.financeiro.LancamentoFinanceiroEntity;
import com.fisiogestao.financeiro.StatusLancamentoFinanceiro;
import com.fisiogestao.financeiro.repository.LancamentoFinanceiroRepository;
import com.fisiogestao.financeiro.service.FinanceiroService;
import com.fisiogestao.pacientes.PlanoSessaoEntity;
import com.fisiogestao.pacientes.PlanoSessaoStatus;
import com.fisiogestao.pacientes.dto.AjusteSessoesPreviewDTO;
import com.fisiogestao.pacientes.dto.ReconciliacaoPreviewDTO;
import com.fisiogestao.pacientes.form.PlanoConsumoForm;
import com.fisiogestao.pacientes.form.PlanoSessaoForm;
import com.fisiogestao.pacientes.repository.PlanoSessaoRepository;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanoSessaoService {

    private final PlanoSessaoRepository planoSessaoRepository;
    private final PacienteService pacienteService;
    private final FinanceiroService financeiroService;
    private final SessaoRepository sessaoRepository;
    private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;
    private final AuditService auditService;
    private final PacienteEmailNotificationService pacienteEmailNotificationService;

    public List<PlanoSessaoEntity> listarPorCliente(UUID clienteId) {
        return planoSessaoRepository.findByClienteIdOrderByCreatedAtDesc(clienteId);
    }

    public List<PlanoSessaoEntity> listarDisponiveisParaAgenda() {
        return planoSessaoRepository.findByStatusOrderByCreatedAtDesc(PlanoSessaoStatus.ATIVO).stream()
                .filter(this::isDisponivel)
                .toList();
    }

    public List<PlanoSessaoEntity> listarTodosParaDiagnosticoAgenda() {
        return planoSessaoRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<PlanoSessaoEntity> listarDisponiveisParaAgenda(UUID clienteId) {
        return listarPorCliente(clienteId).stream()
                .filter(this::isDisponivel)
                .sorted(comparadorDisponibilidade())
                .toList();
    }

    public Optional<PlanoSessaoEntity> buscarPorId(UUID id) {
        return planoSessaoRepository.findById(id);
    }

    public Optional<PlanoSessaoEntity> buscarPrimeiroDisponivel(UUID clienteId) {
        return listarPorCliente(clienteId).stream()
                .filter(this::isDisponivel)
                .sorted(comparadorDisponibilidade())
                .findFirst();
    }

    public Optional<PlanoSessaoEntity> buscarPrimeiroLivreParaAgenda(UUID clienteId) {
        return listarPorCliente(clienteId).stream()
                .filter(plano -> motivoIndisponibilidadeAgenda(plano) == null)
                .sorted(comparadorDisponibilidade())
                .findFirst();
    }

    @Transactional
    public PlanoSessaoEntity criar(UUID clienteId, PlanoSessaoForm form) {
        validarPeriodo(form.getDataInicio(), form.getDataValidade());

        PlanoSessaoEntity plano = PlanoSessaoEntity.builder()
                .cliente(pacienteService.buscarPorId(clienteId))
                .nomePlano(form.getNomePlano().trim())
                .sessoesContratadas(form.getSessoesContratadas())
                .sessoesConsumidas(0)
                .valorTotal(form.getValorTotal())
                .dataInicio(form.getDataInicio())
                .dataValidade(form.getDataValidade())
                .status(PlanoSessaoStatus.ATIVO)
                .build();
        PlanoSessaoEntity salvo = planoSessaoRepository.save(plano);
        financeiroService.registrarReceitaPlano(salvo);
        auditService.recordCreate(
                AuditModule.PLANOS,
                "plano_sessao",
                salvo,
                null,
                "Plano de tratamento criado pelo fluxo vinculado ao paciente.",
                AuditEventOrigin.MANUAL
        );
        return salvo;
    }

    @Transactional
    public PlanoSessaoEntity criarViaCrud(PlanoConsumoForm form) {
        return criarViaCrud(form, null);
    }

    @Transactional
    public PlanoSessaoEntity criarViaCrud(PlanoConsumoForm form, String profissionalResponsavel) {
        validarPeriodo(form.getDataInicio(), form.getDataValidade());

        PlanoSessaoEntity plano = PlanoSessaoEntity.builder()
                .cliente(pacienteService.buscarPorId(form.getClienteId()))
                .nomePlano(form.getNomePlano().trim())
                .sessoesContratadas(form.getSessoesContratadas())
                .sessoesConsumidas(0)
                .valorTotal(form.getValorTotal())
                .dataInicio(form.getDataInicio())
                .dataValidade(form.getDataValidade())
                .status(statusAjustado(form.getStatus(), 0, form.getSessoesContratadas(), form.getDataValidade()))
                .build();
        PlanoSessaoEntity salvo = planoSessaoRepository.save(plano);
        financeiroService.registrarReceitaPlano(salvo, profissionalResponsavel);
        auditService.recordCreate(
                AuditModule.PLANOS,
                "plano_sessao",
                salvo,
                null,
                "Plano de tratamento criado pelo CRUD de planos de tratamento.",
                AuditEventOrigin.MANUAL
        );
        return salvo;
    }

    @Transactional
    public PlanoSessaoEntity atualizarViaCrud(UUID planoId, PlanoConsumoForm form) {
        PlanoSessaoEntity plano = planoSessaoRepository.findById(planoId)
                .orElseThrow(() -> new BusinessException("Plano de tratamento não encontrado."));
        var snapshotAnterior = auditService.snapshotOf(plano);

        validarPeriodo(form.getDataInicio(), form.getDataValidade());
        validarConsumo(plano.getSessoesConsumidas(), form.getSessoesContratadas());
        if (form.getStatus() == PlanoSessaoStatus.CANCELADO) {
            sessaoRepository.deletarFuturasPorPlanoEStatus(planoId, SessaoStatus.AGENDADA, OffsetDateTime.now());
        }

        plano.setCliente(pacienteService.buscarPorId(form.getClienteId()));
        plano.setNomePlano(form.getNomePlano().trim());
        plano.setSessoesContratadas(form.getSessoesContratadas());
        plano.setValorTotal(form.getValorTotal());
        plano.setDataInicio(form.getDataInicio());
        plano.setDataValidade(form.getDataValidade());
        plano.setStatus(statusAjustado(
                form.getStatus(),
                plano.getSessoesConsumidas(),
                form.getSessoesContratadas(),
                form.getDataValidade()
        ));
        PlanoSessaoEntity salvo = planoSessaoRepository.save(plano);
        auditService.recordManual(
                AuditModule.PLANOS,
                "plano_sessao",
                salvo.getId().toString(),
                salvo.getNomePlano(),
                AuditActionType.UPDATE,
                snapshotAnterior,
                auditService.snapshotOf(salvo),
                null,
                "Atualização manual de plano de tratamento.",
                AuditEventOrigin.MANUAL
        );
        return salvo;
    }

    @Transactional
    public void cancelar(UUID planoId) {
        PlanoSessaoEntity plano = planoSessaoRepository.findById(planoId)
                .orElseThrow(() -> new BusinessException("Plano de tratamento não encontrado."));
        var snapshotAnterior = auditService.snapshotOf(plano);

        int sessoesCanceladas = sessaoRepository.deletarFuturasPorPlanoEStatus(
                planoId, SessaoStatus.AGENDADA, OffsetDateTime.now()
        );

        plano.setStatus(PlanoSessaoStatus.CANCELADO);
        PlanoSessaoEntity salvo = planoSessaoRepository.save(plano);

        String detalhes = sessoesCanceladas > 0
                ? String.format("Cancelamento de plano de tratamento. %d sessão(ões) agendada(s) futura(s) removida(s) da agenda.", sessoesCanceladas)
                : "Cancelamento de plano de tratamento.";

        auditService.recordManual(
                AuditModule.PLANOS,
                "plano_sessao",
                salvo.getId().toString(),
                salvo.getNomePlano(),
                AuditActionType.CANCEL,
                snapshotAnterior,
                auditService.snapshotOf(salvo),
                null,
                detalhes,
                AuditEventOrigin.MANUAL
        );
    }

    @Transactional
    public void excluir(UUID planoId) {
        PlanoSessaoEntity plano = planoSessaoRepository.findById(planoId)
                .orElseThrow(() -> new BusinessException("Plano de tratamento não encontrado."));
        var snapshotAnterior = auditService.snapshotOf(plano);

        validarExclusaoFisicaPermitida(plano.getId());
        desvincularRenovacoesDoPlano(plano.getId());
        desvincularSessoesDoPlano(plano.getId());
        limparLancamentosDoPlano(plano.getId());

        auditService.recordManual(
                AuditModule.PLANOS,
                "plano_sessao",
                plano.getId().toString(),
                plano.getNomePlano(),
                AuditActionType.DELETE_PHYSICAL,
                snapshotAnterior,
                null,
                null,
                "Exclusão física de plano de tratamento.",
                AuditEventOrigin.MANUAL
        );
        planoSessaoRepository.delete(plano);
    }

    private void validarExclusaoFisicaPermitida(UUID planoId) {
        if (sessaoRepository.existsByPlanoSessaoIdAndStatusIn(
                planoId,
                List.of(SessaoStatus.REALIZADA, SessaoStatus.FALTA_FATURADA)
        )) {
            throw new BusinessException("Plano com histórico de atendimento não pode ser excluído. Cancele ou desative para preservar o histórico.");
        }
        if (lancamentoFinanceiroRepository.existsByPlanoSessaoIdAndStatus(planoId, StatusLancamentoFinanceiro.PAGO)) {
            throw new BusinessException("Plano com lançamento financeiro pago não pode ser excluído. Cancele ou desative para preservar o histórico.");
        }
    }

    private void desvincularRenovacoesDoPlano(UUID planoId) {
        List<PlanoSessaoEntity> planosRenovados = planoSessaoRepository.findByPlanoAnteriorId(planoId);
        if (planosRenovados.isEmpty()) {
            return;
        }

        for (PlanoSessaoEntity renovado : planosRenovados) {
            var snapshotAnterior = auditService.snapshotOf(renovado);
            renovado.setPlanoAnterior(null);
            PlanoSessaoEntity salvo = planoSessaoRepository.save(renovado);
            auditService.recordManual(
                    AuditModule.PLANOS,
                    "plano_sessao",
                    salvo.getId().toString(),
                    salvo.getNomePlano(),
                    AuditActionType.UPDATE,
                    snapshotAnterior,
                    auditService.snapshotOf(salvo),
                    null,
                    "Desvinculacao de plano anterior antes da exclusao.",
                    AuditEventOrigin.MANUAL
            );
        }
        planoSessaoRepository.flush();
    }

    private void desvincularSessoesDoPlano(UUID planoId) {
        List<SessaoEntity> sessoesVinculadas = sessaoRepository.findByPlanoSessaoIdOrderByDataHoraInicioDesc(planoId);
        if (sessoesVinculadas.isEmpty()) {
            return;
        }

        for (SessaoEntity sessao : sessoesVinculadas) {
            var snapshotAnterior = auditService.snapshotOf(sessao);
            sessao.setPlanoSessao(null);
            sessao.setTipoAgendamento(TipoAgendamento.AVULSO);
            sessao.setPlanoConsumoRegistrado(Boolean.FALSE);
            SessaoEntity salvo = sessaoRepository.save(sessao);
            auditService.recordManual(
                    AuditModule.AGENDA,
                    "sessao",
                    salvo.getId().toString(),
                    auditService.describeOf(salvo),
                    AuditActionType.UPDATE,
                    snapshotAnterior,
                    auditService.snapshotOf(salvo),
                    null,
                    "Desvinculação de plano antes da exclusão.",
                    AuditEventOrigin.MANUAL
            );
        }
        sessaoRepository.flush();
    }

    private void limparLancamentosDoPlano(UUID planoId) {
        List<LancamentoFinanceiroEntity> lancamentosVinculados = lancamentoFinanceiroRepository.findByPlanoSessaoId(planoId);
        if (lancamentosVinculados.isEmpty()) {
            return;
        }

        List<LancamentoFinanceiroEntity> lancamentosRemoviveis = lancamentosVinculados.stream()
                .filter(lancamento -> lancamento.getStatus() != StatusLancamentoFinanceiro.PAGO)
                .toList();
        if (!lancamentosRemoviveis.isEmpty()) {
            lancamentoFinanceiroRepository.deleteAll(lancamentosRemoviveis);
        }

        List<LancamentoFinanceiroEntity> lancamentosPagos = lancamentosVinculados.stream()
                .filter(lancamento -> lancamento.getStatus() == StatusLancamentoFinanceiro.PAGO)
                .toList();
        for (LancamentoFinanceiroEntity lancamento : lancamentosPagos) {
            var snapshotAnterior = auditService.snapshotOf(lancamento);
            lancamento.setPlanoSessao(null);
            LancamentoFinanceiroEntity salvo = lancamentoFinanceiroRepository.save(lancamento);
            auditService.recordManual(
                    AuditModule.FINANCEIRO,
                    "lancamento_financeiro",
                    salvo.getId().toString(),
                    salvo.getDescricao(),
                    AuditActionType.UPDATE,
                    snapshotAnterior,
                    auditService.snapshotOf(salvo),
                    null,
                    "Desvinculação de plano antes da exclusão.",
                    AuditEventOrigin.MANUAL
            );
        }
        lancamentoFinanceiroRepository.flush();
    }

    @Transactional
    public void reservarSessao(UUID planoId) {
        reservarSessao(planoId, true, 1);
    }

    @Transactional
    public PlanoSessaoEntity reservarSessao(UUID planoId, boolean notificar, int quantidadeNotificada) {
        PlanoSessaoEntity plano = validarDisponivel(planoId);
        var snapshotAnterior = auditService.snapshotOf(plano);
        plano.setSessoesConsumidas(plano.getSessoesConsumidas() + 1);
        plano.setStatus(statusAjustado(
                plano.getStatus(),
                plano.getSessoesConsumidas(),
                plano.getSessoesContratadas(),
                plano.getDataValidade()
        ));
        PlanoSessaoEntity salvo = planoSessaoRepository.save(plano);
        auditService.recordManual(
                AuditModule.PLANOS,
                "plano_sessao",
                salvo.getId().toString(),
                salvo.getNomePlano(),
                AuditActionType.UPDATE,
                snapshotAnterior,
                auditService.snapshotOf(salvo),
                null,
                "Reserva/baixa automática de sessão no plano de tratamento.",
                AuditEventOrigin.SISTEMA
        );
        if (notificar && pacienteEmailNotificationService != null) {
            pacienteEmailNotificationService.notificarConsumoPlano(
                    salvo,
                    Math.max(quantidadeNotificada, 1),
                    "Sessão reservada/debitada no plano de tratamento."
            );
        }
        return salvo;
    }

    @Transactional
    public void estornarSessao(UUID planoId) {
        PlanoSessaoEntity plano = planoSessaoRepository.findById(planoId)
                .orElseThrow(() -> new BusinessException("Plano de tratamento não encontrado."));
        if (plano.getSessoesConsumidas() <= 0) {
            return;
        }
        var snapshotAnterior = auditService.snapshotOf(plano);
        plano.setSessoesConsumidas(plano.getSessoesConsumidas() - 1);
        plano.setStatus(statusAjustado(
                PlanoSessaoStatus.ATIVO,
                plano.getSessoesConsumidas(),
                plano.getSessoesContratadas(),
                plano.getDataValidade()
        ));
        PlanoSessaoEntity salvo = planoSessaoRepository.save(plano);
        auditService.recordManual(
                AuditModule.PLANOS,
                "plano_sessao",
                salvo.getId().toString(),
                salvo.getNomePlano(),
                AuditActionType.RESTORE,
                snapshotAnterior,
                auditService.snapshotOf(salvo),
                null,
                "Estorno automático de sessão no plano de tratamento.",
                AuditEventOrigin.SISTEMA
        );
        if (pacienteEmailNotificationService != null) {
            pacienteEmailNotificationService.notificarEstornoPlano(
                salvo,
                1,
                "Sessão estornada no plano de tratamento."
            );
        }
    }

    @Transactional(readOnly = true)
    public void notificarConsumoPlano(UUID planoId, int quantidade, String motivo) {
        PlanoSessaoEntity plano = planoSessaoRepository.findById(planoId)
                .orElseThrow(() -> new BusinessException("Plano de tratamento não encontrado."));
        if (pacienteEmailNotificationService != null) {
            pacienteEmailNotificationService.notificarConsumoPlano(plano, quantidade, motivo);
        }
    }

    public PlanoSessaoEntity validarDisponivel(UUID planoId) {
        PlanoSessaoEntity plano = planoSessaoRepository.findById(planoId)
                .orElseThrow(() -> new BusinessException("Plano de tratamento não encontrado."));
        String motivo = motivoIndisponibilidadeBasica(plano);
        if (motivo != null) {
            throw new BusinessException(motivo);
        }
        if (motivo != null) {
            throw new BusinessException("O plano de tratamento selecionado não possui sessões disponíveis.");
        }
        return plano;
    }

    public PlanoSessaoEntity resolverPlanoParaSessao(UUID clienteId, UUID planoId, UUID planoAtualId) {
        if (planoId == null) {
            return buscarPrimeiroDisponivel(clienteId)
                    .orElseThrow(() -> new BusinessException(
                            "O paciente não possui plano de tratamento disponível. Cadastre ou renove um plano antes de confirmar a sessão."
                    ));
        }

        PlanoSessaoEntity plano = buscarPorId(planoId)
                .orElseThrow(() -> new BusinessException("Plano de tratamento não encontrado."));
        if (!plano.getCliente().getId().equals(clienteId)) {
            throw new BusinessException("O plano de tratamento selecionado não pertence ao paciente informado.");
        }
        if (planoAtualId != null && planoAtualId.equals(planoId)) {
            return plano;
        }
        return validarDisponivel(planoId);
    }

    public int calcularSessoesDisponiveis(PlanoSessaoEntity plano) {
        return Math.max(0, plano.getSessoesContratadas() - plano.getSessoesConsumidas());
    }

    public int calcularSessoesDisponiveisParaAgenda(PlanoSessaoEntity plano) {
        return Math.max(0, calcularSessoesDisponiveis(plano) - (int) sessaoRepository.countAgendadasPorPlano(plano.getId()));
    }

    public boolean isDisponivelParaUso(PlanoSessaoEntity plano) {
        return motivoIndisponibilidadeBasica(plano) == null;
    }

    private String motivoIndisponibilidadeBasica(PlanoSessaoEntity plano) {
        if (plano.getStatus() == PlanoSessaoStatus.CANCELADO) {
            return "Plano cancelado.";
        }
        if (plano.getStatus() == PlanoSessaoStatus.CONCLUIDO) {
            return "Plano concluido, sem sessoes disponiveis.";
        }
        if (plano.getStatus() == PlanoSessaoStatus.EXPIRADO) {
            return "Plano expirado.";
        }
        if (plano.getDataValidade() != null && plano.getDataValidade().isBefore(LocalDate.now())) {
            return "Vigencia encerrada em " + plano.getDataValidade() + ". Crie ou renove um plano para novos atendimentos.";
        }
        if (plano.getSessoesConsumidas() >= plano.getSessoesContratadas()) {
            return "Todas as sessoes foram consumidas.";
        }
        return null;
    }

    public String motivoIndisponibilidadeAgenda(PlanoSessaoEntity plano) {
        String motivoBasico = motivoIndisponibilidadeBasica(plano);
        if (motivoBasico != null) {
            return motivoBasico;
        }
        if (plano.getStatus() == PlanoSessaoStatus.CANCELADO) {
            return "Plano cancelado.";
        }
        if (plano.getStatus() == PlanoSessaoStatus.CONCLUIDO) {
            return "Plano concluído, sem sessões disponíveis.";
        }
        if (plano.getStatus() == PlanoSessaoStatus.EXPIRADO) {
            return "Plano expirado.";
        }
        if (plano.getDataValidade() != null && plano.getDataValidade().isBefore(LocalDate.now())) {
            return "Vigência encerrada em " + plano.getDataValidade() + ".";
        }
        if (plano.getSessoesConsumidas() >= plano.getSessoesContratadas()) {
            return "Todas as sessões foram consumidas.";
        }
        int disponiveisAgenda = calcularSessoesDisponiveisParaAgenda(plano);
        if (disponiveisAgenda <= 0) {
            return "Saldo livre comprometido por sessões já agendadas.";
        }
        return null;
    }

    private boolean isDisponivel(PlanoSessaoEntity plano) {
        return isDisponivelParaUso(plano);
    }

    @Transactional(readOnly = true)
    public ReconciliacaoPreviewDTO previewReconciliacao(UUID planoId) {
        PlanoSessaoEntity plano = planoSessaoRepository.findById(planoId)
                .orElseThrow(() -> new BusinessException("Plano de tratamento não encontrado."));
        int atual = plano.getSessoesConsumidas() == null ? 0 : plano.getSessoesConsumidas();
        int correto = (int) sessaoRepository.countContabilizaveisPorPlano(planoId);
        // Inclui sessões de plano do paciente no período sem vínculo ao plano (órfãs)
        if (plano.getDataInicio() != null && plano.getDataValidade() != null) {
            OffsetDateTime inicio = plano.getDataInicio().atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
            OffsetDateTime fim = plano.getDataValidade().atTime(23, 59, 59).atOffset(java.time.ZoneOffset.UTC);
            correto += (int) sessaoRepository.countOrfasRealizadasNoPeriodo(
                    plano.getCliente().getId(), inicio, fim);
        }
        correto = Math.min(correto, valorSeguro(plano.getSessoesContratadas()));
        int naoRegistradas = (int) sessaoRepository.countNaoRegistradasPorPlano(planoId);
        return new ReconciliacaoPreviewDTO(planoId, atual, correto, naoRegistradas, atual != correto);
    }

    @Transactional
    public void executarReconciliacao(UUID planoId) {
        PlanoSessaoEntity plano = planoSessaoRepository.findById(planoId)
                .orElseThrow(() -> new BusinessException("Plano de tratamento não encontrado."));
        if (plano.getStatus() == PlanoSessaoStatus.CANCELADO) {
            throw new BusinessException("Não é possível reconciliar um plano cancelado.");
        }
        // Vincula sessões órfãs do período ao plano antes de contabilizar
        if (plano.getDataInicio() != null && plano.getDataValidade() != null) {
            OffsetDateTime inicio = plano.getDataInicio().atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
            OffsetDateTime fim = plano.getDataValidade().atTime(23, 59, 59).atOffset(java.time.ZoneOffset.UTC);
            sessaoRepository.vincularOrfasAoPlano(planoId, plano.getCliente().getId(), inicio, fim);
        }
        int totalContratado = valorSeguro(plano.getSessoesContratadas());
        int contabilizaveis = (int) sessaoRepository.countContabilizaveisPorPlano(planoId);
        int correto = Math.min(contabilizaveis, totalContratado);
        int pendentes = (int) sessaoRepository.countNaoRegistradasPorPlano(planoId);
        int jaRegistradas = Math.max(0, contabilizaveis - pendentes);
        int registrarAgora = Math.max(0, correto - jaRegistradas);
        List<SessaoEntity> sessoesParaRegistrar = sessaoRepository.findContabilizaveisNaoRegistradasPorPlano(planoId).stream()
                .limit(registrarAgora)
                .toList();
        for (SessaoEntity sessao : sessoesParaRegistrar) {
            sessao.setPlanoConsumoRegistrado(Boolean.TRUE);
            sessaoRepository.save(sessao);
        }
        plano.setSessoesConsumidas(correto);
        plano.setStatus(statusAjustado(plano.getStatus(), correto,
                plano.getSessoesContratadas(), plano.getDataValidade()));
        planoSessaoRepository.save(plano);
    }

    @Transactional(readOnly = true)
    public AjusteSessoesPreviewDTO previewAjusteSessoesPaciente(UUID pacienteId, UUID planoId) {
        PlanoSessaoEntity plano = buscarPlanoDoPaciente(pacienteId, planoId);
        List<SessaoEntity> pendentes = sessoesPendentesParaAjuste(plano);
        int consumoAtual = valorSeguro(plano.getSessoesConsumidas());
        int totalContratado = valorSeguro(plano.getSessoesContratadas());
        int saldoDisponivel = Math.max(0, totalContratado - consumoAtual);
        int ajustaveis = Math.min(saldoDisponivel, pendentes.size());
        int excedentes = Math.max(0, pendentes.size() - ajustaveis);
        return new AjusteSessoesPreviewDTO(
                pacienteId,
                planoId,
                consumoAtual,
                totalContratado,
                saldoDisponivel,
                pendentes.size(),
                ajustaveis,
                excedentes,
                excedentes > 0
        );
    }

    @Transactional
    public AjusteSessoesPreviewDTO executarAjusteSessoesPaciente(UUID pacienteId, UUID planoId) {
        PlanoSessaoEntity plano = buscarPlanoDoPaciente(pacienteId, planoId);
        if (plano.getStatus() == PlanoSessaoStatus.CANCELADO) {
            throw new BusinessException("Não é possível ajustar sessões de um plano cancelado.");
        }

        AjusteSessoesPreviewDTO preview = previewAjusteSessoesPaciente(pacienteId, planoId);
        if (preview.sessoesAjustaveis() <= 0) {
            return preview;
        }

        var snapshotPlanoAnterior = auditService.snapshotOf(plano);
        List<SessaoEntity> sessoesParaAjustar = sessoesPendentesParaAjuste(plano).stream()
                .limit(preview.sessoesAjustaveis())
                .toList();

        for (SessaoEntity sessao : sessoesParaAjustar) {
            var snapshotSessaoAnterior = auditService.snapshotOf(sessao);
            sessao.setPlanoSessao(plano);
            sessao.setPlanoConsumoRegistrado(Boolean.TRUE);
            SessaoEntity salva = sessaoRepository.save(sessao);
            auditService.recordManual(
                    AuditModule.AGENDA,
                    "sessao",
                    salva.getId().toString(),
                    auditService.describeOf(salva),
                    AuditActionType.UPDATE,
                    snapshotSessaoAnterior,
                    auditService.snapshotOf(salva),
                    null,
                    "Ajuste manual de consumo pelo painel de planos.",
                    AuditEventOrigin.MANUAL
            );
        }

        int novoConsumo = Math.min(
                valorSeguro(plano.getSessoesContratadas()),
                valorSeguro(plano.getSessoesConsumidas()) + sessoesParaAjustar.size()
        );
        plano.setSessoesConsumidas(novoConsumo);
        plano.setStatus(statusAjustado(
                plano.getStatus(),
                novoConsumo,
                plano.getSessoesContratadas(),
                plano.getDataValidade()
        ));
        PlanoSessaoEntity salvo = planoSessaoRepository.save(plano);
        auditService.recordManual(
                AuditModule.PLANOS,
                "plano_sessao",
                salvo.getId().toString(),
                salvo.getNomePlano(),
                AuditActionType.UPDATE,
                snapshotPlanoAnterior,
                auditService.snapshotOf(salvo),
                null,
                "Ajuste manual de sessoes realizadas no consumo do plano.",
                AuditEventOrigin.MANUAL
        );
        return preview;
    }

    private PlanoSessaoEntity buscarPlanoDoPaciente(UUID pacienteId, UUID planoId) {
        PlanoSessaoEntity plano = planoSessaoRepository.findById(planoId)
                .orElseThrow(() -> new BusinessException("Plano de tratamento não encontrado."));
        if (!plano.getCliente().getId().equals(pacienteId)) {
            throw new BusinessException("O plano selecionado não pertence ao paciente informado.");
        }
        return plano;
    }

    private List<SessaoEntity> sessoesPendentesParaAjuste(PlanoSessaoEntity plano) {
        OffsetDateTime inicio = plano.getDataInicio().atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        OffsetDateTime fim = (plano.getDataValidade() == null ? LocalDate.of(9999, 12, 31) : plano.getDataValidade())
                .atTime(23, 59, 59)
                .atOffset(java.time.ZoneOffset.UTC);
        return sessaoRepository.findPendentesDeConsumoDoPacienteNoPlano(
                plano.getCliente().getId(),
                plano.getId(),
                inicio,
                fim
        );
    }

    private void validarPeriodo(LocalDate dataInicio, LocalDate dataValidade) {
        if (dataValidade != null && dataValidade.isBefore(dataInicio)) {
            throw new BusinessException("A validade do plano de tratamento não pode ser anterior à data inicial.");
        }
    }

    private void validarConsumo(Integer sessoesConsumidas, Integer sessoesContratadas) {
        if (sessoesConsumidas > sessoesContratadas) {
            throw new BusinessException("As sessões consumidas não podem ser maiores do que as sessões contratadas.");
        }
    }

    private int valorSeguro(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private Comparator<PlanoSessaoEntity> comparadorDisponibilidade() {
        return Comparator
                .comparing((PlanoSessaoEntity plano) -> plano.getDataValidade() == null)
                .thenComparing(PlanoSessaoEntity::getDataValidade, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PlanoSessaoEntity::getDataInicio)
                .thenComparing(PlanoSessaoEntity::getCreatedAt);
    }

    private PlanoSessaoStatus statusAjustado(
            PlanoSessaoStatus statusSolicitado,
            Integer sessoesConsumidas,
            Integer sessoesContratadas,
            LocalDate dataValidade
    ) {
        if (statusSolicitado == PlanoSessaoStatus.CANCELADO) {
            return PlanoSessaoStatus.CANCELADO;
        }
        if (sessoesConsumidas >= sessoesContratadas) {
            return PlanoSessaoStatus.CONCLUIDO;
        }
        if (dataValidade != null && dataValidade.isBefore(LocalDate.now())) {
            return PlanoSessaoStatus.EXPIRADO;
        }
        return PlanoSessaoStatus.ATIVO;
    }
}
