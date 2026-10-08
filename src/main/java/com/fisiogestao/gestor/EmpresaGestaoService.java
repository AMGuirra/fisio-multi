package com.fisiogestao.gestor;

import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.empresa.ConfiguracaoPlataformaService;
import com.fisiogestao.empresa.EmpresaDadosService;
import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.empresa.EmpresaProvisionamentoService;
import com.fisiogestao.empresa.EmpresaRepository;
import com.fisiogestao.empresa.EmpresaStatus;
import com.fisiogestao.empresa.PeriodoTesteService;
import com.fisiogestao.gestor.form.EmpresaForm;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Operações do SUPER_ADMIN sobre empresas: cadastro, edição, status e período de teste.
 * Toda alteração gera auditoria da plataforma (empresa_id nulo, entidade "empresa").
 */
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class EmpresaGestaoService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmpresaRepository empresaRepository;
    private final EmpresaDadosService dadosService;
    private final EmpresaProvisionamentoService provisionamentoService;
    private final PeriodoTesteService periodoTesteService;
    private final ConfiguracaoPlataformaService configuracaoPlataformaService;
    private final AuditService auditService;
    private final NotificacaoAdminService notificacaoAdminService;

    @Transactional(readOnly = true)
    public Page<EmpresaEntity> buscar(String termo, EmpresaStatus status, int pagina) {
        String texto = termo == null ? "" : termo.trim().toLowerCase(Locale.ROOT);
        String digitos = texto.replaceAll("\\D", "");
        return empresaRepository.buscar(
                status,
                "%" + texto + "%",
                digitos.isEmpty() ? "#" : "%" + digitos + "%",
                PageRequest.of(Math.max(pagina, 0), 20, Sort.by(Sort.Direction.ASC, "nome"))
        );
    }

    @Transactional(readOnly = true)
    public EmpresaEntity buscarPorId(UUID id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Empresa não encontrada."));
    }

    public EmpresaEntity criar(EmpresaForm form) {
        EmpresaEntity nova = new EmpresaEntity();
        preencherDados(nova, form, null);
        EmpresaStatus status = form.getStatus() == null ? EmpresaStatus.TESTE : form.getStatus();
        aplicarStatusInicial(nova, status, form);

        EmpresaEntity salva = provisionamentoService.provisionar(nova, null);
        auditar(salva, AuditActionType.CREATE, null, snapshot(salva), null, "Empresa cadastrada pelo Gestor.");
        return salva;
    }

    @Transactional
    public EmpresaEntity atualizarDados(UUID id, EmpresaForm form) {
        EmpresaEntity empresa = buscarPorId(id);
        verificarVersao(empresa, form.getVersao());
        Map<String, Object> antes = snapshot(empresa);
        preencherDados(empresa, form, id);
        EmpresaEntity salva = salvar(empresa);
        auditar(salva, AuditActionType.UPDATE, antes, snapshot(salva), null, "Dados cadastrais atualizados.");
        return salva;
    }

    @Transactional
    public EmpresaEntity ativar(UUID id, String observacao) {
        EmpresaEntity empresa = buscarPorId(id);
        EmpresaStatus anterior = empresa.getStatus();
        Map<String, Object> antes = snapshot(empresa);
        empresa.setStatus(EmpresaStatus.ATIVO);
        empresa.setMotivoBloqueio(null);
        EmpresaEntity salva = salvar(empresa);
        auditar(salva, AuditActionType.ACTIVATE, antes, snapshot(salva), observacao, "Empresa ativada (antes: " + anterior + ").");
        comunicarStatus(salva, "Acesso liberado", "O acesso da sua clínica ao FisioGestao está liberado.", true);
        return salva;
    }

    @Transactional
    public EmpresaEntity bloquear(UUID id, String motivo) {
        return negarAcesso(id, EmpresaStatus.BLOQUEADO, motivo, AuditActionType.INACTIVATE,
                "Empresa bloqueada.", "Acesso bloqueado", "O acesso da sua clínica ao FisioGestao foi bloqueado.");
    }

    @Transactional
    public EmpresaEntity inativar(UUID id, String motivo) {
        return negarAcesso(id, EmpresaStatus.INATIVO, motivo, AuditActionType.INACTIVATE,
                "Empresa inativada.", "Clínica inativada", "A sua clínica foi inativada no FisioGestao.");
    }

    /** Inicia (ou reinicia) o teste a partir de hoje com a quantidade de dias úteis informada. */
    @Transactional
    public EmpresaEntity iniciarTeste(UUID id, Integer dias) {
        int quantidade = dias == null ? configuracaoPlataformaService.diasUteisTestePadrao() : dias;
        EmpresaEntity empresa = buscarPorId(id);
        Map<String, Object> antes = snapshot(empresa);
        periodoTesteService.iniciarTeste(empresa, quantidade, PeriodoTesteService.hoje());
        EmpresaEntity salva = salvar(empresa);
        auditar(salva, AuditActionType.UPDATE, antes, snapshot(salva), null,
                "Teste iniciado/reiniciado com " + quantidade + " dia(s) útil(eis).");
        comunicarStatus(salva, "Período de teste liberado",
                "Sua clínica está em período de teste até " + DATA.format(salva.getDataFimTeste()
                        .atZoneSameInstant(PeriodoTesteService.zonaPlataforma())) + ".", true);
        return salva;
    }

    @Transactional
    public EmpresaEntity prorrogarTeste(UUID id, Integer diasAdicionais) {
        if (diasAdicionais == null) {
            throw new BusinessException("Informe quantos dias úteis deseja acrescentar.");
        }
        EmpresaEntity empresa = buscarEmTeste(id);
        Map<String, Object> antes = snapshot(empresa);
        periodoTesteService.prorrogar(empresa, diasAdicionais);
        EmpresaEntity salva = salvar(empresa);
        auditar(salva, AuditActionType.UPDATE, antes, snapshot(salva), null,
                "Teste prorrogado em " + diasAdicionais + " dia(s) útil(eis).");
        return salva;
    }

    /** Define o total de dias úteis do teste (maior = prorroga; menor = reduz), mantendo o início. */
    @Transactional
    public EmpresaEntity alterarDiasTeste(UUID id, Integer novaQuantidade) {
        if (novaQuantidade == null) {
            throw new BusinessException("Informe a quantidade de dias úteis do teste.");
        }
        EmpresaEntity empresa = buscarEmTeste(id);
        Map<String, Object> antes = snapshot(empresa);
        periodoTesteService.alterarQuantidadeDias(empresa, novaQuantidade);
        EmpresaEntity salva = salvar(empresa);
        auditar(salva, AuditActionType.UPDATE, antes, snapshot(salva), null,
                "Quantidade de dias úteis do teste alterada para " + novaQuantidade + ".");
        return salva;
    }

    @Transactional
    public EmpresaEntity definirDataFinalTeste(UUID id, LocalDate dataFinal) {
        EmpresaEntity empresa = buscarEmTeste(id);
        Map<String, Object> antes = snapshot(empresa);
        periodoTesteService.definirDataFinal(empresa, dataFinal);
        EmpresaEntity salva = salvar(empresa);
        auditar(salva, AuditActionType.UPDATE, antes, snapshot(salva), null,
                "Data final do teste definida manualmente para " + (dataFinal == null ? "-" : DATA.format(dataFinal)) + ".");
        return salva;
    }

    /** Usado pela conclusão de cancelamento: inativa e comunica, preservando todos os dados. */
    @Transactional
    public EmpresaEntity inativarPorCancelamento(UUID id, String observacao) {
        return negarAcesso(id, EmpresaStatus.INATIVO, observacao == null || observacao.isBlank()
                        ? "Cancelamento solicitado pela clínica." : observacao,
                AuditActionType.CANCEL, "Empresa inativada por conclusão de cancelamento.",
                "Cancelamento concluído",
                "O cancelamento da sua clínica no FisioGestao foi concluído. Os dados permanecem preservados.");
    }

    private EmpresaEntity negarAcesso(
            UUID id,
            EmpresaStatus status,
            String motivo,
            AuditActionType acao,
            String observacaoAuditoria,
            String tituloComunicado,
            String mensagemComunicado
    ) {
        if (motivo == null || motivo.isBlank()) {
            throw new BusinessException("Informe o motivo.");
        }
        EmpresaEntity empresa = buscarPorId(id);
        Map<String, Object> antes = snapshot(empresa);
        empresa.setStatus(status);
        empresa.setMotivoBloqueio(motivo.trim());
        EmpresaEntity salva = salvar(empresa);
        auditar(salva, acao, antes, snapshot(salva), motivo.trim(), observacaoAuditoria);
        comunicarStatus(salva, tituloComunicado, mensagemComunicado, false);
        return salva;
    }

    private void aplicarStatusInicial(EmpresaEntity empresa, EmpresaStatus status, EmpresaForm form) {
        switch (status) {
            case TESTE -> {
                int dias = form.getQuantidadeDiasUteisTeste() == null
                        ? configuracaoPlataformaService.diasUteisTestePadrao()
                        : form.getQuantidadeDiasUteisTeste();
                periodoTesteService.iniciarTeste(empresa, dias,
                        form.getDataInicioTeste() == null ? PeriodoTesteService.hoje() : form.getDataInicioTeste());
                if (form.getDataFimTeste() != null) {
                    periodoTesteService.definirDataFinal(empresa, form.getDataFimTeste());
                }
            }
            case BLOQUEADO, INATIVO -> {
                if (form.getMotivoBloqueio() == null || form.getMotivoBloqueio().isBlank()) {
                    throw new BusinessException("Informe o motivo do bloqueio/inativação.");
                }
                empresa.setStatus(status);
                empresa.setMotivoBloqueio(form.getMotivoBloqueio().trim());
            }
            case ATIVO -> empresa.setStatus(EmpresaStatus.ATIVO);
        }
    }

    private void preencherDados(EmpresaEntity empresa, EmpresaForm form, UUID idIgnorado) {
        empresa.setNome(dadosService.normalizarNome(form.getNome(), "Informe o nome da clínica."));
        empresa.setSlug(form.getSlug() == null || form.getSlug().isBlank()
                ? (idIgnorado == null ? dadosService.gerarSlugUnico(empresa.getNome()) : empresa.getSlug())
                : dadosService.validarSlug(form.getSlug(), idIgnorado));
        empresa.setNomeResponsavel(dadosService.normalizarNomeOpcional(form.getNomeResponsavel()));
        String cpf = dadosService.normalizarCpf(form.getCpf());
        String cnpj = dadosService.normalizarCnpj(form.getCnpj());
        dadosService.garantirCnpjDisponivel(cnpj, idIgnorado);
        empresa.setCpf(cpf);
        empresa.setCnpj(cnpj);
        empresa.setEmail(dadosService.normalizarEmail(form.getEmail(), false));
        empresa.setTelefone(dadosService.normalizarTelefone(form.getTelefone(), false));
    }

    private EmpresaEntity buscarEmTeste(UUID id) {
        EmpresaEntity empresa = buscarPorId(id);
        if (empresa.getStatus() != EmpresaStatus.TESTE) {
            throw new BusinessException("A empresa não está em período de teste. Use \"Iniciar teste\".");
        }
        return empresa;
    }

    private void verificarVersao(EmpresaEntity empresa, Long versaoFormulario) {
        if (versaoFormulario != null && !Objects.equals(versaoFormulario, empresa.getVersao())) {
            throw new BusinessException("Os dados desta empresa foram alterados por outra pessoa. Recarregue a página.");
        }
    }

    private EmpresaEntity salvar(EmpresaEntity empresa) {
        try {
            return empresaRepository.saveAndFlush(empresa);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BusinessException("Os dados desta empresa foram alterados por outra pessoa. Recarregue a página.");
        }
    }

    private void comunicarStatus(EmpresaEntity empresa, String titulo, String mensagem, boolean linkLogin) {
        Map<String, String> detalhes = new LinkedHashMap<>();
        detalhes.put("Clínica", empresa.getNome());
        detalhes.put("Código", empresa.getSlug());
        detalhes.put("Novo status", empresa.getStatus().getLabel());
        notificacaoAdminService.notificar(TipoNotificacaoGestor.STATUS_EMPRESA,
                titulo + ": " + empresa.getNome(), mensagem, empresa.getId(), empresa.getId(),
                "/gestor/empresas/" + empresa.getId(), detalhes);
        notificacaoAdminService.enviarEmailClinica(empresa, titulo, mensagem, linkLogin);
    }

    private void auditar(EmpresaEntity empresa, AuditActionType acao, Map<String, Object> antes,
                         Map<String, Object> depois, String motivo, String observacao) {
        TenantContext.executarSemEmpresa(() -> auditService.recordManual(
                AuditModule.GESTOR,
                "empresa",
                empresa.getId().toString(),
                empresa.getNome(),
                acao,
                antes,
                depois,
                motivo,
                observacao,
                AuditEventOrigin.MANUAL
        ));
    }

    public static Map<String, Object> snapshot(EmpresaEntity empresa) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("nome", empresa.getNome());
        mapa.put("slug", empresa.getSlug());
        mapa.put("status", empresa.getStatus() == null ? null : empresa.getStatus().name());
        mapa.put("motivoBloqueio", empresa.getMotivoBloqueio());
        mapa.put("dataInicioTeste", empresa.getDataInicioTeste());
        mapa.put("dataFimTeste", empresa.getDataFimTeste());
        mapa.put("quantidadeDiasUteisTeste", empresa.getQuantidadeDiasUteisTeste());
        mapa.put("possuiCpf", empresa.getCpf() != null);
        mapa.put("possuiCnpj", empresa.getCnpj() != null);
        mapa.put("email", empresa.getEmail());
        return mapa;
    }
}
