package com.fisiogestao.tenant;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventEntity;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.form.AuditFilterForm;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.auth.UserRole;
import com.fisiogestao.auth.UsuarioSistemaEntity;
import com.fisiogestao.dashboard.DashboardMetricasService;
import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.empresa.EmpresaStatus;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.PlanoSessaoEntity;
import com.fisiogestao.pacientes.PlanoSessaoStatus;
import com.fisiogestao.pacientes.repository.PlanoSessaoRepository;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.support.MultiEmpresaTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Requisitos 1 a 4: uma clínica nunca vê, acessa ou relaciona dados de outra. */
class IsolamentoMultiEmpresaTest extends MultiEmpresaTestBase {

    @Autowired private PacienteService pacienteService;
    @Autowired private PlanoSessaoRepository planoSessaoRepository;
    @Autowired private SessaoRepository sessaoRepository;
    @Autowired private AuditService auditService;
    @Autowired private DashboardMetricasService dashboardMetricasService;
    @Autowired private com.fisiogestao.auditoria.repository.AuditEventRepository auditEventRepository;

    private EmpresaEntity clinicaA;
    private EmpresaEntity clinicaB;
    private UsuarioSistemaEntity adminA;
    private UsuarioSistemaEntity adminB;
    private ClienteEntity pacienteA;
    private ClienteEntity pacienteB;
    private PlanoSessaoEntity planoB;

    @BeforeEach
    void preparar() {
        clinicaA = criarEmpresa("clinica-a", EmpresaStatus.ATIVO);
        clinicaB = criarEmpresa("clinica-b", EmpresaStatus.ATIVO);
        // Mesmo username nas duas clínicas: permitido e isolado.
        adminA = criarUsuario(clinicaA, "admin-iso", UserRole.ADMIN_CLINICA);
        adminB = criarUsuario(clinicaB, "admin-iso", UserRole.ADMIN_CLINICA);
        pacienteA = criarPaciente(clinicaA, "Ana Paciente Alfa " + UUID.randomUUID());
        pacienteB = criarPaciente(clinicaB, "Bruno Paciente Beta " + UUID.randomUUID());
        planoB = TenantContext.executarComo(clinicaB.getId(), () -> planoSessaoRepository.save(PlanoSessaoEntity.builder()
                .cliente(pacienteB)
                .nomePlano("Plano Beta")
                .sessoesContratadas(10)
                .sessoesConsumidas(0)
                .valorTotal(new BigDecimal("500.00"))
                .dataInicio(LocalDate.now())
                .status(PlanoSessaoStatus.ATIVO)
                .build()));
        TenantContext.executarComo(clinicaB.getId(), () -> auditService.recordManual(AuditModule.PACIENTES, "cliente",
                pacienteB.getId().toString(), "Registro secreto da clínica B", AuditActionType.CREATE, null, null,
                null, "marcador-auditoria-clinica-b", AuditEventOrigin.MANUAL));
    }

