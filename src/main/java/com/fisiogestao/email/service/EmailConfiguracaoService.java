package com.fisiogestao.email.service;

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

    public Optional<EmailConfiguracaoEntity> buscarAtiva() {
        return emailConfiguracaoRepository.findFirstByAtivoTrueOrderByUpdatedAtDesc();
    }

    public List<EmailConfiguracaoEntity> listarConfiguracoes() {
        return emailConfiguracaoRepository.findAllByOrderByAtivoDescUpdatedAtDesc();
    }

    public EmailConfiguracaoEntity buscarPorId(UUID id) {
        return emailConfiguracaoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Configuração de e-mail não encontrada."));
    }

    public List<com.fisiogestao.email.EmailLogEntity> listarHistorico() {
        return emailLogRepository.findTop50ByOrderByCreatedAtDesc();
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
        EmailConfiguracaoEntity entity = id == null
                ? new EmailConfiguracaoEntity()
                : buscarPorId(id);

        entity.setNomeRemetente(textoObrigatorio(nomeRemetente, "Informe o nome do remetente."));
        entity.setEmailRemetente(email(emailRemetente, "Informe o e-mail do remetente."));
        entity.setSmtpHost(textoObrigatorio(smtpHost, "Informe o host SMTP."));
        entity.setSmtpPort(validarPorta(smtpPort));
        entity.setUsaAutenticacao(usaAutenticacao);
        entity.setUsaTls(usaTls);
        entity.setUsaSsl(usaSsl);
        entity.setReplyTo(emailOpcional(replyTo));
        entity.setAtivo(ativo);
        entity.setObservacoes(textoOpcional(observacoes));

        if (manterSenhaAtual) {
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

        return emailConfiguracaoRepository.save(entity);
    }

    @Transactional
    public void definirAtiva(UUID id) {
        desativarDemaisConfiguracoes(id);
        EmailConfiguracaoEntity entity = buscarPorId(id);
        entity.setAtivo(true);
        emailConfiguracaoRepository.save(entity);
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
        emailConfiguracaoRepository.findAllByOrderByAtivoDescUpdatedAtDesc().stream()
                .filter(item -> !item.getId().equals(configuracaoAtivaId))
                .filter(item -> Boolean.TRUE.equals(item.getAtivo()))
                .forEach(item -> item.setAtivo(false));
    }

    private Integer validarPorta(Integer porta) {
        if (porta == null || porta < 1 || porta > 65535) {
            throw new BusinessException("Informe uma porta SMTP válida.");
        }
        return porta;
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
