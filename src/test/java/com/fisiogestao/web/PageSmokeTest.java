package com.fisiogestao.web;

import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventEntity;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.repository.AuditEventRepository;
import com.fisiogestao.auth.UsuarioSistemaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PageSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private UsuarioSistemaRepository usuarioSistemaRepository;

    private UUID auditEventId;
    private UUID adminUserId;

    @BeforeEach
    void setUp() {
        adminUserId = usuarioSistemaRepository.findByUsername("admin")
                .orElseThrow(() -> new IllegalStateException("Usuário admin não encontrado para os testes."))
                .getId();

        auditEventId = auditEventRepository.findAll().stream()
                .findFirst()
                .map(AuditEventEntity::getId)
                .orElseGet(() -> auditEventRepository.save(AuditEventEntity.builder()
                        .modulo(AuditModule.AUDITORIA)
                        .entidade("teste")
                        .entidadeId("page-smoke")
                        .descricaoRegistro("Evento de auditoria para teste de páginas")
                        .tipoAcao(AuditActionType.CREATE)
                        .origemEvento(AuditEventOrigin.MANUAL)
                        .usuarioNome("Administrador")
                        .perfilUsuario("ADMIN")
                        .sucessoOperacao(true)
                        .alteracaoCritica(false)
                        .build()).getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/login",
            "/esqueci-minha-senha",
            "/resetar-senha",
            "/agendamento-online",
            "/agendamento-online/sucesso"
    })
    void publicPagesRender(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @WithMockUser(username = "admin", roles = "ADMIN")
    @ValueSource(strings = {
            "/",
            "/dashboard",
            "/agenda",
            "/bloqueios",
            "/calendario",
            "/auditoria",
            "/configuracoes/email",
            "/configuracao/email",
            "/configuracoes/clinica",
            "/configuracao/clinica",
            "/financeiro",
            "/pacientes",
            "/pacotes-consumo",
            "/prontuario",
            "/relatorios",
            "/solicitacoes",
            "/whatsapp",
            "/usuarios",
            "/funcoes-usuarios"
    })
    void securedPagesRenderForAdmin(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @WithMockUser(username = "admin", roles = "ADMIN")
    @ValueSource(strings = {
            "/usuarios?editar=%s",
            "/auditoria/%s"
    })
    void detailPagesRenderForAdmin(String pathTemplate) throws Exception {
        String path = pathTemplate.contains("/usuarios")
                ? pathTemplate.formatted(adminUserId)
                : pathTemplate.formatted(auditEventId);

        mockMvc.perform(get(path))
                .andExpect(status().isOk());
    }
}
