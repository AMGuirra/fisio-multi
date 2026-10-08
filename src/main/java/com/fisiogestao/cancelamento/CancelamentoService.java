package com.fisiogestao.cancelamento;

import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.auth.UserRole;
import com.fisiogestao.auth.UsuarioAutenticado;
import com.fisiogestao.empresa.EmpresaDadosService;
import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.empresa.EmpresaRepository;
import com.fisiogestao.gestor.EmpresaGestaoService;
import com.fisiogestao.gestor.NotificacaoAdminService;
import com.fisiogestao.gestor.TipoNotificacaoGestor;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.suporte.SuporteService;
import com.fisiogestao.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Solicitações de cancelamento: abertura pela clínica (autenticada ou pela página pública)
 * e decisão pelo SUPER_ADMIN. Abrir uma solicitação nunca altera o status da empresa nem
 * remove dados; somente a conclusão inativa a empresa (dados preservados).
 */
@Service
@RequiredArgsConstructor
public class CancelamentoService {

    public static final String TEXTO_CONFIRMACAO = "Entendo que o acesso poderá ser encerrado após análise.";
    private static final EnumSet<StatusSolicitacaoCancelamento> ABERTAS = EnumSet.of(
            StatusSolicitacaoCancelamento.PENDENTE,
            StatusSolicitacaoCancelamento.EM_ANALISE,
            StatusSolicitacaoCancelamento.CANCELAMENTO_AGENDADO
    );

    private final SolicitacaoCancelamentoRepository repository;
    private final EmpresaRepository empresaRepository;
    private final EmpresaDadosService dadosService;
    private final SuporteService suporteService;
    private final EmpresaGestaoService empresaGestaoService;
    private final NotificacaoAdminService notificacaoAdminService;
    private final AuditService auditService;

    /** Solicitação feita dentro do sistema: somente o ADMIN_CLINICA da própria empresa. */
    public SolicitacaoCancelamentoEntity solicitarPelaClinica(
            String nomeSolicitante,
            String emailContato,
            MotivoCancelamento motivo,
            String descricao,
            boolean confirmacao,
            String ip
    ) {
        UsuarioAutenticado usuario = TenantContext.usuarioAutenticado()
                .filter(u -> u.getRole() == UserRole.ADMIN_CLINICA && u.getEmpresaId() != null)
                .orElseThrow(() -> new AccessDeniedException("Somente o administrador da clínica pode solicitar o cancelamento."));
        UUID empresaId = usuario.getEmpresaId();
        if (repository.existsByEmpresaIdAndStatusIn(empresaId, ABERTAS)) {
            throw new BusinessException("Já existe uma solicitação de cancelamento em andamento para esta clínica.");
        }
        SolicitacaoCancelamentoEntity nova = montar(OrigemSolicitacaoCancelamento.SISTEMA, nomeSolicitante,
                emailContato, motivo, descricao, confirmacao, ip);
        nova.setEmpresaId(empresaId);
        nova.setSolicitanteUsuarioId(usuario.getId());
        return registrar(nova, usuario.getUsername());
    }

    /** Solicitação pela página pública (clínica sem acesso): empresa vinculada pelo documento, se possível. */
    public SolicitacaoCancelamentoEntity solicitarPublicamente(
            String nomeSolicitante,
            String emailContato,
            String cpf,
            String cnpj,
            MotivoCancelamento motivo,
            String descricao,
            boolean confirmacao,
            String ip
    ) {
        String cpfNormalizado = dadosService.normalizarCpf(cpf);
        String cnpjNormalizado = dadosService.normalizarCnpj(cnpj);
        dadosService.exigirCpfOuCnpj(cpfNormalizado, cnpjNormalizado);
        SolicitacaoCancelamentoEntity nova = montar(OrigemSolicitacaoCancelamento.PUBLICA, nomeSolicitante,
                emailContato, motivo, descricao, confirmacao, ip);
        nova.setCpf(cpfNormalizado);
        nova.setCnpj(cnpjNormalizado);
        nova.setEmpresaId(TenantContext.executarSemEmpresa(() ->
                suporteService.identificarEmpresa(cpfNormalizado, cnpjNormalizado)).map(EmpresaEntity::getId).orElse(null));
        return registrar(nova, "pagina-publica");
    }

