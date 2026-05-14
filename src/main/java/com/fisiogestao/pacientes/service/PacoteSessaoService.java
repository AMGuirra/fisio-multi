package com.fisiogestao.pacientes.service;

import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.TipoAgendamento;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.financeiro.LancamentoFinanceiroEntity;
import com.fisiogestao.financeiro.StatusLancamentoFinanceiro;
import com.fisiogestao.financeiro.repository.LancamentoFinanceiroRepository;
import com.fisiogestao.financeiro.service.FinanceiroService;
import com.fisiogestao.pacientes.PacoteSessaoEntity;
import com.fisiogestao.pacientes.PacoteSessaoStatus;
import com.fisiogestao.pacientes.form.PacoteConsumoForm;
import com.fisiogestao.pacientes.form.PacoteSessaoForm;
import com.fisiogestao.pacientes.repository.PacoteSessaoRepository;
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
public class PacoteSessaoService {

    private final PacoteSessaoRepository pacoteSessaoRepository;
    private final PacienteService pacienteService;
    private final FinanceiroService financeiroService;
    private final SessaoRepository sessaoRepository;
    private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;
    private final AuditService auditService;

    public List<PacoteSessaoEntity> listarPorCliente(UUID clienteId) {
        return pacoteSessaoRepository.findByClienteIdOrderByCreatedAtDesc(clienteId);
    }

    public List<PacoteSessaoEntity> listarDisponiveisParaAgenda() {
        return pacoteSessaoRepository.findByStatusOrderByCreatedAtDesc(PacoteSessaoStatus.ATIVO).stream()
                .filter(this::isDisponivel)
                .toList();
    }

    public List<PacoteSessaoEntity> listarDisponiveisParaAgenda(UUID clienteId) {
        return listarPorCliente(clienteId).stream()
                .filter(this::isDisponivel)
                .sorted(comparadorDisponibilidade())
                .toList();
    }

    public Optional<PacoteSessaoEntity> buscarPorId(UUID id) {
        return pacoteSessaoRepository.findById(id);
    }

    public Optional<PacoteSessaoEntity> buscarPrimeiroDisponivel(UUID clienteId) {
        return listarPorCliente(clienteId).stream()
                .filter(this::isDisponivel)
                .sorted(comparadorDisponibilidade())
                .findFirst();
    }

    @Transactional
    public PacoteSessaoEntity criar(UUID clienteId, PacoteSessaoForm form) {
        validarPeriodo(form.getDataInicio(), form.getDataValidade());

        PacoteSessaoEntity pacote = PacoteSessaoEntity.builder()
                .cliente(pacienteService.buscarPorId(clienteId))
                .nomePacote(form.getNomePacote().trim())
                .sessoesContratadas(form.getSessoesContratadas())
                .sessoesConsumidas(0)
                .valorTotal(form.getValorTotal())
                .dataInicio(form.getDataInicio())
                .dataValidade(form.getDataValidade())
                .status(PacoteSessaoStatus.ATIVO)
                .build();
        PacoteSessaoEntity salvo = pacoteSessaoRepository.save(pacote);
        financeiroService.registrarReceitaPacote(salvo);
        auditService.recordCreate(
                AuditModule.PACOTES,
                "pacote_sessao",
                salvo,
                null,
                "Plano de tratamento criado pelo fluxo vinculado ao paciente.",
                AuditEventOrigin.MANUAL
        );
        return salvo;
    }

    @Transactional
    public PacoteSessaoEntity criarViaCrud(PacoteConsumoForm form) {
        validarPeriodo(form.getDataInicio(), form.getDataValidade());

        PacoteSessaoEntity pacote = PacoteSessaoEntity.builder()
                .cliente(pacienteService.buscarPorId(form.getClienteId()))
                .nomePacote(form.getNomePacote().trim())
                .sessoesContratadas(form.getSessoesContratadas())
                .sessoesConsumidas(0)
                .valorTotal(form.getValorTotal())
                .dataInicio(form.getDataInicio())
                .dataValidade(form.getDataValidade())
                .status(statusAjustado(form.getStatus(), 0, form.getSessoesContratadas(), form.getDataValidade()))
                .build();
        PacoteSessaoEntity salvo = pacoteSessaoRepository.save(pacote);
        financeiroService.registrarReceitaPacote(salvo);
        auditService.recordCreate(
                AuditModule.PACOTES,
                "pacote_sessao",
                salvo,
                null,
                "Plano de tratamento criado pelo CRUD de planos de tratamento.",
                AuditEventOrigin.MANUAL
        );
        return salvo;
    }

