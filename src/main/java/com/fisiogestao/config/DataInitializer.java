package com.fisiogestao.config;

import com.fisiogestao.auth.BootstrapAdminProperties;
import com.fisiogestao.auth.PasswordPolicy;
import com.fisiogestao.auth.SuperAdminBootstrapProperties;
import com.fisiogestao.auth.UserRole;
import com.fisiogestao.auth.UsuarioSistemaEntity;
import com.fisiogestao.auth.UsuarioSistemaRepository;
import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.empresa.EmpresaLegada;
import com.fisiogestao.empresa.EmpresaProvisionamentoService;
import com.fisiogestao.empresa.EmpresaRepository;
import com.fisiogestao.empresa.EmpresaStatus;
import com.fisiogestao.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;
import java.util.Optional;

/**
 * Inicialização idempotente da plataforma:
 * <ol>
 *   <li>garante a "Clínica Principal" (empresa legada) quando o banco não tem nenhuma empresa —
 *       em bancos migrados pelo Flyway ela já foi criada pela V54;</li>
 *   <li>garante a configuração operacional da clínica legada;</li>
 *   <li>cria o SUPER_ADMIN local ({@code admin/admin}) apenas quando habilitado e fora de produção,
 *       sem nunca sobrescrever a senha de um usuário existente;</li>
 *   <li>mantém o bootstrap opcional do administrador da clínica legada via variáveis de ambiente.</li>
 * </ol>
 */
@Component
@Order(10)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataInitializer.class);

    private final EmpresaRepository empresaRepository;
    private final EmpresaProvisionamentoService provisionamentoService;
    private final UsuarioSistemaRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SuperAdminBootstrapProperties superAdminProperties;
    private final BootstrapAdminProperties bootstrapAdminProperties;
    private final PlatformTransactionManager transactionManager;
    private final Environment environment;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Override
    public void run(String... args) {
        EmpresaEntity legada = garantirEmpresaLegada();
        if (legada != null) {
            TenantContext.executarComo(legada.getId(), () ->
                    new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                            provisionamentoService.criarConfiguracaoInicial(legada)));
            criarAdministradorLegadoSeConfigurado(legada);
        }
        criarSuperAdminLocal();
    }

    private EmpresaEntity garantirEmpresaLegada() {
        Optional<EmpresaEntity> existente = empresaRepository.findBySlug(EmpresaLegada.SLUG);
        if (existente.isPresent()) {
            return existente.get();
        }
        if (empresaRepository.count() > 0) {
            return null;
        }
        EmpresaEntity criada = empresaRepository.save(EmpresaEntity.builder()
                .nome(EmpresaLegada.NOME)
                .slug(EmpresaLegada.SLUG)
                .status(EmpresaStatus.ATIVO)
                .build());
        LOGGER.info("Empresa legada '{}' criada (banco sem empresas).", EmpresaLegada.NOME);
        return criada;
    }

    private void criarSuperAdminLocal() {
        if (!superAdminProperties.isEnabled()) {
            return;
        }
        String username = normalizar(superAdminProperties.getUsername());
        if (username == null) {
            LOGGER.warn("Bootstrap do SUPER_ADMIN habilitado sem username; nada foi criado.");
            return;
        }
        if (usuarioRepository.existsByEmpresaIdIsNullAndUsername(username)) {
            return; // idempotente: nunca altera senha nem dados de um superadmin existente
        }
        String senha = superAdminProperties.getPassword();
        boolean senhaPadraoLocal = SuperAdminBootstrapProperties.SENHA_LOCAL_PADRAO.equals(senha);
        if (senhaPadraoLocal && isRuntimeProducao()) {
            LOGGER.error("SUPER_ADMIN com senha padrão 'admin' recusado em produção. Defina APP_BOOTSTRAP_SUPER_ADMIN_PASSWORD.");
            return;
        }
        if (senha == null || senha.isBlank()) {
            LOGGER.warn("Bootstrap do SUPER_ADMIN habilitado sem senha; nada foi criado.");
            return;
        }
        if (!senhaPadraoLocal) {
            senha = PasswordPolicy.normalize(senha, true);
        }
        usuarioRepository.save(UsuarioSistemaEntity.builder()
                .nome(superAdminProperties.getNome() == null ? "Superadministrador" : superAdminProperties.getNome().trim())
                .username(username)
                .email(normalizar(superAdminProperties.getEmail()))
                .passwordHash(passwordEncoder.encode(senha))
                .role(UserRole.SUPER_ADMIN)
                .empresaId(null)
                .build());
        if (senhaPadraoLocal) {
            LOGGER.warn("SUPER_ADMIN local criado: usuário '{}' / senha 'admin'. USO EXCLUSIVO EM AMBIENTE LOCAL.", username);
        } else {
            LOGGER.info("SUPER_ADMIN '{}' criado via bootstrap.", username);
        }
    }

    private void criarAdministradorLegadoSeConfigurado(EmpresaEntity legada) {
        if (!bootstrapAdminProperties.isEnabled() || usuarioRepository.countByEmpresaId(legada.getId()) > 0) {
            return;
        }
        String username = normalizar(bootstrapAdminProperties.getUsername());
        String email = normalizar(bootstrapAdminProperties.getEmail());
        if (username == null || email == null) {
            LOGGER.warn("APP_BOOTSTRAP_ADMIN_ENABLED=true exige APP_BOOTSTRAP_ADMIN_USERNAME, _EMAIL e _PASSWORD.");
            return;
        }
        usuarioRepository.save(UsuarioSistemaEntity.builder()
                .nome(bootstrapAdminProperties.getNome() == null ? "Administrador" : bootstrapAdminProperties.getNome().trim())
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(PasswordPolicy.normalize(bootstrapAdminProperties.getPassword(), true)))
                .role(UserRole.ADMIN_CLINICA)
                .empresaId(legada.getId())
                .build());
        LOGGER.info("Administrador inicial da clínica legada criado via bootstrap seguro.");
    }

    private boolean isRuntimeProducao() {
        if (environment.matchesProfiles("prod", "production") || "true".equalsIgnoreCase(environment.getProperty("RENDER"))) {
            return true;
        }
        return appBaseUrl != null && !appBaseUrl.contains("localhost") && !appBaseUrl.contains("127.0.0.1");
    }

    private static String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim().toLowerCase(Locale.ROOT);
    }
}