    @Test
    void usuarioDaClinicaANaoVisualizaDadosDaClinicaB() throws Exception {
        mockMvc.perform(get("/pacientes").with(comoUsuario(adminA)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(pacienteA.getNomeCompleto())))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(pacienteB.getNomeCompleto()))));

        List<ClienteEntity> visiveisParaA = TenantContext.executarComo(clinicaA.getId(), () -> clienteRepository.findAll());
        assertTrue(visiveisParaA.stream().anyMatch(c -> c.getId().equals(pacienteA.getId())));
        assertFalse(visiveisParaA.stream().anyMatch(c -> c.getId().equals(pacienteB.getId())));
    }

    @Test
    void usuarioDaClinicaANaoAcessaRegistroDaClinicaBPorIdDireto() throws Exception {
        // Repositório: findById também é filtrado pela empresa.
        assertTrue(TenantContext.executarComo(clinicaA.getId(), () -> clienteRepository.findById(pacienteB.getId())).isEmpty());
        assertTrue(TenantContext.executarComo(clinicaA.getId(), () -> planoSessaoRepository.findById(planoB.getId())).isEmpty());

        // Serviço
        autenticarComo(adminA);
        assertThrows(BusinessException.class, () -> pacienteService.buscarPorId(pacienteB.getId()));

        // URL / API
        mockMvc.perform(get("/prontuario").param("paciente", pacienteB.getId().toString()).with(comoUsuario(adminA)))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(pacienteB.getNomeCompleto()))));
        mockMvc.perform(get("/api/pacientes/por-cpf").param("cpf", "52998224725").with(comoUsuario(adminA)))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(pacienteB.getNomeCompleto()))));
    }

    @Test
    void usuarioDaClinicaANaoCriaRelacionamentoComDadosDaClinicaB() throws Exception {
        long sessoesBAntes = TenantContext.executarComo(clinicaB.getId(), () -> sessaoRepository.count());

        mockMvc.perform(post("/planos-consumo/agendar-sessao")
                .with(csrf())
                .with(comoUsuario(adminA))
                .param("clienteId", pacienteB.getId().toString())
                .param("planoSessaoId", planoB.getId().toString())
                .param("data", LocalDate.now().plusDays(3).toString())
                .param("horario", "10:00"));

        long sessoesBDepois = TenantContext.executarComo(clinicaB.getId(), () -> sessaoRepository.count());
        long sessoesA = TenantContext.executarComo(clinicaA.getId(), () -> sessaoRepository.count());
        assertEquals(sessoesBAntes, sessoesBDepois, "Nenhuma sessão pode ser criada na clínica B por um usuário da clínica A.");
        assertEquals(0, sessoesA, "Nenhuma sessão vinculada a paciente de outra clínica pode existir na clínica A.");

        // Persistência: o Hibernate rejeita gravar um registro com empresa diferente da do contexto.
        assertThrows(RuntimeException.class, () -> TenantContext.executarComo(clinicaA.getId(), () -> {
            SessaoEntity sessao = TenantContext.executarComo(clinicaB.getId(), () -> sessaoRepository.findAll()).stream().findFirst().orElse(null);
            if (sessao == null) {
                throw new IllegalStateException("sem sessão para validar");
            }
            sessao.setObservacoes("alteração cruzada");
            return sessaoRepository.save(sessao);
        }));
    }

    @Test
    void dashboardsBuscasEAuditoriaSaoFiltradosPorEmpresa() throws Exception {
        autenticarComo(adminA);
        var metricas = dashboardMetricasService.carregarMetricas();
        assertEquals(1, metricas.retencaoPlanos().totalPacientesAtivos(), "Dashboard da clínica A conta apenas os pacientes de A.");

        List<AuditEventEntity> auditoriaA = auditService.list(new AuditFilterForm());
        assertTrue(auditoriaA.stream().noneMatch(e -> "marcador-auditoria-clinica-b".equals(e.getObservacao())));
        assertTrue(auditoriaA.stream().allMatch(e -> clinicaA.getId().equals(e.getEmpresaId())));

        UUID eventoB = TenantContext.executarComo(clinicaB.getId(), () -> auditService.list(new AuditFilterForm())).stream()
                .filter(e -> "marcador-auditoria-clinica-b".equals(e.getObservacao()))
                .findFirst().orElseThrow().getId();
        assertThrows(BusinessException.class, () -> auditService.detail(eventoB));

        mockMvc.perform(get("/auditoria").with(comoUsuario(adminA)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Registro secreto da clínica B"))));

        mockMvc.perform(get("/pacientes").param("busca", "Bruno").with(comoUsuario(adminA)))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(pacienteB.getNomeCompleto()))));

        // Exportação roda no escopo da clínica A e é auditada somente nela.
        mockMvc.perform(get("/relatorios/exportar").with(comoUsuario(adminA))
                        .param("formato", "EXCEL")
                        .param("dataInicial", LocalDate.now().minusMonths(1).toString())
                        .param("dataFinal", LocalDate.now().toString()))
                .andExpect(status().isOk());
        assertTrue(TenantContext.executarComo(clinicaA.getId(), () -> auditService.list(new AuditFilterForm())).stream()
                .anyMatch(e -> e.getModulo() == AuditModule.RELATORIOS));
        assertTrue(TenantContext.executarComo(clinicaB.getId(), () -> auditService.list(new AuditFilterForm())).stream()
                .noneMatch(e -> e.getModulo() == AuditModule.RELATORIOS));
    }

    @Test
    void usernameRepetidoEmClinicasDiferentesLoginResolveAEmpresaPeloCodigo() throws Exception {
        var loginA = login("admin-iso", SENHA, clinicaA.getSlug());
        assertEquals("/dashboard", loginA.getResponse().getRedirectedUrl());
        mockMvc.perform(get("/pacientes").session(sessao(loginA)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(pacienteA.getNomeCompleto())))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(pacienteB.getNomeCompleto()))));

        // Sem código e com a mesma senha nas duas clínicas: ambíguo, recusa com mensagem genérica.
        var ambiguo = login("admin-iso", SENHA, null);
        assertTrue(ambiguo.getResponse().getRedirectedUrl().startsWith("/login?error"));

        // Parâmetro "clinica" enviado depois do login não troca a empresa da sessão.
        mockMvc.perform(get("/pacientes").param("clinica", clinicaB.getSlug()).session(sessao(loginA)))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(pacienteB.getNomeCompleto()))));
    }
}
