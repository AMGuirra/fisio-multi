package com.fisiogestao.web;

import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
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
import com.fisiogestao.pacientes.PacoteSessaoEntity;
import com.fisiogestao.pacientes.PacoteSessaoStatus;
import com.fisiogestao.pacientes.repository.ClienteRepository;
import com.fisiogestao.pacientes.repository.PacoteSessaoRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SuppressWarnings("null")
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
    private PacoteSessaoRepository pacoteSessaoRepository;

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

    @Test
    void loginRenderizaComTokenCsrf() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    void patientLookupApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/pacientes/por-cpf").param("cpf", "12345678901"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerUiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isUnauthorized());
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
            "/",
            "/dashboard",
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
            "/usuarios",
            "/funcoes-usuarios"
    })
    void securedPagesRenderForAdmin(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void rotaLegadaDeBloqueiosRedirecionaParaConfiguracaoClinica() throws Exception {
        mockMvc.perform(get("/bloqueios"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/configuracoes/clinica"));
    }

    @ParameterizedTest
    @WithMockUser(username = RECEPCAO_USERNAME, roles = "RECEPCAO")
    @ValueSource(strings = {
            "/calendario",
            "/solicitacoes",
            "/pacientes",
            "/pacotes-consumo"
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
    void pacotesConsumoAbreModalDeAgendamentoAoSelecionarPaciente() throws Exception {
        mockMvc.perform(get("/pacotes-consumo")
                        .param("paciente", clienteAgenda.getId().toString())
                        .param("agendar", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"openAgendamentoSessaoModal\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"agendamentoSessaoModal\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-hidden=\"false\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Paciente em atendimento")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Buscar Paciente"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Plano Teste Agenda")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void pacotesConsumoAgendaSessaoEPermaneceNaMesmaTela() throws Exception {
        PacoteSessaoEntity pacoteAntes = pacoteSessaoRepository.findAll().stream()
                .filter(item -> item.getCliente().getId().equals(clienteAgenda.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Pacote de teste não encontrado para o agendamento via planos."));

        mockMvc.perform(post("/pacotes-consumo/agendar-sessao")
                        .with(csrf())
                        .param("clienteId", clienteAgenda.getId().toString())
                        .param("pacoteSessaoId", pacoteAntes.getId().toString())
                        .param("data", agendaDate.toString())
                        .param("horario", "10:00")
                        .param("tipoAtendimento", "RPG")
                        .param("observacoes", "Agendamento criado pela tela de planos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pacotes-consumo?paciente=" + clienteAgenda.getId()));

        PacoteSessaoEntity pacoteDepois = pacoteSessaoRepository.findById(pacoteAntes.getId())
                .orElseThrow(() -> new IllegalStateException("Pacote de teste não encontrado após o agendamento."));

        org.junit.jupiter.api.Assertions.assertEquals(
                pacoteAntes.getSessoesConsumidas() + 1,
                pacoteDepois.getSessoesConsumidas()
        );

        long sessoesAgendadas = sessaoRepository.findByClienteIdOrderByDataHoraInicioDesc(clienteAgenda.getId()).stream()
                .filter(sessao -> sessao.getDataHoraInicio().toLocalDate().equals(agendaDate))
                .filter(sessao -> sessao.getDataHoraInicio().toLocalTime().equals(LocalTime.of(10, 0)))
                .filter(sessao -> sessao.getStatus() == SessaoStatus.AGENDADA)
                .count();

        org.junit.jupiter.api.Assertions.assertEquals(1L, sessoesAgendadas);
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Marcar pago")))
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
                .dataCompetencia(OffsetDateTime.now(ZoneId.of("America/Campo_Grande")).withHour(10).withMinute(0).withSecond(0).withNano(0))
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
        pacoteSessaoRepository.deleteAll();
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

        PacoteSessaoEntity pacote = pacoteSessaoRepository.save(PacoteSessaoEntity.builder()
                .cliente(clienteAgenda)
                .nomePacote("Plano Teste Agenda")
                .sessoesContratadas(10)
                .sessoesConsumidas(1)
                .valorTotal(new BigDecimal("1200.00"))
                .dataInicio(agendaDate.minusDays(7))
                .dataValidade(agendaDate.plusDays(30))
                .status(PacoteSessaoStatus.ATIVO)
                .build());

        SessaoEntity sessao = sessaoRepository.save(SessaoEntity.builder()
                .cliente(clienteAgenda)
                .pacoteSessao(pacote)
                .dataHoraInicio(agendaDate.atTime(9, 0).atZone(zoneId).toOffsetDateTime())
                .dataHoraFim(agendaDate.atTime(9, 50).atZone(zoneId).toOffsetDateTime())
                .status(SessaoStatus.AGENDADA)
                .tipoAtendimento("Pilates")
                .observacoes("Sessão criada no smoke test.")
                .pacoteConsumoRegistrado(Boolean.TRUE)
                .build());

        sessaoAgendaId = sessao.getId();
    }

    private LancamentoFinanceiroEntity criarLancamentoFinanceiroPendente() {
        PacoteSessaoEntity pacote = pacoteSessaoRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Pacote de teste não encontrado para o financeiro."));

        return lancamentoFinanceiroRepository.save(LancamentoFinanceiroEntity.builder()
                .tipo(TipoLancamentoFinanceiro.RECEITA)
                .status(StatusLancamentoFinanceiro.PENDENTE)
                .descricao("Pacote de sessões: Acompanhamento domiciliar")
                .categoria("Pacotes")
                .valor(new BigDecimal("1.00"))
                .dataCompetencia(agendaDate.atStartOfDay(ZoneId.of("America/Campo_Grande")).toOffsetDateTime())
                .cliente(clienteAgenda)
                .pacoteSessao(pacote)
                .build());
    }
}
