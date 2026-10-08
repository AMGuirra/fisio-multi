package com.fisiogestao.cancelamento;

import com.fisiogestao.auth.UserRole;
import com.fisiogestao.auth.UsuarioSistemaEntity;
import com.fisiogestao.email.EmailLogType;
import com.fisiogestao.email.service.EmailRequest;
import com.fisiogestao.empresa.ConfiguracaoPlataformaService;
import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.empresa.EmpresaStatus;
import com.fisiogestao.gestor.EmpresaGestaoService;
import com.fisiogestao.gestor.NotificacaoGestorRepository;
import com.fisiogestao.gestor.TipoNotificacaoGestor;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.support.MultiEmpresaTestBase;
import com.fisiogestao.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Solicitação de cancelamento pela clínica e decisão/reativação pelo SUPER_ADMIN. */
class CancelamentoClinicaTest extends MultiEmpresaTestBase {

    @Autowired private SolicitacaoCancelamentoRepository repository;
    @Autowired private CancelamentoService cancelamentoService;
    @Autowired private EmpresaGestaoService empresaGestaoService;
    @Autowired private NotificacaoGestorRepository notificacaoGestorRepository;
    @Autowired private ConfiguracaoPlataformaService configuracaoPlataformaService;

    private EmpresaEntity clinica;
    private UsuarioSistemaEntity admin;
    private UsuarioSistemaEntity recepcao;
    private UsuarioSistemaEntity superAdmin;
    private ClienteEntity paciente;

    @BeforeEach
    void preparar() {
        configuracaoPlataformaService.salvar(15, 3, "admin-cancelamento@plataforma.test");
        clinica = criarEmpresa("cancela", EmpresaStatus.ATIVO);
        admin = criarUsuario(clinica, "admin-cancela", UserRole.ADMIN_CLINICA);
        recepcao = criarUsuario(clinica, "recepcao-cancela", UserRole.RECEPCAO);
        superAdmin = usuarioRepository.findByEmpresaIdIsNullAndUsername("superadmin").orElseThrow();
        paciente = criarPaciente(clinica, "Paciente Preservado");
    }

    @AfterEach
    void restaurar() {
        configuracaoPlataformaService.salvar(15, 3, null);
    }

    private void solicitarComo(UsuarioSistemaEntity usuario) throws Exception {
        mockMvc.perform(post("/configuracoes/clinica/cancelamento").with(csrf()).with(comoUsuario(usuario))
                .param("nomeSolicitante", "Responsável")
                .param("emailContato", "resp@clinica.test")
                .param("motivo", "CUSTO")
                .param("descricao", "Vamos encerrar as atividades.")
                .param("confirmacao", "true"));
    }

    private List<SolicitacaoCancelamentoEntity> daClinica() {
        return repository.findAll().stream().filter(s -> clinica.getId().equals(s.getEmpresaId())).toList();
    }

    @Test
    void somenteAdminClinicaSolicitaCancelamentoEUsuarioComumNao() throws Exception {
        solicitarComo(recepcao);
        assertTrue(daClinica().isEmpty(), "Recepção não pode solicitar cancelamento em nome da clínica.");

        solicitarComo(admin);
        List<SolicitacaoCancelamentoEntity> solicitacoes = daClinica();
        assertEquals(1, solicitacoes.size());
        assertEquals(StatusSolicitacaoCancelamento.PENDENTE, solicitacoes.getFirst().getStatus());
        assertEquals(OrigemSolicitacaoCancelamento.SISTEMA, solicitacoes.getFirst().getOrigem());

        // Sem confirmação explícita, não registra.
        long antes = repository.count();
        mockMvc.perform(post("/configuracoes/clinica/cancelamento").with(csrf()).with(comoUsuario(admin))
                .param("nomeSolicitante", "X").param("emailContato", "x@x.test").param("motivo", "CUSTO"));
        assertEquals(antes, repository.count());
    }

