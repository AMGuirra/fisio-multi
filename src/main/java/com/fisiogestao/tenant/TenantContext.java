package com.fisiogestao.tenant;

import com.fisiogestao.auth.UsuarioAutenticado;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Ponto central para descobrir a empresa (tenant) da operação corrente.
 *
 * <p>Ordem de resolução:</p>
 * <ol>
 *   <li>escopo explícito aberto por {@link #executarComo(UUID, Supplier)} — usado por rotas públicas
 *       (slug/token já validados no servidor), jobs agendados e pelo cadastro de novas clínicas;</li>
 *   <li>empresa do usuário autenticado ({@link UsuarioAutenticado#getEmpresaId()}), definida no login
 *       a partir do banco — nunca a partir de parâmetros enviados pelo navegador;</li>
 *   <li>nenhuma empresa: o Hibernate recebe {@link #NENHUMA_EMPRESA}, um identificador que não existe,
 *       e toda consulta a dados de clínica retorna vazio (falha fechada).</li>
 * </ol>
 *
 * <p>O escopo explícito é sempre restaurado ao valor anterior no {@code finally}, então não vaza entre
 * requisições que reaproveitam a mesma thread.</p>
 */
public final class TenantContext {

    /** Tenant inexistente usado quando não há empresa no contexto: nenhuma linha possui esse valor. */
    public static final UUID NENHUMA_EMPRESA = new UUID(0L, 0L);

    private static final ThreadLocal<Escopo> ESCOPO = new ThreadLocal<>();

    private TenantContext() {
    }

    public static Optional<UUID> empresaIdAtual() {
        Escopo escopo = ESCOPO.get();
        if (escopo != null) {
            return Optional.ofNullable(escopo.empresaId());
        }
        return usuarioAutenticado().map(UsuarioAutenticado::getEmpresaId);
    }

    /** Empresa atual ou {@code null} (superadmin, visitante anônimo ou rotina de plataforma). */
    public static UUID empresaIdOuNull() {
        return empresaIdAtual().orElse(null);
    }

    public static UUID requireEmpresaId() {
        return empresaIdAtual().orElseThrow(TenantAusenteException::new);
    }

    public static boolean possuiEmpresa() {
        return empresaIdAtual().isPresent();
    }

    public static Optional<UsuarioAutenticado> usuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return Optional.of(usuario);
        }
        return Optional.empty();
    }

    public static <T> T executarComo(UUID empresaId, Supplier<T> acao) {
        Escopo anterior = ESCOPO.get();
        ESCOPO.set(new Escopo(empresaId));
        try {
            return acao.get();
        } finally {
            restaurar(anterior);
        }
    }

    public static void executarComo(UUID empresaId, Runnable acao) {
        executarComo(empresaId, () -> {
            acao.run();
            return null;
        });
    }

    /** Executa explicitamente fora de qualquer empresa (escopo da plataforma/Gestor). */
    public static <T> T executarSemEmpresa(Supplier<T> acao) {
        return executarComo((UUID) null, acao);
    }

    public static void executarSemEmpresa(Runnable acao) {
        executarComo((UUID) null, acao);
    }

    /**
     * Abre um escopo que precisa ser fechado manualmente — usado por filtros servlet,
     * que precisam envolver toda a cadeia restante da requisição.
     */
    public static Escopo abrir(UUID empresaId) {
        Escopo anterior = ESCOPO.get();
        ESCOPO.set(new Escopo(empresaId));
        return anterior == null ? Escopo.VAZIO : anterior;
    }

    public static void fechar(Escopo anterior) {
        restaurar(anterior == Escopo.VAZIO ? null : anterior);
    }

    private static void restaurar(Escopo anterior) {
        if (anterior == null) {
            ESCOPO.remove();
        } else {
            ESCOPO.set(anterior);
        }
    }

    public record Escopo(UUID empresaId) {
        static final Escopo VAZIO = new Escopo(NENHUMA_EMPRESA);
    }
}
