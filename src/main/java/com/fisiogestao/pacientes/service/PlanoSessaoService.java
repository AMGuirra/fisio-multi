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
        if (!isDisponivel(plano)) {
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

    private boolean isDisponivel(PlanoSessaoEntity plano) {
        if (plano.getStatus() != PlanoSessaoStatus.ATIVO) {
            return false;
        }
        if (plano.getDataValidade() != null && plano.getDataValidade().isBefore(LocalDate.now())) {
            return false;
        }
        return plano.getSessoesConsumidas() < plano.getSessoesContratadas();
    }

    @Transactional(readOnly = true)
    public ReconciliacaoPreviewDTO previewReconciliacao(UUID planoId) {
        PlanoSessaoEntity plano = planoSessaoRepository.findById(planoId)
                .orElseThrow(() -> new BusinessException("Plano de tratamento não encontrado."));
        int atual = plano.getSessoesConsumidas() == null ? 0 : plano.getSessoesConsumidas();
        int correto = (int) sessaoRepository.countContabilizaveisPorPlano(planoId);
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
        sessaoRepository.marcarComoRegistradasPorPlano(planoId);
        int correto = (int) sessaoRepository.countContabilizaveisPorPlano(planoId);
        plano.setSessoesConsumidas(correto);
        plano.setStatus(statusAjustado(plano.getStatus(), correto,
                plano.getSessoesContratadas(), plano.getDataValidade()));
        planoSessaoRepository.save(plano);
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