    /** Últimas solicitações da clínica do usuário atual (exibidas em Config. Clínica). */
    @Transactional(readOnly = true)
    public List<SolicitacaoCancelamentoEntity> listarDaClinicaAtual() {
        UUID empresaId = TenantContext.empresaIdOuNull();
        return empresaId == null ? List.of() : repository.findTop5ByEmpresaIdOrderByCreatedAtDesc(empresaId);
    }

    // ── Gestor ───────────────────────────────────────────────────────────────────

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional(readOnly = true)
    public Page<SolicitacaoCancelamentoEntity> buscar(StatusSolicitacaoCancelamento status, UUID empresaId,
                                                     String responsavel, LocalDate de, LocalDate ate, int pagina) {
        var zona = com.fisiogestao.empresa.PeriodoTesteService.zonaPlataforma();
        OffsetDateTime inicio = (de == null ? LocalDate.of(2000, 1, 1) : de).atStartOfDay(zona).toOffsetDateTime();
        OffsetDateTime fim = (ate == null ? LocalDate.of(2999, 12, 31) : ate.plusDays(1)).atStartOfDay(zona).toOffsetDateTime();
        String texto = responsavel == null ? "" : responsavel.trim().toLowerCase(Locale.ROOT);
        return repository.buscar(status, empresaId, "%" + texto + "%", inicio, fim,
                PageRequest.of(Math.max(pagina, 0), 20, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional(readOnly = true)
    public SolicitacaoCancelamentoEntity detalhar(UUID id) {
        return repository.findWithHistoricoById(id)
                .orElseThrow(() -> new BusinessException("Solicitação de cancelamento não encontrada."));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public SolicitacaoCancelamentoEntity colocarEmAnalise(UUID id, String observacao) {
        return mudarStatus(id, StatusSolicitacaoCancelamento.EM_ANALISE, observacao, null);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public SolicitacaoCancelamentoEntity recusar(UUID id, String observacao) {
        if (observacao == null || observacao.isBlank()) {
            throw new BusinessException("Informe a resposta/justificativa da recusa.");
        }
        SolicitacaoCancelamentoEntity salva = mudarStatus(id, StatusSolicitacaoCancelamento.RECUSADA, observacao, null);
        comunicarClinica(salva, "Solicitação de cancelamento recusada",
                "A solicitação de cancelamento da sua clínica foi analisada e recusada. Resposta: " + observacao.trim());
        return salva;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public SolicitacaoCancelamentoEntity agendar(UUID id, LocalDate data, String observacao) {
        if (data == null || data.isBefore(com.fisiogestao.empresa.PeriodoTesteService.hoje())) {
            throw new BusinessException("Informe uma data de cancelamento a partir de hoje.");
        }
        SolicitacaoCancelamentoEntity salva = mudarStatus(id, StatusSolicitacaoCancelamento.CANCELAMENTO_AGENDADO, observacao, data);
        comunicarClinica(salva, "Cancelamento agendado",
                "O cancelamento da sua clínica foi agendado para " + data.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".");
        return salva;
    }

    /** Conclui: empresa INATIVO (sessões caem na próxima requisição), dados preservados. */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public SolicitacaoCancelamentoEntity concluir(UUID id, String observacao) {
        SolicitacaoCancelamentoEntity solicitacao = detalhar(id);
        if (solicitacao.getEmpresaId() == null) {
            throw new BusinessException("Vincule a solicitação a uma empresa antes de concluir o cancelamento.");
        }
        SolicitacaoCancelamentoEntity salva = mudarStatus(id, StatusSolicitacaoCancelamento.CANCELADA, observacao, null);
        salva.setConcluidaEm(OffsetDateTime.now());
        // Inativa a empresa, audita e envia confirmação à clínica e ao e-mail administrativo global.
        empresaGestaoService.inativarPorCancelamento(salva.getEmpresaId(), observacao);
        return salva;
    }

    /** Vincula manualmente uma solicitação pública à empresa correta. */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public SolicitacaoCancelamentoEntity vincularEmpresa(UUID id, UUID empresaId) {
        SolicitacaoCancelamentoEntity solicitacao = detalhar(id);
        EmpresaEntity empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new BusinessException("Empresa não encontrada."));
        solicitacao.setEmpresaId(empresa.getId());
        solicitacao.registrarHistorico(solicitacao.getStatus(), "Vinculada à empresa " + empresa.getNome() + ".", usuarioAtual());
        return repository.save(solicitacao);
    }

    /** Usado pelo job diário: conclui cancelamentos agendados cuja data chegou. */
    @Transactional
    public int concluirAgendadosVencidos(LocalDate hoje) {
        int total = 0;
        for (SolicitacaoCancelamentoEntity s : repository.findAll()) {
            if (s.getStatus() == StatusSolicitacaoCancelamento.CANCELAMENTO_AGENDADO
                    && s.getDataCancelamentoAgendada() != null
                    && !s.getDataCancelamentoAgendada().isAfter(hoje)
                    && s.getEmpresaId() != null) {
                StatusSolicitacaoCancelamento anterior = s.getStatus();
                s.setStatus(StatusSolicitacaoCancelamento.CANCELADA);
                s.setConcluidaEm(OffsetDateTime.now());
                s.registrarHistorico(anterior, "Concluído automaticamente na data agendada.", "sistema");
                repository.save(s);
                empresaRepository.findById(s.getEmpresaId()).ifPresent(empresa -> {
                    empresa.setStatus(com.fisiogestao.empresa.EmpresaStatus.INATIVO);
                    empresa.setMotivoBloqueio("Cancelamento agendado concluído.");
                    empresaRepository.save(empresa);
                    notificacaoAdminService.enviarEmailClinica(empresa, "Cancelamento concluído",
                            "O cancelamento da sua clínica no FisioGestao foi concluído. Os dados permanecem preservados.", false);
                });
                auditar(s, AuditActionType.CANCEL, anterior, "Cancelamento agendado concluído automaticamente.");
                total++;
            }
        }
        return total;
    }

    // ── internos ────────────────────────────────────────────────────────────────

    private SolicitacaoCancelamentoEntity montar(OrigemSolicitacaoCancelamento origem, String nome, String email,
                                                MotivoCancelamento motivo, String descricao, boolean confirmacao, String ip) {
        if (!confirmacao) {
            throw new BusinessException("Confirme que entende que o acesso poderá ser encerrado após análise.");
        }
        if (motivo == null) {
            throw new BusinessException("Selecione o motivo do cancelamento.");
        }
        String descricaoNormalizada = descricao == null || descricao.isBlank() ? null : descricao.trim();
        if (descricaoNormalizada != null && descricaoNormalizada.length() > SolicitacaoCancelamentoEntity.LIMITE_DESCRICAO) {
            throw new BusinessException("A descrição deve ter no máximo 500 caracteres.");
        }
        return SolicitacaoCancelamentoEntity.builder()
                .origem(origem)
                .nomeSolicitante(dadosService.normalizarNome(nome, "Informe o nome do solicitante."))
                .emailContato(dadosService.normalizarEmail(email, true))
                .motivo(motivo)
                .descricao(descricaoNormalizada)
                .ipOrigem(ip == null || ip.length() <= 64 ? ip : ip.substring(0, 64))
                .build();
    }

    private SolicitacaoCancelamentoEntity registrar(SolicitacaoCancelamentoEntity nova, String autor) {
        nova.registrarHistorico(null, "Solicitação aberta (" + nova.getOrigem().name().toLowerCase(Locale.ROOT) + ").", autor);
        UUID empresaDaSolicitacao = nova.getEmpresaId();
        SolicitacaoCancelamentoEntity salva = TenantContext.executarSemEmpresa(() -> repository.save(nova));
        // Auditoria visível para a clínica (quando identificada) e para a plataforma.
        Map<String, Object> dados = Map.of("status", salva.getStatus().name(), "motivo", salva.getMotivo().name(),
                "origem", salva.getOrigem().name());
        if (empresaDaSolicitacao != null) {
            TenantContext.executarComo(empresaDaSolicitacao, () -> auditService.recordManual(AuditModule.CONFIGURACOES,
                    "solicitacao_cancelamento", salva.getId().toString(), "Solicitação de cancelamento",
                    AuditActionType.CREATE, null, dados, salva.getMotivo().getLabel(),
                    "Solicitação de cancelamento registrada.", AuditEventOrigin.ONLINE));
        }
        TenantContext.executarSemEmpresa(() -> auditService.recordManual(AuditModule.GESTOR,
                "solicitacao_cancelamento", salva.getId().toString(), "Solicitação de cancelamento",
                AuditActionType.CREATE, null, dados, salva.getMotivo().getLabel(),
                "Solicitação de cancelamento registrada.", AuditEventOrigin.ONLINE));

        String nomeEmpresa = empresaDaSolicitacao == null ? "não identificada"
                : empresaRepository.findById(empresaDaSolicitacao).map(EmpresaEntity::getNome).orElse("-");
        Map<String, String> detalhes = new LinkedHashMap<>();
        detalhes.put("Empresa", nomeEmpresa);
        detalhes.put("Solicitante", salva.getNomeSolicitante());
        detalhes.put("E-mail", salva.getEmailContato());
        detalhes.put("Motivo", salva.getMotivo().getLabel());
        notificacaoAdminService.notificar(TipoNotificacaoGestor.CANCELAMENTO,
                "Solicitação de cancelamento: " + nomeEmpresa,
                "Uma clínica solicitou o cancelamento da conta. Nenhum dado foi alterado até a sua decisão.",
                empresaDaSolicitacao, salva.getId(), "/gestor/cancelamentos/" + salva.getId(), detalhes);
        return salva;
    }

    private SolicitacaoCancelamentoEntity mudarStatus(UUID id, StatusSolicitacaoCancelamento novo,
                                                     String observacao, LocalDate dataAgendada) {
        SolicitacaoCancelamentoEntity solicitacao = detalhar(id);
        if (solicitacao.getStatus().isEncerrada()) {
            throw new BusinessException("Esta solicitação já foi encerrada.");
        }
        StatusSolicitacaoCancelamento anterior = solicitacao.getStatus();
        solicitacao.setStatus(novo);
        if (dataAgendada != null) {
            solicitacao.setDataCancelamentoAgendada(dataAgendada);
        }
        String obs = observacao == null || observacao.isBlank() ? null : observacao.trim();
        if (obs != null) {
            solicitacao.setObservacaoGestor(obs);
        }
        solicitacao.registrarHistorico(anterior, obs, usuarioAtual());
        SolicitacaoCancelamentoEntity salva = repository.save(solicitacao);
        auditar(salva, novo == StatusSolicitacaoCancelamento.CANCELADA ? AuditActionType.CANCEL
                : novo == StatusSolicitacaoCancelamento.RECUSADA ? AuditActionType.REJECT : AuditActionType.UPDATE,
                anterior, obs);
        return salva;
    }

    private void auditar(SolicitacaoCancelamentoEntity s, AuditActionType acao, StatusSolicitacaoCancelamento anterior, String obs) {
        Map<String, Object> antes = new LinkedHashMap<>();
        antes.put("status", anterior == null ? null : anterior.name());
        Map<String, Object> depois = new LinkedHashMap<>();
        depois.put("status", s.getStatus().name());
        depois.put("dataCancelamentoAgendada", s.getDataCancelamentoAgendada());
        depois.put("empresaId", s.getEmpresaId());
        TenantContext.executarSemEmpresa(() -> auditService.recordManual(AuditModule.GESTOR, "solicitacao_cancelamento",
                s.getId().toString(), "Solicitação de cancelamento", acao, antes, depois, null, obs, AuditEventOrigin.MANUAL));
    }

    private void comunicarClinica(SolicitacaoCancelamentoEntity solicitacao, String titulo, String mensagem) {
        if (solicitacao.getEmpresaId() == null) {
            return;
        }
        empresaRepository.findById(solicitacao.getEmpresaId())
                .ifPresent(empresa -> notificacaoAdminService.enviarEmailClinica(empresa, titulo, mensagem, false));
    }

    private static String usuarioAtual() {
        return TenantContext.usuarioAutenticado().map(UsuarioAutenticado::getUsername).orElse("sistema");
    }
}
