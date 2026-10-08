package com.fisiogestao.tenant;

import com.fisiogestao.auth.UserRole;
import com.fisiogestao.auth.UsuarioSistemaEntity;
import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.empresa.EmpresaStatus;
import com.fisiogestao.support.MultiEmpresaTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Requisitos 5 a 13: área Gestor, papéis e regra de acesso por status da empresa. */
class AcessoEmpresaStatusTest extends MultiEmpresaTestBase {

    @Test
    void superAdminAcessaGestor() throws Exception {
        UsuarioSistemaEntity superAdmin = usuarioRepository.findByEmpresaIdIsNullAndUsername("superadmin").orElseThrow();
        mockMvc.perform(get("/gestor").with(comoUsuario(superAdmin))).andExpect(status().isOk());
        mockMvc.perform(get("/gestor/empresas").with(comoUsuario(superAdmin))).andExpect(status().isOk());
        mockMvc.perform(get("/gestor/configuracoes").with(comoUsuario(superAdmin))).andExpect(status().isOk());
        mockMvc.perform(get("/gestor/suporte").with(comoUsuario(superAdmin))).andExpect(status().isOk());
        mockMvc.perform(get("/gestor/cancelamentos").with(comoUsuario(superAdmin))).andExpect(status().isOk());
        mockMvc.perform(get("/gestor/auditoria").with(comoUsuario(superAdmin))).andExpect(status().isOk());

        // Login real do superadmin (sem código de clínica) vai direto para o Gestor.
        MvcResult login = login("superadmin", "admin", null);
        assertEquals("/gestor", login.getResponse().getRedirectedUrl());
    }

    @Test
    void usuarioComumNaoAcessaGestor() throws Exception {
        EmpresaEntity clinica = criarEmpresa("sem-gestor", EmpresaStatus.ATIVO);
        UsuarioSistemaEntity admin = criarUsuario(clinica, "admin-clinica", UserRole.ADMIN_CLINICA);
        UsuarioSistemaEntity recepcao = criarUsuario(clinica, "recepcao", UserRole.RECEPCAO);
        mockMvc.perform(get("/gestor").with(comoUsuario(admin))).andExpect(status().isForbidden());
        mockMvc.perform(get("/gestor/empresas").with(comoUsuario(admin))).andExpect(status().isForbidden());
        mockMvc.perform(get("/gestor/configuracoes").with(comoUsuario(recepcao))).andExpect(status().isForbidden());
    }

