package com.fisiogestao.email.service;

import com.fisiogestao.email.EmailScopeType;
import com.fisiogestao.tenant.TenantContext;
import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.email.EmailConfiguracaoEntity;
import com.fisiogestao.email.EmailLogType;
import com.fisiogestao.email.repository.EmailConfiguracaoRepository;
import com.fisiogestao.email.repository.EmailLogRepository;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailConfiguracaoService {

    private final EmailConfiguracaoRepository emailConfiguracaoRepository;
    private final EmailLogRepository emailLogRepository;
    private final SecretCryptoService secretCryptoService;
    private final SystemEmailService systemEmailService;
    private final AuditService auditService;
    private final SmtpEndpointValidator smtpEndpointValidator;

    /** SMTP ativo do escopo atual (clínica ou plataforma), sem fallback. */
    public Optional<EmailConfiguracaoEntity> buscarAtiva() {
        UUID empresaId = TenantContext.empresaIdOuNull();
        return empresaId == null
                ? emailConfiguracaoRepository.findFirstByEmpresaIdIsNullAndAtivoTrueOrderByUpdatedAtDesc()
                : emailConfiguracaoRepository.findFirstByEmpresaIdAndAtivoTrueOrderByUpdatedAtDesc(empresaId);
    }

    /** Indica se o escopo atual usa o SMTP global da plataforma por não ter um próprio. */
    public boolean usandoSmtpGlobal() {
        return TenantContext.possuiEmpresa() && buscarAtiva().isEmpty()
                && emailConfiguracaoRepository.findFirstByEmpresaIdIsNullAndAtivoTrueOrderByUpdatedAtDesc().isPresent();
    }

    public List<EmailConfiguracaoEntity> listarConfiguracoes() {
        UUID empresaId = TenantContext.empresaIdOuNull();
        return empresaId == null
                ? emailConfiguracaoRepository.findAllByEmpresaIdIsNullOrderByAtivoDescUpdatedAtDesc()
                : emailConfiguracaoRepository.findAllByEmpresaIdOrderByAtivoDescUpdatedAtDesc(empresaId);
    }

    public EmailConfiguracaoEntity buscarPorId(UUID id) {
        UUID empresaId = TenantContext.empresaIdOuNull();
        return (empresaId == null
                ? emailConfiguracaoRepository.findByIdAndEmpresaIdIsNull(id)
                : emailConfiguracaoRepository.findByIdAndEmpresaId(id, empresaId))
                .orElseThrow(() -> new BusinessException("Configuração de e-mail não encontrada."));
    }

    public List<com.fisiogestao.email.EmailLogEntity> listarHistorico() {
        UUID empresaId = TenantContext.empresaIdOuNull();
        return empresaId == null
                ? emailLogRepository.findTop50ByEmpresaIdIsNullOrderByCreatedAtDesc()
                : emailLogRepository.findTop50ByEmpresaIdOrderByCreatedAtDesc(empresaId);
    }

    @Transactional
    public EmailConfiguracaoEntity salvar(
            UUID id,
            String nomeRemetente,
            String emailRemetente,
            String senhaApp,
            boolean manterSenhaAtual,
            String smtpHost,
            Integer smtpPort,
            boolean usaAutenticacao,
            boolean usaTls,
            boolean usaSsl,
            String replyTo,
            boolean ativo,
            String observacoes
    ) {
        boolean novoCadastro = id == null;
        EmailConfiguracaoEntity entity = novoCadastro
                ? novaConfiguracaoNoEscopo()
                : buscarPorId(id);
        var snapshotAnterior = novoCadastro ? null : auditService.snapshotOf(entity);

        entity.setNomeRemetente(textoObrigatorio(nomeRemetente, "Informe o nome do remetente."));
        entity.setEmailRemetente(email(emailRemetente, "Informe o e-mail do remetente."));
        entity.setSmtpHost(smtpEndpointValidator.validar(smtpHost, smtpPort));
        entity.setSmtpPort(smtpPort);
        entity.setUsaAutenticacao(usaAutenticacao);
        entity.setUsaTls(usaTls);
        entity.setUsaSsl(usaSsl);
        entity.setReplyTo(emailOpcional(replyTo));
        entity.setAtivo(ativo);
        entity.setObservacoes(textoOpcional(observacoes));

        boolean senhaNovaInformada = senhaApp != null && !senhaApp.isBlank();
        if (manterSenhaAtual && !senhaNovaInformada) {
            if (entity.getSenhaAppCriptografada() == null || entity.getSenhaAppCriptografada().isBlank()) {
                throw new BusinessException("Informe a senha de app para concluir o cadastro SMTP.");
            }
        } else {
            entity.setSenhaAppCriptografada(secretCryptoService.encrypt(textoObrigatorio(
                    senhaApp,
                    "Informe a senha de app do Gmail."
            )));
        }

        if (Boolean.TRUE.equals(ativo)) {
            desativarDemaisConfiguracoes(id);
        }

        EmailConfiguracaoEntity salva = emailConfiguracaoRepository.save(entity);
        if (novoCadastro) {
            auditService.recordCreate(
                    AuditModule.CONFIGURACOES,
                    "email_configuracao",
                    salva,
                    null,
                    "Cadastro de configuração SMTP.",
                    AuditEventOrigin.MANUAL
            );
        } else {
            auditService.recordManual(
                    AuditModule.CONFIGURACOES,
                    "email_configuracao",
                    salva.getId().toString(),
                    auditService.describeOf(salva),
                    AuditActionType.UPDATE,
                    snapshotAnterior,
                    auditService.snapshotOf(salva),
                    null,
                    "Atualização de configuração SMTP.",
                    AuditEventOrigin.MANUAL
            );
        }
        return salva;
    }

    @Transactional
    public void definirAtiva(UUID id) {
        desativarDemaisConfiguracoes(id);
        EmailConfiguracaoEntity entity = buscarPorId(id);
        var snapshotAnterior = auditService.snapshotOf(entity);
        entity.setAtivo(true);
        EmailConfiguracaoEntity salva = emailConfiguracaoRepository.save(entity);
        auditService.recordManual(
                AuditModule.CONFIGURACOES,
                "email_configuracao",
                salva.getId().toString(),
                auditService.describeOf(salva),
                AuditActionType.ACTIVATE,
                snapshotAnterior,
                auditService.snapshotOf(salva),
                null,
                "Configuração SMTP ativada.",
                AuditEventOrigin.MANUAL
        );
    }

    public EmailDispatchResult testarEnvio(EmailConfiguracaoEntity configuracao, String destinatarioTeste, String usuario) {
        String email = email(destinatarioTeste, "Informe um e-mail de destino para o teste.");
        return systemEmailService.sendWithConfiguration(
                configuracao,
                EmailRequest.builder()
                        .type(EmailLogType.TESTE_SMTP)
                        .subject("Teste de SMTP - FisioGestao")
                        .to(email)
                        .templateName("email/teste-smtp")
                        .templateId("teste-smtp")
                        .triggeredBy(usuario)
                        .notes("Teste manual de configuração SMTP.")
                        .templateVariables(java.util.Map.of(
                                "titulo", "Teste de SMTP concluído",
                                "mensagem", "Esta mensagem confirma que a configuração SMTP informada conseguiu autenticar e enviar e-mails.",
                                "nomeRemetente", configuracao.getNomeRemetente(),
                                "emailRemetente", configuracao.getEmailRemetente()
                        ))
                        .build()
        );
    }

    public String revelarSenha(EmailConfiguracaoEntity configuracao) {
        return secretCryptoService.decrypt(configuracao.getSenhaAppCriptografada());
    }

    private void desativarDemaisConfiguracoes(UUID configuracaoAtivaId) {
        listarConfiguracoes().stream()
                .filter(item -> !item.getId().equals(configuracaoAtivaId))
                .filter(item -> Boolean.TRUE.equals(item.getAtivo()))
                .forEach(item -> {
                    var snapshotAnterior = auditService.snapshotOf(item);
                    item.setAtivo(false);
                    EmailConfiguracaoEntity salva = emailConfiguracaoRepository.save(item);
                    auditService.recordManual(
                            AuditModule.CONFIGURACOES,
                            "email_configuracao",
                            salva.getId().toString(),
                            auditService.describeOf(salva),
                            AuditActionType.INACTIVATE,
                            snapshotAnterior,
                            auditService.snapshotOf(salva),
                            null,
                            "Configuração SMTP desativada automaticamente para manter uma única configuração ativa.",
                            AuditEventOrigin.MANUAL
                    );
                });
    }

    private EmailConfiguracaoEntity novaConfiguracaoNoEscopo() {
        EmailConfiguracaoEntity entity = new EmailConfiguracaoEntity();
        UUID empresaId = TenantContext.empresaIdOuNull();
        entity.setEmpresaId(empresaId);
        entity.setScopeType(empresaId == null ? EmailScopeType.GLOBAL : EmailScopeType.CLINICA);
        entity.setScopeId(empresaId);
        return entity;
    }

    private String textoObrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new BusinessException(mensagem);
        }
        return valor.trim();
    }

    private String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String email(String valor, String mensagem) {
        String normalizado = emailOpcional(valor);
        if (normalizado == null) {
            throw new BusinessException(mensagem);
        }
        return normalizado;
    }

    private String emailOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normalizado = valor.trim().toLowerCase();
        if (!normalizado.contains("@") || normalizado.startsWith("@") || normalizado.endsWith("@")) {
            throw new BusinessException("Informe um e-mail válido.");
        }
        return normalizado;
    }
}
