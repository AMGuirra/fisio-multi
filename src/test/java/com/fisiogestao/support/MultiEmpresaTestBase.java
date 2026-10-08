package com.fisiogestao.support;

import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.auth.UserRole;
import com.fisiogestao.auth.UsuarioAutenticado;
import com.fisiogestao.auth.UsuarioSistemaEntity;
import com.fisiogestao.auth.UsuarioSistemaRepository;
import com.fisiogestao.email.service.EmailDispatchResult;
import com.fisiogestao.email.service.SystemEmailService;
import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.empresa.EmpresaRepository;
import com.fisiogestao.empresa.EmpresaStatus;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.repository.ClienteRepository;
import com.fisiogestao.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Base dos testes multiempresa: banco H2 próprio (não interfere nos demais testes), e-mail simulado
 * e utilitários para criar clínicas, usuários e pacientes já no escopo correto.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fisio_multiempresa_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class MultiEmpresaTestBase {

    public static final String SENHA = "SenhaForte123";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected EmpresaRepository empresaRepository;
    @Autowired protected UsuarioSistemaRepository usuarioRepository;
    @Autowired protected ClienteRepository clienteRepository;
    @Autowired protected ConfiguracaoClinicaRepository configuracaoClinicaRepository;
    @Autowired protected PasswordEncoder passwordEncoder;

    @MockitoBean protected SystemEmailService systemEmailService;

    @BeforeEach
    void configurarEmailSimulado() {
        lenient().when(systemEmailService.send(any())).thenReturn(EmailDispatchResult.success("ok"));
        lenient().when(systemEmailService.sendPlataforma(any())).thenReturn(EmailDispatchResult.success("ok"));
    }

    @AfterEach
    void limparSeguranca() {
        SecurityContextHolder.clearContext();
    }

    protected EmpresaEntity criarEmpresa(String prefixo, EmpresaStatus status) {
        String slug = prefixo + "-" + UUID.randomUUID().toString().substring(0, 8);
        EmpresaEntity empresa = EmpresaEntity.builder()
                .nome("Clínica " + prefixo)
                .slug(slug)
                .email(slug + "@clinica.test")
                .status(status)
                .build();
        if (status == EmpresaStatus.TESTE) {
            empresa.setDataInicioTeste(OffsetDateTime.now().minusDays(1));
            empresa.setDataFimTeste(OffsetDateTime.now().plusDays(10));
            empresa.setQuantidadeDiasUteisTeste(10);
        }
        if (status == EmpresaStatus.BLOQUEADO || status == EmpresaStatus.INATIVO) {
            empresa.setMotivoBloqueio("Motivo de teste");
        }
        EmpresaEntity salva = empresaRepository.save(empresa);
        TenantContext.executarComo(salva.getId(), () -> configuracaoClinicaRepository.save(ConfiguracaoClinicaEntity.builder()
                .nomeClinica(salva.getNome())
                .timezoneId("America/Campo_Grande")
                .horarioAbertura(LocalTime.of(7, 0))
                .horarioFechamento(LocalTime.of(19, 0))
                .duracaoSessaoMinutos(60)
                .diasFuncionamento("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
                .ativo(true)
                .build()));
        return salva;
    }

    protected UsuarioSistemaEntity criarUsuario(EmpresaEntity empresa, String username, UserRole role) {
        return usuarioRepository.save(UsuarioSistemaEntity.builder()
                .nome("Usuário " + username)
                .username(username)
                .email(username + "@" + (empresa == null ? "plataforma" : empresa.getSlug()) + ".test")
                .passwordHash(passwordEncoder.encode(SENHA))
                .role(role)
                .empresaId(empresa == null ? null : empresa.getId())
                .build());
    }

    protected ClienteEntity criarPaciente(EmpresaEntity empresa, String nome) {
        return TenantContext.executarComo(empresa.getId(), () -> clienteRepository.save(ClienteEntity.builder()
                .nomeCompleto(nome)
                .telefone("679" + ThreadLocalRandom.current().nextInt(10000000, 99999999))
                .email(nome.toLowerCase().replaceAll("[^a-z]", "") + "@paciente.test")
                .ativo(true)
                .build()));
    }

    /** Principal real para MockMvc ({@code .with(comoUsuario(...))}). */
    protected RequestPostProcessor comoUsuario(UsuarioSistemaEntity usuario) {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .authentication(autenticacao(usuario));
    }

    protected Authentication autenticacao(UsuarioSistemaEntity usuario) {
        String slug = usuario.getEmpresaId() == null ? null
                : empresaRepository.findById(usuario.getEmpresaId()).map(EmpresaEntity::getSlug).orElse(null);
        UsuarioAutenticado principal = UsuarioAutenticado.of(usuario, slug);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    /** Autentica a thread do teste para chamadas diretas a serviços. */
    protected void autenticarComo(UsuarioSistemaEntity usuario) {
        SecurityContextHolder.getContext().setAuthentication(autenticacao(usuario));
    }

    /** CNPJ válido e único para não colidir entre classes que compartilham o banco de teste. */
    protected static String cnpjValidoAleatorio() {
        String base = String.format("%012d", ThreadLocalRandom.current().nextLong(1_000_000_000L, 999_999_999_999L));
        int d1 = digitoCnpj(base, new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        int d2 = digitoCnpj(base + d1, new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return base + d1 + d2;
    }

    private static int digitoCnpj(String s, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (s.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /** IP aleatório para não esbarrar no rate limiting entre testes. */
    protected static RequestPostProcessor ipAleatorio() {
        String ip = "10." + ThreadLocalRandom.current().nextInt(1, 250) + "."
                + ThreadLocalRandom.current().nextInt(1, 250) + "." + ThreadLocalRandom.current().nextInt(1, 250);
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    protected MvcResult login(String username, String senha, String clinica) throws Exception {
        var requisicao = post("/login")
                .with(csrf())
                .with(ipAleatorio())
                .param("username", username)
                .param("password", senha);
        if (clinica != null) {
            requisicao.param("clinica", clinica);
        }
        return mockMvc.perform(requisicao).andReturn();
    }

    protected static MockHttpSession sessao(MvcResult resultado) {
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }
}
