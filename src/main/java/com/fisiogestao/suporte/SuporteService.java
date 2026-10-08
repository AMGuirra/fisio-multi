package com.fisiogestao.suporte;

import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.empresa.EmpresaDadosService;
import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.empresa.EmpresaRepository;
import com.fisiogestao.gestor.NotificacaoAdminService;
import com.fisiogestao.gestor.TipoNotificacaoGestor;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SuporteService {

    private final SolicitacaoSuporteRepository repository;
    private final EmpresaRepository empresaRepository;
    private final EmpresaDadosService dadosService;
    private final NotificacaoAdminService notificacaoAdminService;
    private final AuditService auditService;

    /**
     * Registra uma solicitação pública. A empresa é vinculada apenas quando o CNPJ (ou o CPF do
     * responsável, se único) corresponder a exatamente uma clínica — e o solicitante nunca é
     * informado se houve ou não correspondência.
     */
    public SolicitacaoSuporteEntity registrarPublica(String nome, String cpf, String cnpj, String email,
                                                     String descricao, String ip) {
        String nomeNormalizado = dadosService.normalizarNome(nome, "Informe seu nome.");
        String cpfNormalizado = dadosService.normalizarCpf(cpf);
        String cnpjNormalizado = dadosService.normalizarCnpj(cnpj);
        dadosService.exigirCpfOuCnpj(cpfNormalizado, cnpjNormalizado);
        String emailNormalizado = dadosService.normalizarEmail(email, true);
        String descricaoNormalizada = validarDescricao(descricao);

        SolicitacaoSuporteEntity salva = TenantContext.executarSemEmpresa(() -> salvarNovaSolicitacao(
                SolicitacaoSuporteEntity.builder()
                        .empresaId(identificarEmpresa(cpfNormalizado, cnpjNormalizado).map(EmpresaEntity::getId).orElse(null))
                        .nome(nomeNormalizado)
                        .cpf(cpfNormalizado)
                        .cnpj(cnpjNormalizado)
                        .email(emailNormalizado)
                        .descricao(descricaoNormalizada)
                        .ipOrigem(ip == null || ip.length() <= 64 ? ip : ip.substring(0, 64))
                        .build()));

        Map<String, String> detalhes = new LinkedHashMap<>();
        detalhes.put("Solicitante", salva.getNome());
        detalhes.put("E-mail", salva.getEmail());
        detalhes.put("Empresa identificada", salva.getEmpresaId() == null ? "Não" : "Sim");
        detalhes.put("Relato", salva.getDescricao());
        notificacaoAdminService.notificar(TipoNotificacaoGestor.SUPORTE,
                "Nova solicitação de suporte de " + salva.getNome(),
                "Uma nova solicitação de suporte foi registrada na página pública.",
                salva.getEmpresaId(), salva.getId(), "/gestor/suporte/" + salva.getId(), detalhes);
        return salva;
    }

    private SolicitacaoSuporteEntity salvarNovaSolicitacao(SolicitacaoSuporteEntity entity) {
        SolicitacaoSuporteEntity salva = repository.save(entity);
        auditService.recordManual(AuditModule.GESTOR, "solicitacao_suporte", salva.getId().toString(),
                "Suporte - " + salva.getNome(), AuditActionType.CREATE, null,
                Map.of("status", salva.getStatus().name(), "empresaIdentificada", salva.getEmpresaId() != null),
                null, "Solicitação de suporte pública registrada.", AuditEventOrigin.ONLINE);
        return salva;
    }

    public Optional<EmpresaEntity> identificarEmpresa(String cpf, String cnpj) {
        if (cnpj != null) {
            Optional<EmpresaEntity> porCnpj = empresaRepository.findFirstByCnpj(cnpj);
            if (porCnpj.isPresent()) {
                return porCnpj;
            }
        }
        if (cpf != null) {
            List<EmpresaEntity> porCpf = empresaRepository.findByCpf(cpf);
            if (porCpf.size() == 1) {
                return Optional.of(porCpf.getFirst());
            }
        }
        return Optional.empty();
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional(readOnly = true)
    public Page<SolicitacaoSuporteEntity> buscar(String termo, StatusSolicitacaoSuporte status, int pagina) {
        String texto = termo == null ? "" : termo.trim().toLowerCase(Locale.ROOT);
        String digitos = texto.replaceAll("\\D", "");
        return repository.buscar(status, "%" + texto + "%", digitos.isEmpty() ? "#" : "%" + digitos + "%",
                PageRequest.of(Math.max(pagina, 0), 20, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional(readOnly = true)
    public SolicitacaoSuporteEntity buscarPorId(UUID id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException("Solicitação não encontrada."));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public SolicitacaoSuporteEntity atualizarStatus(UUID id, StatusSolicitacaoSuporte novoStatus, String observacao) {
        if (novoStatus == null) {
            throw new BusinessException("Selecione o novo status.");
        }
        SolicitacaoSuporteEntity solicitacao = buscarPorId(id);
        StatusSolicitacaoSuporte anterior = solicitacao.getStatus();
        solicitacao.setStatus(novoStatus);
        if (observacao != null && !observacao.isBlank()) {
            solicitacao.setObservacaoGestor(observacao.trim());
        }
        SolicitacaoSuporteEntity salva = repository.save(solicitacao);
        Map<String, Object> antes = new LinkedHashMap<>();
        antes.put("status", anterior.name());
        Map<String, Object> depois = new LinkedHashMap<>();
        depois.put("status", novoStatus.name());
        depois.put("observacaoGestor", salva.getObservacaoGestor());
        auditService.recordManual(AuditModule.GESTOR, "solicitacao_suporte", salva.getId().toString(),
                "Suporte - " + salva.getNome(), AuditActionType.UPDATE, antes, depois, null,
                "Status da solicitação de suporte alterado.", AuditEventOrigin.MANUAL);
        return salva;
    }

    public static String validarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new BusinessException("Descreva a sua solicitação.");
        }
        String normalizada = descricao.trim();
        if (normalizada.length() > SolicitacaoSuporteEntity.LIMITE_DESCRICAO) {
            throw new BusinessException("A descrição deve ter no máximo 500 caracteres.");
        }
        return normalizada;
    }
}