    @Test
    void solicitacaoApareceNoGestorNotificaENaoBloqueiaNemApagaDados() throws Exception {
        solicitarComo(admin);
        SolicitacaoCancelamentoEntity solicitacao = daClinica().getFirst();

        // Empresa continua ativa, com acesso e dados.
        assertEquals(EmpresaStatus.ATIVO, empresaRepository.findById(clinica.getId()).orElseThrow().getStatus());
        assertEquals("/dashboard", login("admin-cancela", SENHA, clinica.getSlug()).getResponse().getRedirectedUrl());
        assertTrue(TenantContext.executarComo(clinica.getId(), () -> clienteRepository.findById(paciente.getId())).isPresent());

        assertTrue(notificacaoGestorRepository.findByTipoAndEmpresaId(TipoNotificacaoGestor.CANCELAMENTO, clinica.getId())
                .stream().findAny().isPresent());
        ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(systemEmailService, atLeastOnce()).sendPlataforma(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(r -> r.type() == EmailLogType.NOTIFICACAO_ADMINISTRATIVA
                && r.to().contains("admin-cancelamento@plataforma.test")));

        mockMvc.perform(get("/gestor/cancelamentos").with(comoUsuario(superAdmin)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(clinica.getNome())));
        mockMvc.perform(get("/gestor/cancelamentos/" + solicitacao.getId()).with(comoUsuario(superAdmin)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Custo")));
    }

    @Test
    void conclusaoInativaEmpresaBloqueiaAcessoEReativacaoLiberaNovamente() throws Exception {
        solicitarComo(admin);
        SolicitacaoCancelamentoEntity solicitacao = daClinica().getFirst();
        var sessaoAntes = sessao(login("admin-cancela", SENHA, clinica.getSlug()));

        autenticarComo(superAdmin);
        cancelamentoService.colocarEmAnalise(solicitacao.getId(), "Analisando");
        cancelamentoService.concluir(solicitacao.getId(), "Conforme solicitado");

        EmpresaEntity inativa = empresaRepository.findById(clinica.getId()).orElseThrow();
        assertEquals(EmpresaStatus.INATIVO, inativa.getStatus());
        SolicitacaoCancelamentoEntity concluida = cancelamentoService.detalhar(solicitacao.getId());
        assertEquals(StatusSolicitacaoCancelamento.CANCELADA, concluida.getStatus());
        assertTrue(concluida.getHistorico().size() >= 3, "Histórico registra abertura, análise e conclusão.");

        // Sessão existente cai na próxima requisição; novo login é negado.
        mockMvc.perform(get("/dashboard").session(sessaoAntes))
                .andExpect(status().is3xxRedirection());
        assertEquals("/acesso-negado?motivo=INATIVO",
                login("admin-cancela", SENHA, clinica.getSlug()).getResponse().getRedirectedUrl());
        // Dados preservados.
        assertTrue(TenantContext.executarComo(clinica.getId(), () -> clienteRepository.findById(paciente.getId())).isPresent());

        // Reativação em TESTE com dias úteis escolhidos.
        autenticarComo(superAdmin);
        EmpresaEntity reativada = empresaGestaoService.iniciarTeste(clinica.getId(), 12);
        assertEquals(EmpresaStatus.TESTE, reativada.getStatus());
        assertEquals(12, reativada.getQuantidadeDiasUteisTeste());
        assertEquals("/dashboard", login("admin-cancela", SENHA, clinica.getSlug()).getResponse().getRedirectedUrl());
    }

    @Test
    void isolamentoValeParaSolicitacoesDeCancelamento() throws Exception {
        solicitarComo(admin);
        EmpresaEntity outra = criarEmpresa("outra-cancela", EmpresaStatus.ATIVO);
        UsuarioSistemaEntity adminOutra = criarUsuario(outra, "admin-outra", UserRole.ADMIN_CLINICA);

        autenticarComo(adminOutra);
        assertTrue(cancelamentoService.listarDaClinicaAtual().isEmpty(), "Outra clínica não vê solicitações alheias.");
        mockMvc.perform(get("/configuracoes/clinica").with(comoUsuario(adminOutra)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Vamos encerrar as atividades."))));
        // Usuário de clínica não acessa a gestão de cancelamentos.
        mockMvc.perform(get("/gestor/cancelamentos").with(comoUsuario(adminOutra))).andExpect(status().isForbidden());
    }

    @Test
    void solicitacaoPublicaIdentificaEmpresaPeloCnpjSemAlterarStatus() throws Exception {
        String cnpj = cnpjValidoAleatorio();
        clinica.setCnpj(cnpj);
        empresaRepository.save(clinica);
        mockMvc.perform(post("/suporte/cancelamento").with(csrf()).with(ipAleatorio())
                        .param("nome", "Dono")
                        .param("email", "dono@clinica.test")
                        .param("cnpj", cnpj)
                        .param("motivo", "OUTRO_SISTEMA")
                        .param("confirmacao", "true"))
                .andExpect(status().is3xxRedirection());
        assertTrue(daClinica().stream().anyMatch(s -> s.getOrigem() == OrigemSolicitacaoCancelamento.PUBLICA));
        assertEquals(EmpresaStatus.ATIVO, empresaRepository.findById(clinica.getId()).orElseThrow().getStatus());
    }
}
