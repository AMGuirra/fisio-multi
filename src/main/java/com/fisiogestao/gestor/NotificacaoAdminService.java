package com.fisiogestao.gestor;

import com.fisiogestao.email.EmailLogType;
import com.fisiogestao.email.service.EmailDispatchResult;
import com.fisiogestao.email.service.EmailRequest;
import com.fisiogestao.email.service.SystemEmailService;
import com.fisiogestao.empresa.ConfiguracaoPlataformaService;
import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Alertas da plataforma: grava o aviso no painel do Gestor e tenta enviar e-mail ao endereço
 * administrativo global configurado em "Configurações globais". Falhas de envio são registradas
 * (log da aplicação + log de e-mail) e nunca interrompem cadastro, suporte ou ações administrativas.
 */
@Service
@RequiredArgsConstructor
public class NotificacaoAdminService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificacaoAdminService.class);

    private final NotificacaoGestorRepository notificacaoRepository;
    private final ConfiguracaoPlataformaService configuracaoPlataformaService;
    private final SystemEmailService systemEmailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public NotificacaoGestorEntity registrarAlerta(
            TipoNotificacaoGestor tipo,
            String titulo,
            String mensagem,
            UUID empresaId,
            UUID referenciaId,
            String link
    ) {
        return TenantContext.executarSemEmpresa(() -> notificacaoRepository.save(NotificacaoGestorEntity.builder()
                .tipo(tipo)
                .titulo(limitar(titulo, 200))
                .mensagem(mensagem)
                .empresaId(empresaId)
                .referenciaId(referenciaId)
                .link(link)
                .build()));
    }

    /** Alerta no Gestor + e-mail administrativo (best effort). */
    public void notificar(
            TipoNotificacaoGestor tipo,
            String titulo,
            String mensagem,
            UUID empresaId,
            UUID referenciaId,
            String link,
            Map<String, String> detalhes
    ) {
        try {
            registrarAlerta(tipo, titulo, mensagem, empresaId, referenciaId, link);
        } catch (RuntimeException ex) {
            LOGGER.error("Falha ao registrar alerta do Gestor ({}): {}", tipo, ex.getMessage(), ex);
        }
        enviarEmailAdministrativo(titulo, mensagem, link, detalhes);
    }

    /** Envia ao e-mail administrativo global; retorna {@code false} se não configurado ou se falhar. */
    public boolean enviarEmailAdministrativo(String titulo, String mensagem, String link, Map<String, String> detalhes) {
        try {
            String destino = TenantContext.executarSemEmpresa(configuracaoPlataformaService::emailNotificacoes);
            if (destino == null || destino.isBlank()) {
                LOGGER.warn("E-mail administrativo global não configurado; notificação '{}' não enviada.", titulo);
                return false;
            }
            Map<String, Object> variaveis = new LinkedHashMap<>();
            variaveis.put("titulo", titulo);
            variaveis.put("mensagem", mensagem == null ? "" : mensagem);
            variaveis.put("detalhes", detalhes == null ? Map.of() : detalhes);
            if (link != null) {
                variaveis.put("link", urlAbsoluta(link));
            }
            variaveis.put("nomeClinica", "FisioGestao — Gestor");
            EmailDispatchResult resultado = systemEmailService.sendPlataforma(EmailRequest.builder()
                    .type(EmailLogType.NOTIFICACAO_ADMINISTRATIVA)
                    .subject("[FisioGestao] " + titulo)
                    .to(destino)
                    .templateName("email/notificacao-administrativa")
                    .templateId("notificacao-administrativa")
                    .triggeredBy("sistema")
                    .entityType("plataforma")
                    .templateVariables(variaveis)
                    .build());
            if (!resultado.sent()) {
                LOGGER.warn("Notificação administrativa '{}' não enviada: {}", titulo, resultado.message());
            }
            return resultado.sent();
        } catch (RuntimeException ex) {
            LOGGER.error("Falha ao enviar notificação administrativa '{}': {}", titulo, ex.getMessage(), ex);
            return false;
        }
    }

    /** Comunicado à clínica (status, cancelamento, reativação) usando o e-mail cadastrado da empresa. */
    public boolean enviarEmailClinica(EmpresaEntity empresa, String titulo, String mensagem, boolean incluirLinkLogin) {
        if (empresa == null || empresa.getEmail() == null || empresa.getEmail().isBlank()) {
            return false;
        }
        try {
            Map<String, Object> variaveis = new LinkedHashMap<>();
            variaveis.put("titulo", titulo);
            variaveis.put("mensagem", mensagem == null ? "" : mensagem);
            if (empresa.getNomeResponsavel() != null) {
                variaveis.put("nomeResponsavel", empresa.getNomeResponsavel());
            }
            variaveis.put("nomeClinica", empresa.getNome());
            if (incluirLinkLogin) {
                variaveis.put("link", urlAbsoluta("/login?clinica=" + empresa.getSlug()));
            }
            variaveis.put("textoLink", "Acessar o sistema");
            EmailDispatchResult resultado = systemEmailService.sendPlataforma(EmailRequest.builder()
                    .type(EmailLogType.NOTIFICACAO_CLINICA)
                    .subject(titulo + " — " + empresa.getNome())
                    .to(empresa.getEmail())
                    .templateName("email/notificacao-clinica")
                    .templateId("notificacao-clinica")
                    .triggeredBy("sistema")
                    .entityType("empresa")
                    .entityId(empresa.getId())
                    .templateVariables(variaveis)
                    .build());
            return resultado.sent();
        } catch (RuntimeException ex) {
            LOGGER.error("Falha ao enviar comunicado à clínica {}: {}", empresa.getId(), ex.getMessage(), ex);
            return false;
        }
    }

    private String urlAbsoluta(String caminho) {
        String base = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        return caminho.startsWith("http") ? caminho : base + caminho;
    }

    private static String limitar(String texto, int max) {
        return texto == null || texto.length() <= max ? texto : texto.substring(0, max);
    }
}
