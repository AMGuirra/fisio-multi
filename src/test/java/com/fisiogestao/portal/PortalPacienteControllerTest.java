package com.fisiogestao.portal;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.repository.ClienteRepository;
import com.fisiogestao.email.service.EmailRequest;
import com.fisiogestao.email.service.SystemEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Cobre o login do portal do paciente por identificador (e-mail/CPF) + senha,
 * garantindo que o link mágico ({@code /paciente/{token}}) sozinho não concede
 * mais acesso e que o roteamento entre {@code /paciente/login} e
 * {@code /paciente/{token}} não colide.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PortalPacienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private SessaoRepository sessaoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private SystemEmailService systemEmailService;

    private ClienteEntity pacienteComSenha;
    private ClienteEntity pacienteSemEmail;

    @BeforeEach
    void setUp() {
        pacienteComSenha = clienteRepository.save(ClienteEntity.builder()
                .nomeCompleto("Maria Portal Teste")
                .email("maria.portal.teste@example.com")
                .cpf("52998224725")
                .senhaPortal(passwordEncoder.encode("Senha123"))
                .build());

        pacienteSemEmail = clienteRepository.save(ClienteEntity.builder()
                .nomeCompleto("Joao Sem Email Teste")
                .telefone("67999990000")
                .cpf("11144477735")
                .build());
    }

    @Test
    void loginGetNaoEhConfundidoComRotaDeToken() throws Exception {
        mockMvc.perform(get("/paciente/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("portal/login"));
    }

    @Test
    void esqueciSenhaGetNaoEhConfundidoComRotaDeToken() throws Exception {
        mockMvc.perform(get("/paciente/esqueci-minha-senha"))
                .andExpect(status().isOk())
                .andExpect(view().name("portal/esqueci-senha-portal"));
    }

    @Test
    void acessarDashboardSemSessaoRedirecionaParaLogin() throws Exception {
        mockMvc.perform(get("/paciente/{token}", pacienteComSenha.getPortalToken()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/paciente/login"));
    }

    @Test
    void confirmarPresencaSemSessaoDoPortalRedirecionaENaoAlteraSessao() throws Exception {
        SessaoEntity sessao = sessaoRepository.save(SessaoEntity.builder()
                .cliente(pacienteComSenha)
                .dataHoraInicio(OffsetDateTime.now().plusDays(2))
                .dataHoraFim(OffsetDateTime.now().plusDays(2).plusMinutes(50))
                .status(SessaoStatus.AGENDADA)
                .build());

        mockMvc.perform(post("/paciente/{token}/sessao/{sessaoId}/confirmar",
                        pacienteComSenha.getPortalToken(), sessao.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/paciente/login"));

        assertFalse(sessaoRepository.findById(sessao.getId()).orElseThrow().getConfirmadoPeloPaciente());
    }

    @Test
    void tokenInexistenteMostraPaginaDeTokenInvalido() throws Exception {
        mockMvc.perform(get("/paciente/{token}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(view().name("portal/token-invalido"));
    }

    @Test
    void loginComSenhaErradaNaoAutentica() throws Exception {
        mockMvc.perform(post("/paciente/login")
                        .with(csrf())
                        .param("identificador", pacienteComSenha.getEmail())
                        .param("senha", "senha-errada"))
                .andExpect(status().isOk())
                .andExpect(view().name("portal/login"));
    }

    @Test
    void loginComEmailCorretoAutenticaEAcessaDashboard() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/paciente/login")
                        .with(csrf())
                        .param("identificador", pacienteComSenha.getEmail())
                        .param("senha", "Senha123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/paciente/" + pacienteComSenha.getPortalToken()))
                .andReturn();

        mockMvc.perform(get("/paciente/{token}", pacienteComSenha.getPortalToken())
                        .session((org.springframework.mock.web.MockHttpSession)
                                loginResult.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(view().name("portal/index"));
    }

    @Test
    void loginComCpfCorretoAutentica() throws Exception {
        mockMvc.perform(post("/paciente/login")
                        .with(csrf())
                        .param("identificador", pacienteComSenha.getCpf())
                        .param("senha", "Senha123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/paciente/" + pacienteComSenha.getPortalToken()));
    }

    @Test
    void contaSemSenhaNaoConsegueLogar() throws Exception {
        mockMvc.perform(post("/paciente/login")
                        .with(csrf())
                        .param("identificador", pacienteSemEmail.getCpf())
                        .param("senha", "qualquer-coisa"))
                .andExpect(status().isOk())
                .andExpect(view().name("portal/login"));
    }

    @Test
    void esqueciSenhaComIdentificadorSemEmailNaoLancaExcecao() throws Exception {
        mockMvc.perform(post("/paciente/esqueci-minha-senha")
                        .with(csrf())
                        .param("identificador", pacienteSemEmail.getTelefone()))
                .andExpect(status().isOk())
                .andExpect(view().name("portal/esqueci-senha-portal"));

        assertFalse(clienteRepository.findById(pacienteSemEmail.getId()).isEmpty());
    }

    @Test
    void esqueciSenhaComIdentificadorInexistenteNaoLancaExcecao() throws Exception {
        mockMvc.perform(post("/paciente/esqueci-minha-senha")
                        .with(csrf())
                        .param("identificador", "ninguem@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("portal/esqueci-senha-portal"));
        assertTrue(true);
    }

    @Test
    void resetDoPacienteIgnoraHostEncaminhado() throws Exception {
        mockMvc.perform(post("/paciente/esqueci-minha-senha")
                        .with(csrf())
                        .header("X-Forwarded-Host", "host-controlado.invalid")
                        .header("X-Forwarded-Proto", "https")
                        .param("identificador", pacienteComSenha.getEmail()))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<EmailRequest> captor = org.mockito.ArgumentCaptor.forClass(EmailRequest.class);
        verify(systemEmailService).send(captor.capture());
        String link = (String) captor.getValue().templateVariables().get("linkRedefinicao");
        assertTrue(link.startsWith("http://localhost:8083/paciente/resetar-senha?token="));
        assertFalse(link.contains("host-controlado.invalid"));
    }
}