    @Test
    void superAdminNaoAcessaPaginasOperacionaisDeClinica() throws Exception {
        UsuarioSistemaEntity superAdmin = usuarioRepository.findByEmpresaIdIsNullAndUsername("superadmin").orElseThrow();
        for (String rota : new String[]{"/dashboard", "/pacientes", "/calendario", "/financeiro", "/prontuario", "/relatorios", "/usuarios"}) {
            mockMvc.perform(get(rota).with(comoUsuario(superAdmin)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/gestor?acessoNegado=1"));
        }
        mockMvc.perform(get("/api/pacientes/por-cpf").param("cpf", "52998224725").with(comoUsuario(superAdmin)))
                .andExpect(status().isForbidden());
    }

    @Test
    void empresaAtivaAcessaSemPrazo() throws Exception {
        EmpresaEntity ativa = criarEmpresa("ativa", EmpresaStatus.ATIVO);
        criarUsuario(ativa, "usuario-ativo", UserRole.ADMIN_CLINICA);
        MvcResult resultado = login("usuario-ativo", SENHA, ativa.getSlug());
        assertEquals("/dashboard", resultado.getResponse().getRedirectedUrl());
        mockMvc.perform(get("/dashboard").session(sessao(resultado))).andExpect(status().isOk());
    }

    @Test
    void empresaBloqueadaNaoAcessaEVeMensagemCorreta() throws Exception {
        EmpresaEntity bloqueada = criarEmpresa("bloqueada", EmpresaStatus.BLOQUEADO);
        criarUsuario(bloqueada, "usuario-bloq", UserRole.ADMIN_CLINICA);
        MvcResult resultado = login("usuario-bloq", SENHA, bloqueada.getSlug());
        assertEquals("/acesso-negado?motivo=BLOQUEADO", resultado.getResponse().getRedirectedUrl());
        mockMvc.perform(get("/acesso-negado").param("motivo", "BLOQUEADO"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("O acesso da sua clínica está bloqueado.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/suporte/solicitar")));
    }

    @Test
    void empresaInativaNaoAcessaEVeMensagemCorreta() throws Exception {
        EmpresaEntity inativa = criarEmpresa("inativa", EmpresaStatus.INATIVO);
        criarUsuario(inativa, "usuario-inat", UserRole.ADMIN_CLINICA);
        MvcResult resultado = login("usuario-inat", SENHA, inativa.getSlug());
        assertEquals("/acesso-negado?motivo=INATIVO", resultado.getResponse().getRedirectedUrl());
        mockMvc.perform(get("/acesso-negado").param("motivo", "INATIVO"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("A clínica está inativa.")));
    }

    @Test
    void empresaEmTesteValidoAcessa() throws Exception {
        EmpresaEntity teste = criarEmpresa("teste-ok", EmpresaStatus.TESTE);
        criarUsuario(teste, "usuario-teste", UserRole.ADMIN_CLINICA);
        MvcResult resultado = login("usuario-teste", SENHA, teste.getSlug());
        assertEquals("/dashboard", resultado.getResponse().getRedirectedUrl());
    }

    @Test
    void empresaComTesteExpiradoNaoAcessaEVeMensagemCorreta() throws Exception {
        EmpresaEntity expirada = criarEmpresa("teste-expirado", EmpresaStatus.TESTE);
        expirada.setDataInicioTeste(OffsetDateTime.now().minusDays(30));
        expirada.setDataFimTeste(OffsetDateTime.now().minusMinutes(1));
        empresaRepository.save(expirada);
        criarUsuario(expirada, "usuario-expirado", UserRole.ADMIN_CLINICA);

        MvcResult resultado = login("usuario-expirado", SENHA, expirada.getSlug());
        assertEquals("/acesso-negado?motivo=TESTE_EXPIRADO", resultado.getResponse().getRedirectedUrl());
        mockMvc.perform(get("/acesso-negado").param("motivo", "TESTE_EXPIRADO"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("O período de teste da sua clínica foi finalizado.")));
    }

    @Test
    void senhaErradaNaoRevelaStatusDaEmpresa() throws Exception {
        EmpresaEntity bloqueada = criarEmpresa("bloq-senha", EmpresaStatus.BLOQUEADO);
        criarUsuario(bloqueada, "usuario-x", UserRole.ADMIN_CLINICA);
        MvcResult resultado = login("usuario-x", "senha-errada", bloqueada.getSlug());
        assertTrue(resultado.getResponse().getRedirectedUrl().startsWith("/login?error"));
    }

    @Test
    void alteracaoDeStatusInvalidaSessaoExistenteNaRequisicaoSeguinte() throws Exception {
        EmpresaEntity clinica = criarEmpresa("sessao", EmpresaStatus.ATIVO);
        criarUsuario(clinica, "usuario-sessao", UserRole.ADMIN_CLINICA);
        MvcResult resultado = login("usuario-sessao", SENHA, clinica.getSlug());
        MockHttpSession sessao = sessao(resultado);
        mockMvc.perform(get("/dashboard").session(sessao)).andExpect(status().isOk());

        EmpresaEntity recarregada = empresaRepository.findById(clinica.getId()).orElseThrow();
        recarregada.setStatus(EmpresaStatus.BLOQUEADO);
        recarregada.setMotivoBloqueio("Inadimplência");
        empresaRepository.save(recarregada);

        mockMvc.perform(get("/dashboard").session(sessao))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/acesso-negado?motivo=BLOQUEADO"));
        assertTrue(sessao.isInvalid(), "A sessão existente deve ser invalidada.");
    }
}
