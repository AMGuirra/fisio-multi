package com.fisiogestao.config;

import com.fisiogestao.auth.BootstrapAdminProperties;
import com.fisiogestao.auth.SuperAdminBootstrapProperties;
import com.fisiogestao.auth.UserRole;
import com.fisiogestao.auth.UsuarioSistemaEntity;
import com.fisiogestao.auth.UsuarioSistemaRepository;
import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.empresa.EmpresaLegada;
import com.fisiogestao.empresa.EmpresaProvisionamentoService;
import com.fisiogestao.empresa.EmpresaRepository;
import com.fisiogestao.empresa.EmpresaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Requisito 23: admin/admin local é criado como SUPER_ADMIN de forma idempotente e nunca em produção. */
class BootstrapSuperAdminTest {

    private UsuarioSistemaRepository usuarioRepository;
    private EmpresaRepository empresaRepository;
    private SuperAdminBootstrapProperties properties;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioSistemaRepository.class);
        empresaRepository = mock(EmpresaRepository.class);
        when(empresaRepository.findBySlug(EmpresaLegada.SLUG)).thenReturn(Optional.empty());
        when(empresaRepository.count()).thenReturn(1L); // já existem empresas: não cria a legada
        properties = new SuperAdminBootstrapProperties();
        properties.setEnabled(true);
        properties.setUsername("admin");
        properties.setPassword("admin");
    }

    private DataInitializer inicializador(Environment environment, String baseUrl) {
        DataInitializer initializer = new DataInitializer(
                empresaRepository,
                mock(EmpresaProvisionamentoService.class),
                usuarioRepository,
                encoder,
                properties,
                new BootstrapAdminProperties(),
                mock(PlatformTransactionManager.class),
                environment
        );
        ReflectionTestUtils.setField(initializer, "appBaseUrl", baseUrl);
        return initializer;
    }

    @Test
    void criaAdminAdminComoSuperAdminSemEmpresaEmAmbienteLocal() {
        when(usuarioRepository.existsByEmpresaIdIsNullAndUsername("admin")).thenReturn(false);

        inicializador(new MockEnvironment(), "http://localhost:8080").run();

        ArgumentCaptor<UsuarioSistemaEntity> captor = ArgumentCaptor.forClass(UsuarioSistemaEntity.class);
        verify(usuarioRepository).save(captor.capture());
        UsuarioSistemaEntity criado = captor.getValue();
        assertEquals("admin", criado.getUsername());
        assertEquals(UserRole.SUPER_ADMIN, criado.getRole());
        assertNull(criado.getEmpresaId());
        assertTrue(encoder.matches("admin", criado.getPasswordHash()));
    }

    @Test
    void ehIdempotenteENaoSobrescreveSenhaDeUsuarioExistente() {
        when(usuarioRepository.existsByEmpresaIdIsNullAndUsername("admin")).thenReturn(true);

        DataInitializer initializer = inicializador(new MockEnvironment(), "http://localhost:8080");
        initializer.run();
        initializer.run();

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void naoCriaAdminAdminEmProducao() {
        when(usuarioRepository.existsByEmpresaIdIsNullAndUsername("admin")).thenReturn(false);
        MockEnvironment producao = new MockEnvironment();
        producao.setActiveProfiles("prod");

        inicializador(producao, "https://app.fisiogestao.com.br").run();

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void desabilitadoNaoCriaNada() {
        properties.setEnabled(false);
        inicializador(new MockEnvironment(), "http://localhost:8080").run();
        verify(usuarioRepository, times(0)).save(any());
        verify(usuarioRepository, never()).existsByEmpresaIdIsNullAndUsername(any());
    }

    @SuppressWarnings("unused")
    private static EmpresaEntity empresa() {
        return EmpresaEntity.builder().nome("x").slug("x").status(EmpresaStatus.ATIVO).build();
    }
}