    @Transactional
    public PacoteSessaoEntity atualizarViaCrud(UUID pacoteId, PacoteConsumoForm form) {
        PacoteSessaoEntity pacote = pacoteSessaoRepository.findById(pacoteId)
                .orElseThrow(() -> new BusinessException("Plano de tratamento não encontrado."));
        var snapshotAnterior = auditService.snapshotOf(pacote);

        validarPeriodo(form.getDataInicio(), form.getDataValidade());
        validarConsumo(pacote.getSessoesConsumidas(), form.getSessoesContratadas());
        if (form.getStatus() == PacoteSessaoStatus.CANCELADO) {
            sessaoRepository.deletarFuturasPorPacoteEStatus(pacoteId, SessaoStatus.AGENDADA, OffsetDateTime.now());
        }

        pacote.setCliente(pacienteService.buscarPorId(form.getClienteId()));
        pacote.setNomePacote(form.getNomePacote().trim());
        pacote.setSessoesContratadas(form.getSessoesContratadas());
        pacote.setValorTotal(form.getValorTotal());
        pacote.setDataInicio(form.getDataInicio());
        pacote.setDataValidade(form.getDataValidade());
        pacote.setStatus(statusAjustado(
                form.getStatus(),
                pacote.getSessoesConsumidas(),
                form.getSessoesContratadas(),
                form.getDataValidade()
        ));
        PacoteSessaoEntity salvo = pacoteSessaoRepository.save(pacote);
        auditService.recordManual(
                AuditModule.PACOTES,
                "pacote_sessao",
                salvo.getId().toString(),
                salvo.getNomePacote(),
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
    public void cancelar(UUID pacoteId) {
        PacoteSessaoEntity pacote = pacoteSessaoRepository.findById(pacoteId)
                .orElseThrow(() -> new BusinessException("Plano de tratamento não encontrado."));
        var snapshotAnterior = auditService.snapshotOf(pacote);

        int sessoesCanceladas = sessaoRepository.deletarFuturasPorPacoteEStatus(
                pacoteId, SessaoStatus.AGENDADA, OffsetDateTime.now()
        );

        pacote.setStatus(PacoteSessaoStatus.CANCELADO);
        PacoteSessaoEntity salvo = pacoteSessaoRepository.save(pacote);

        String detalhes = sessoesCanceladas > 0
                ? String.format("Cancelamento de plano de tratamento. %d sessão(ões) agendada(s) futura(s) removida(s) da agenda.", sessoesCanceladas)
                : "Cancelamento de plano de tratamento.";

        auditService.recordManual(
                AuditModule.PACOTES,
                "pacote_sessao",
                salvo.getId().toString(),
                salvo.getNomePacote(),
                AuditActionType.CANCEL,
                snapshotAnterior,
                auditService.snapshotOf(salvo),
                null,
                detalhes,
                AuditEventOrigin.MANUAL
        );
    }

    @Transactional
    public void excluir(UUID pacoteId) {
        PacoteSessaoEntity pacote = pacoteSessaoRepository.findById(pacoteId)
                .orElseThrow(() -> new BusinessException("Plano de tratamento não encontrado."));
        var snapshotAnterior = auditService.snapshotOf(pacote);

        desvincularSessoesDoPlano(pacote.getId());
        limparLancamentosDoPlano(pacote.getId());

        auditService.recordManual(
                AuditModule.PACOTES,
                "pacote_sessao",
                pacote.getId().toString(),
                pacote.getNomePacote(),
                AuditActionType.DELETE_PHYSICAL,
                snapshotAnterior,
                null,
                null,
                "Exclusão física de plano de tratamento.",
                AuditEventOrigin.MANUAL
        );
        pacoteSessaoRepository.delete(pacote);
    }

    private void desvincularSessoesDoPlano(UUID pacoteId) {
        List<SessaoEntity> sessoesVinculadas = sessaoRepository.findByPacoteSessaoIdOrderByDataHoraInicioDesc(pacoteId);
        if (sessoesVinculadas.isEmpty()) {
            return;
        }

        for (SessaoEntity sessao : sessoesVinculadas) {
            var snapshotAnterior = auditService.snapshotOf(sessao);
            sessao.setPacoteSessao(null);
            sessao.setTipoAgendamento(TipoAgendamento.AVULSO);
            sessao.setPacoteConsumoRegistrado(Boolean.FALSE);
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

    private void limparLancamentosDoPlano(UUID pacoteId) {
        List<LancamentoFinanceiroEntity> lancamentosVinculados = lancamentoFinanceiroRepository.findByPacoteSessaoId(pacoteId);
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
            lancamento.setPacoteSessao(null);
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
    public void reservarSessao(UUID pacoteId) {
        PacoteSessaoEntity pacote = validarDisponivel(pacoteId);
        var snapshotAnterior = auditService.snapshotOf(pacote);
        pacote.setSessoesConsumidas(pacote.getSessoesConsumidas() + 1);
        pacote.setStatus(statusAjustado(
                pacote.getStatus(),
                pacote.getSessoesConsumidas(),
                pacote.getSessoesContratadas(),
                pacote.getDataValidade()
        ));
        PacoteSessaoEntity salvo = pacoteSessaoRepository.save(pacote);
        auditService.recordManual(
                AuditModule.PACOTES,
                "pacote_sessao",
                salvo.getId().toString(),
                salvo.getNomePacote(),
                AuditActionType.UPDATE,
                snapshotAnterior,
                auditService.snapshotOf(salvo),
                null,
                "Reserva/baixa automática de sessão no plano de tratamento.",
                AuditEventOrigin.SISTEMA
        );
    }

    @Transactional
    public void estornarSessao(UUID pacoteId) {
        PacoteSessaoEntity pacote = pacoteSessaoRepository.findById(pacoteId)
                .orElseThrow(() -> new BusinessException("Plano de tratamento não encontrado."));
        if (pacote.getSessoesConsumidas() <= 0) {
            return;
        }
        var snapshotAnterior = auditService.snapshotOf(pacote);
        pacote.setSessoesConsumidas(pacote.getSessoesConsumidas() - 1);
        pacote.setStatus(statusAjustado(
                PacoteSessaoStatus.ATIVO,
                pacote.getSessoesConsumidas(),
                pacote.getSessoesContratadas(),
                pacote.getDataValidade()
        ));
        PacoteSessaoEntity salvo = pacoteSessaoRepository.save(pacote);
        auditService.recordManual(
                AuditModule.PACOTES,
                "pacote_sessao",
                salvo.getId().toString(),
                salvo.getNomePacote(),
                AuditActionType.RESTORE,
                snapshotAnterior,
                auditService.snapshotOf(salvo),
                null,
                "Estorno automático de sessão no plano de tratamento.",
                AuditEventOrigin.SISTEMA
        );
    }

    public PacoteSessaoEntity validarDisponivel(UUID pacoteId) {
        PacoteSessaoEntity pacote = pacoteSessaoRepository.findById(pacoteId)
                .orElseThrow(() -> new BusinessException("Plano de tratamento não encontrado."));
        if (!isDisponivel(pacote)) {
            throw new BusinessException("O plano de tratamento selecionado não possui sessões disponíveis.");
        }
        return pacote;
    }

    public PacoteSessaoEntity resolverPacoteParaSessao(UUID clienteId, UUID pacoteId, UUID pacoteAtualId) {
        if (pacoteId == null) {
            return buscarPrimeiroDisponivel(clienteId)
                    .orElseThrow(() -> new BusinessException(
                            "O paciente não possui plano de tratamento disponível. Cadastre ou renove um plano antes de confirmar a sessão."
                    ));
        }

        PacoteSessaoEntity pacote = buscarPorId(pacoteId)
                .orElseThrow(() -> new BusinessException("Plano de tratamento não encontrado."));
        if (!pacote.getCliente().getId().equals(clienteId)) {
            throw new BusinessException("O plano de tratamento selecionado não pertence ao paciente informado.");
        }
        if (pacoteAtualId != null && pacoteAtualId.equals(pacoteId)) {
            return pacote;
        }
        return validarDisponivel(pacoteId);
    }

    public int calcularSessoesDisponiveis(PacoteSessaoEntity pacote) {
        return Math.max(0, pacote.getSessoesContratadas() - pacote.getSessoesConsumidas());
    }

    private boolean isDisponivel(PacoteSessaoEntity pacote) {
        if (pacote.getStatus() != PacoteSessaoStatus.ATIVO) {
            return false;
        }
        if (pacote.getDataValidade() != null && pacote.getDataValidade().isBefore(LocalDate.now())) {
            return false;
        }
        return pacote.getSessoesConsumidas() < pacote.getSessoesContratadas();
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

    private Comparator<PacoteSessaoEntity> comparadorDisponibilidade() {
        return Comparator
                .comparing((PacoteSessaoEntity pacote) -> pacote.getDataValidade() == null)
                .thenComparing(PacoteSessaoEntity::getDataValidade, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PacoteSessaoEntity::getDataInicio)
                .thenComparing(PacoteSessaoEntity::getCreatedAt);
    }

    private PacoteSessaoStatus statusAjustado(
            PacoteSessaoStatus statusSolicitado,
            Integer sessoesConsumidas,
            Integer sessoesContratadas,
            LocalDate dataValidade
    ) {
        if (statusSolicitado == PacoteSessaoStatus.CANCELADO) {
            return PacoteSessaoStatus.CANCELADO;
        }
        if (sessoesConsumidas >= sessoesContratadas) {
            return PacoteSessaoStatus.CONCLUIDO;
        }
        if (dataValidade != null && dataValidade.isBefore(LocalDate.now())) {
            return PacoteSessaoStatus.EXPIRADO;
        }
        return PacoteSessaoStatus.ATIVO;
    }
}
