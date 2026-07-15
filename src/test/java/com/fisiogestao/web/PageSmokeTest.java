package com.fisiogestao.web;

import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.TipoAgendamento;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventEntity;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auth.UserRole;
import com.fisiogestao.auth.UsuarioSistemaEntity;
import com.fisiogestao.auditoria.repository.AuditEventRepository;
import com.fisiogestao.auth.UserThemePreference;
import com.fisiogestao.auth.UsuarioSistemaRepository;
import com.fisiogestao.financeiro.LancamentoFinanceiroEntity;
import com.fisiogestao.financeiro.StatusLancamentoFinanceiro;
import com.fisiogestao.financeiro.TipoLancamentoFinanceiro;
import com.fisiogestao.financeiro.repository.LancamentoFinanceiroRepository;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.PlanoSessaoEntity;
import com.fisiogestao.pacientes.PlanoSessaoStatus;
import com.fisiogestao.pacientes.repository.ClienteRepository;
import com.fisiogestao.pacientes.repository.PlanoSessaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.RequestDispatcher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
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

    @Autowired
    private ConfiguracaoClinicaRepository configuracaoClinicaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PlanoSessaoRepository planoSessaoRepository;

    @Autowired
    private SessaoRepository sessaoRepository;

    @Autowired
    private LancamentoFinanceiroRepository lancamentoFinanceiroRepository;

    private UUID auditEventId;
    private UUID adminUserId;
    private LocalDate agendaDate;
    private UUID sessaoAgendaId;
    private ClienteEntity clienteAgenda;
    private static final String RECEPCAO_USERNAME = "recepcao-smoke";

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

        usuarioSistemaRepository.findByUsername(RECEPCAO_USERNAME)
                .orElseGet(() -> usuarioSistemaRepository.save(UsuarioSistemaEntity.builder()
                        .nome("Recepção Smoke")
                        .username(RECEPCAO_USERNAME)
                        .email("recepcao.smoke@teste.local")
                        .passwordHash("mock-password-hash")
                        .role(UserRole.RECEPCAO)
                        .build()));

        prepararCenarioAgenda();
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
    @ValueSource(strings = {"/", "/home"})
    void homeRedirecionaParaLogin(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void loginRenderizaComTokenCsrf() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    void errorPageRenderizaHtmlSemSerializarMap() throws Exception {
        mockMvc.perform(get("/error")
                        .accept(TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/rota-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Pagina nao encontrada")));
    }

    @Test
    void errorPageRespondeJsonQuandoClientePedeJson() throws Exception {
        mockMvc.perform(get("/error")
                        .accept(APPLICATION_JSON)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/teste"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"status\":500")));
    }

    @Test
    void patientLookupApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/pacientes/por-cpf").param("cpf", "12345678901"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void swaggerUiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = RECEPCAO_USERNAME, roles = "RECEPCAO")
    void swaggerUiRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void apiPostRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired"));
    }

    @ParameterizedTest
    @WithMockUser(username = "admin", roles = "ADMIN")
    @ValueSource(strings = {
            "/dashboard",
            "/calendario",
            "/auditoria",
            "/auditoria/vinculos",
            "/admin/system-status",
            "/configuracoes/email",
            "/configuracao/email",
            "/configuracoes/clinica",
            "/configuracao/clinica",
            "/bloqueios",
            "/financeiro",
            "/financeiro/contas-a-receber",
            "/financeiro/contas-a-pagar",
            "/financeiro/relatorio-faturamento",
            "/pacientes",
            "/planos-consumo",
            "/prontuario",
            "/relatorios",
            "/solicitacoes",
            "/usuarios",
            "/funcoes-usuarios"
    })
    void securedPagesRenderForAdmin(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void exclusaoPacienteComPlanoApagaTudoEmCascata() throws Exception {
        UUID clienteId = clienteAgenda.getId();

        mockMvc.perform(post("/pacientes/{id}/excluir", clienteId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pacientes"))
                .andExpect(flash().attributeExists("successMessage"));

        org.junit.jupiter.api.Assertions.assertTrue(clienteRepository.findById(clienteId).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(planoSessaoRepository.findByClienteIdOrderByCreatedAtDesc(clienteId).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(sessaoRepository.findByClienteIdOrderByDataHoraInicioDesc(clienteId).isEmpty());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void apiCriarPacienteRetornaIdENomeParaSelecaoImediata() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"nomeCompleto\":\"Paciente Via Api\",\"telefone\":\"65988887777\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.nomeCompleto").value("Paciente Via Api"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id").exists());

        org.junit.jupiter.api.Assertions.assertTrue(
                clienteRepository.findAll().stream().anyMatch(c -> "Paciente Via Api".equals(c.getNomeCompleto())));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void apiCriarPacienteComNomeVazioRetorna400() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"nomeCompleto\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void apiCriarPacienteSemCsrfEhBloqueado() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .contentType(APPLICATION_JSON)
                        .content("{\"nomeCompleto\":\"Paciente Sem Csrf\"}"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired"));

        org.junit.jupiter.api.Assertions.assertTrue(
                clienteRepository.findAll().stream().noneMatch(c -> "Paciente Sem Csrf".equals(c.getNomeCompleto())));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void paginaDeBloqueiosRenderizaSeparada() throws Exception {
        mockMvc.perform(get("/bloqueios"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @WithMockUser(username = RECEPCAO_USERNAME, roles = "RECEPCAO")
    @ValueSource(strings = {
            "/calendario",
            "/bloqueios",
            "/solicitacoes",
            "/pacientes",
            "/planos-consumo"
    })
    void recepcaoPodeAcessarMenusPadraoEsperados(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = RECEPCAO_USERNAME, roles = "RECEPCAO")
    void recepcaoNaoPodeAcessarRelatorios() throws Exception {
        mockMvc.perform(get("/relatorios"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?acessoNegado=1"));
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

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void agendaRenderizaComPlanoESessaoMesmoComOpenInViewDesabilitado() throws Exception {
        mockMvc.perform(get("/calendario")
                        .param("data", agendaDate.toString())
                        .param("mes", agendaDate.withDayOfMonth(1).toString().substring(0, 7)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Paciente Teste Agenda")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Plano Teste Agenda")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"editarSessaoModal\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Salvar alterações")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Confirmar realização")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void agendaExibeEditarECancelarParaSessaoRealizada() throws Exception {
        SessaoEntity sessao = sessaoRepository.findById(sessaoAgendaId)
                .orElseThrow(() -> new IllegalStateException("Sessão da agenda não encontrada."));
        sessao.setStatus(SessaoStatus.REALIZADA);
        sessaoRepository.save(sessao);

        mockMvc.perform(get("/calendario")
                        .param("data", agendaDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("REALIZADA")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Editar")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cancelar")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Excluir")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Concluir"))));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void agendaExibeVoltarParaPendenteEExcluirParaSessaoCancelada() throws Exception {
        SessaoEntity sessao = sessaoRepository.findById(sessaoAgendaId)
                .orElseThrow(() -> new IllegalStateException("Sessão da agenda não encontrada."));
        sessao.setStatus(SessaoStatus.CANCELADA);
        sessaoRepository.save(sessao);

        mockMvc.perform(get("/calendario")
                        .param("data", agendaDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CANCELADA")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Voltar para pendente")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Excluir")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void agendaRedirecionaParaCalendarioUnificado() throws Exception {
        mockMvc.perform(get("/agenda")
                        .param("data", agendaDate.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendario?data=" + agendaDate));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void calendarioRenderizaComDomingoPrimeiroEAgendaDoDiaNaMesmaTela() throws Exception {
        mockMvc.perform(get("/calendario"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<span>Dom</span><span>Seg</span>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sessões do Dia")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"editarSessaoModal\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("agendaDiaIframe"))));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void calendarioRenderizaAcoesDeSessaoComTokenCsrf() throws Exception {
        mockMvc.perform(get("/calendario").param("data", agendaDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"sessao-falta-" + sessaoAgendaId + "\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    @WithMockUser(username = RECEPCAO_USERNAME, roles = "RECEPCAO")
    void recepcaoPodeRegistrarFaltaSemCobranca() throws Exception {
        mockMvc.perform(post("/agenda/sessoes/{id}/falta", sessaoAgendaId)
                        .with(csrf())
                        .param("data", agendaDate.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendario?data=" + agendaDate));

        SessaoEntity sessao = sessaoRepository.findById(sessaoAgendaId)
                .orElseThrow(() -> new IllegalStateException("SessÃ£o de teste nÃ£o encontrada."));

        org.junit.jupiter.api.Assertions.assertEquals(SessaoStatus.FALTA, sessao.getStatus());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void planosConsumoAbreModalDeAgendamentoAoSelecionarPaciente() throws Exception {
        mockMvc.perform(get("/planos-consumo")
                        .param("paciente", clienteAgenda.getId().toString())
                        .param("agendar", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"openAgendamentoSessaoModal\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"openAjusteSessoesModal\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"agendamentoSessaoModal\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-hidden=\"false\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Paciente em atendimento")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"clienteId\" value=\"" + clienteAgenda.getId())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"planoSessaoId\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"data\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"horario\" value=\"08:00\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Buscar Paciente"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Plano Teste Agenda")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void planosConsumoAgendaSessaoEPermaneceNaMesmaTela() throws Exception {
        PlanoSessaoEntity planoAntes = planoSessaoRepository.findAll().stream()
                .filter(item -> item.getCliente().getId().equals(clienteAgenda.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Plano de teste não encontrado para o agendamento via planos."));

        mockMvc.perform(post("/planos-consumo/agendar-sessao")
                        .with(csrf())
                        .param("clienteId", clienteAgenda.getId().toString())
                        .param("planoSessaoId", planoAntes.getId().toString())
                        .param("data", agendaDate.toString())
                        .param("horario", "10:00")
                        .param("tipoAtendimento", "RPG")
                        .param("observacoes", "Agendamento criado pela tela de planos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planos-consumo?paciente=" + clienteAgenda.getId()));

        PlanoSessaoEntity planoDepois = planoSessaoRepository.findById(planoAntes.getId())
                .orElseThrow(() -> new IllegalStateException("Plano de teste não encontrado após o agendamento."));

        // Consumo adiado: não incrementa no agendamento — apenas na realização da sessão
        org.junit.jupiter.api.Assertions.assertEquals(
                planoAntes.getSessoesConsumidas(),
                planoDepois.getSessoesConsumidas(),
                "Consumo do plano não deve ser registrado no agendamento — apenas na realização da sessão"
        );

        SessaoEntity sessaoAgendada = sessaoRepository
                .findByClienteIdOrderByDataHoraInicioDesc(clienteAgenda.getId()).stream()
                .filter(s -> s.getDataHoraInicio().toLocalDate().equals(agendaDate))
                .filter(s -> s.getDataHoraInicio().toLocalTime().equals(LocalTime.of(10, 0)))
                .filter(s -> s.getStatus() == SessaoStatus.AGENDADA)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Sessão agendada não encontrada."));

        org.junit.jupiter.api.Assertions.assertEquals(1L,
                sessaoRepository.findByClienteIdOrderByDataHoraInicioDesc(clienteAgenda.getId()).stream()
                        .filter(s -> s.getDataHoraInicio().toLocalDate().equals(agendaDate))
                        .filter(s -> s.getDataHoraInicio().toLocalTime().equals(LocalTime.of(10, 0)))
                        .filter(s -> s.getStatus() == SessaoStatus.AGENDADA)
                        .count());

        org.junit.jupiter.api.Assertions.assertEquals(Boolean.FALSE, sessaoAgendada.getPlanoConsumoRegistrado());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void planosConsumoAjustaSessaoRealizadaPendenteEPermaneceNaMesmaTela() throws Exception {
        PlanoSessaoEntity planoAntes = planoSessaoRepository.findAll().stream()
                .filter(item -> item.getCliente().getId().equals(clienteAgenda.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Plano de teste nÃ£o encontrado para ajuste de sessÃµes."));
        int consumoInicial = planoAntes.getSessoesConsumidas();

        SessaoEntity pendente = sessaoRepository.save(SessaoEntity.builder()
                .cliente(clienteAgenda)
                .planoSessao(null)
                .dataHoraInicio(agendaDate.plusDays(1).atTime(11, 0).atZone(ZoneId.of("America/Campo_Grande")).toOffsetDateTime())
                .dataHoraFim(agendaDate.plusDays(1).atTime(11, 50).atZone(ZoneId.of("America/Campo_Grande")).toOffsetDateTime())
                .status(SessaoStatus.REALIZADA)
                .tipoAgendamento(TipoAgendamento.PLANO)
                .tipoAtendimento("RPG")
                .planoConsumoRegistrado(Boolean.FALSE)
                .build());

        mockMvc.perform(post("/planos-consumo/{pacienteId}/ajustar-sessoes", clienteAgenda.getId())
                        .with(csrf())
                        .param("planoId", planoAntes.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planos-consumo?pacienteId=" + clienteAgenda.getId()));

        PlanoSessaoEntity planoDepois = planoSessaoRepository.findById(planoAntes.getId())
                .orElseThrow(() -> new IllegalStateException("Plano de teste nÃ£o encontrado apÃ³s ajuste."));
        SessaoEntity sessaoDepois = sessaoRepository.findById(pendente.getId())
                .orElseThrow(() -> new IllegalStateException("SessÃ£o pendente nÃ£o encontrada apÃ³s ajuste."));

        org.junit.jupiter.api.Assertions.assertEquals(consumoInicial + 1, planoDepois.getSessoesConsumidas());
        org.junit.jupiter.api.Assertions.assertEquals(planoAntes.getId(), sessaoDepois.getPlanoSessao().getId());
        org.junit.jupiter.api.Assertions.assertTrue(sessaoDepois.getPlanoConsumoRegistrado());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void planosConsumoExcluiPlanoComSessaoVinculada() throws Exception {
        PlanoSessaoEntity plano = planoSessaoRepository.findAll().stream()
                .filter(item -> item.getCliente().getId().equals(clienteAgenda.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Plano de teste não encontrado para exclusão."));

        mockMvc.perform(post("/planos-consumo/{id}/excluir", plano.getId())
                        .with(csrf())
                        .param("paciente", clienteAgenda.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planos-consumo?paciente=" + clienteAgenda.getId()));

        org.junit.jupiter.api.Assertions.assertFalse(planoSessaoRepository.existsById(plano.getId()));

        SessaoEntity sessao = sessaoRepository.findById(sessaoAgendaId)
                .orElseThrow(() -> new IllegalStateException("Sessão de teste não encontrada após excluir o plano."));
        org.junit.jupiter.api.Assertions.assertNull(sessao.getPlanoSessao());
        org.junit.jupiter.api.Assertions.assertEquals(TipoAgendamento.AVULSO, sessao.getTipoAgendamento());
        org.junit.jupiter.api.Assertions.assertFalse(sessao.getPlanoConsumoRegistrado());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void financeiroExibeEditarCancelarEMarcarPagoParaLancamentoPendente() throws Exception {
        UUID lancamentoId = criarLancamentoFinanceiroPendente().getId();

        mockMvc.perform(get("/financeiro")
                        .param("inicio", agendaDate.withDayOfMonth(1).toString())
                        .param("fim", agendaDate.toString())
                        .param("editar", lancamentoId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Editar Lançamento")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Salvar alterações")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Baixa")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cancelar")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void relatoriosRenderizamMesmoComReceitaVinculadaAoPaciente() throws Exception {
        lancamentoFinanceiroRepository.save(LancamentoFinanceiroEntity.builder()
                .tipo(TipoLancamentoFinanceiro.RECEITA)
                .status(StatusLancamentoFinanceiro.PENDENTE)
                .descricao("Receita vinculada ao paciente")
                .categoria("Planos de tratamento")
                .valor(new BigDecimal("350.00"))
                .dataRecebimento(OffsetDateTime.now(ZoneId.of("America/Campo_Grande")).withHour(10).withMinute(0).withSecond(0).withNano(0))
                .cliente(clienteAgenda)
                .build());

        mockMvc.perform(get("/relatorios"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Receita vinculada ao paciente")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Paciente Teste Agenda")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void salvarPermissoesDaFuncaoRedirecionaSemErro() throws Exception {
        mockMvc.perform(get("/funcoes-usuarios"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/funcoes-usuarios/FISIOTERAPEUTA")
                        .with(csrf())
                        .param("menus", "DASHBOARD", "AGENDA", "PACIENTES", "RELATORIOS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/funcoes-usuarios"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void acessarRotaGetDaFuncaoEspecificaNaoPodeExplodir() throws Exception {
        mockMvc.perform(get("/funcoes-usuarios/FISIOTERAPEUTA"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Fisioterapeuta")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void dashboardMostraInformacoesNaNavbarSemCardDuplicado() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("header-insights")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Status do sistema")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Data atual")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Hora atual")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/pacientes\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/calendario?data=")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/solicitacoes\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/financeiro#financeiroReceitasPendentes\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/financeiro#financeiroDespesasPendentes\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/financeiro#financeiroSaldoProjetado\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("href=\"/whatsapp"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Fila WhatsApp"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("info-list"))));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void dashboardExibeSeletorDeTemaNaSidebar() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tema da interface")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"LIGHT\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"DARK\"")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void usuarioPodeAtualizarTemaEscuro() throws Exception {
        mockMvc.perform(post("/preferencias/tema")
                        .with(csrf())
                        .header("Referer", "http://localhost/dashboard")
                        .param("tema", "DARK"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        UserThemePreference tema = usuarioSistemaRepository.findByUsername("admin")
                .orElseThrow(() -> new IllegalStateException("Usuário admin não encontrado para validar o tema."))
                .getTemaInterface();

        org.junit.jupiter.api.Assertions.assertEquals(UserThemePreference.DARK, tema);
    }

    private void prepararCenarioAgenda() {
        sessaoRepository.deleteAll();
        lancamentoFinanceiroRepository.deleteAll();
        planoSessaoRepository.deleteAll();
        clienteRepository.deleteAll();
        configuracaoClinicaRepository.deleteAll();

        agendaDate = LocalDate.now().plusDays(1);
        ZoneId zoneId = ZoneId.of("America/Campo_Grande");

        configuracaoClinicaRepository.save(ConfiguracaoClinicaEntity.builder()
                .nomeClinica("Clínica Teste")
                .diasFuncionamento("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY")
                .horarioAbertura(LocalTime.of(8, 0))
                .horarioFechamento(LocalTime.of(18, 0))
                .duracaoSessaoMinutos(50)
                .timezoneId("America/Campo_Grande")
                .ativo(true)
                .build());

        clienteAgenda = clienteRepository.save(ClienteEntity.builder()
                .nomeCompleto("Paciente Teste Agenda")
                .telefone("65999990000")
                .email("paciente.agenda@example.com")
                .ativo(true)
                .build());

        PlanoSessaoEntity plano = planoSessaoRepository.save(PlanoSessaoEntity.builder()
                .cliente(clienteAgenda)
                .nomePlano("Plano Teste Agenda")
                .sessoesContratadas(10)
                .sessoesConsumidas(1)
                .valorTotal(new BigDecimal("1200.00"))
                .dataInicio(agendaDate.minusDays(7))
                .dataValidade(agendaDate.plusDays(30))
                .status(PlanoSessaoStatus.ATIVO)
                .build());

        SessaoEntity sessao = sessaoRepository.save(SessaoEntity.builder()
                .cliente(clienteAgenda)
                .planoSessao(plano)
                .dataHoraInicio(agendaDate.atTime(9, 0).atZone(zoneId).toOffsetDateTime())
                .dataHoraFim(agendaDate.atTime(9, 50).atZone(zoneId).toOffsetDateTime())
                .status(SessaoStatus.AGENDADA)
                .tipoAtendimento("Pilates")
                .observacoes("Sessão criada no smoke test.")
                .planoConsumoRegistrado(Boolean.TRUE)
                .build());

        sessaoAgendaId = sessao.getId();
    }

    private LancamentoFinanceiroEntity criarLancamentoFinanceiroPendente() {
        PlanoSessaoEntity plano = planoSessaoRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Plano de teste não encontrado para o financeiro."));

        return lancamentoFinanceiroRepository.save(LancamentoFinanceiroEntity.builder()
                .tipo(TipoLancamentoFinanceiro.RECEITA)
                .status(StatusLancamentoFinanceiro.PENDENTE)
                .descricao("Plano de sessões: Acompanhamento domiciliar")
                .categoria("Planos de tratamento")
                .valor(new BigDecimal("1.00"))
                .dataRecebimento(agendaDate.atStartOfDay(ZoneId.of("America/Campo_Grande")).toOffsetDateTime())
                .cliente(clienteAgenda)
                .planoSessao(plano)
                .build());
    }
}
