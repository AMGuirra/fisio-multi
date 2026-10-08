package com.fisiogestao.suporte;

import com.fisiogestao.auth.UsuarioSistemaEntity;
import com.fisiogestao.email.EmailLogType;
import com.fisiogestao.email.service.EmailRequest;
import com.fisiogestao.empresa.ConfiguracaoPlataformaService;
import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.empresa.EmpresaStatus;
import com.fisiogestao.support.MultiEmpresaTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Requisitos 20 e 21: suporte público valida 500 caracteres, aparece no Gestor e notifica por e-mail. */
class SuportePublicoTest extends MultiEmpresaTestBase {

    @Autowired private SolicitacaoSuporteRepository repository;
    @Autowired private ConfiguracaoPlataformaService configuracaoPlataformaService;

    @AfterEach
    void restaurar() {
        configuracaoPlataformaService.salvar(15, 3, null);
    }

    @Test
    void descricaoAcimaDe500CaracteresEhRecusada() throws Exception {
        long antes = repository.count();
        mockMvc.perform(post("/suporte/solicitar").with(csrf()).with(ipAleatorio())
                        .param("nome", "Fulano")
                        .param("cpf", "52998224725")
                        .param("email", "fulano@x.test")
                        .param("descricao", "a".repeat(501)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("no máximo 500 caracteres")));
        assertEquals(antes, repository.count());
    }

    @Test
    void solicitacaoAparecesNoGestorEGeraNotificacaoPorEmail() throws Exception {
        configuracaoPlataformaService.salvar(15, 3, "suporte-admin@plataforma.test");
        EmpresaEntity empresa = criarEmpresa("suporte", EmpresaStatus.BLOQUEADO);
        String cnpj = cnpjValidoAleatorio();
        empresa.setCnpj(cnpj);
        empresaRepository.save(empresa);
        String marcador = "Não consigo entrar " + UUID.randomUUID();

        mockMvc.perform(post("/suporte/solicitar").with(csrf()).with(ipAleatorio())
                        .param("nome", "Responsável Bloqueado")
                        .param("cnpj", cnpj)
                        .param("email", "resp@clinica.test")
                        .param("descricao", "a".repeat(400) + marcador.substring(0, 50)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/suporte/solicitar/sucesso"));

        SolicitacaoSuporteEntity salva = repository.findAll().stream()
                .filter(s -> "resp@clinica.test".equals(s.getEmail())).findFirst().orElseThrow();
        assertEquals(empresa.getId(), salva.getEmpresaId(), "Empresa identificada pelo CNPJ.");
        assertEquals(StatusSolicitacaoSuporte.PENDENTE, salva.getStatus());

        UsuarioSistemaEntity superAdmin = usuarioRepository.findByEmpresaIdIsNullAndUsername("superadmin").orElseThrow();
        mockMvc.perform(get("/gestor/suporte").with(comoUsuario(superAdmin)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Responsável Bloqueado")));
        mockMvc.perform(get("/gestor/suporte/" + salva.getId()).with(comoUsuario(superAdmin)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(empresa.getNome())));

        ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(systemEmailService, atLeastOnce()).sendPlataforma(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(r -> r.type() == EmailLogType.NOTIFICACAO_ADMINISTRATIVA
                && r.to().contains("suporte-admin@plataforma.test")));

        mockMvc.perform(post("/gestor/suporte/" + salva.getId() + "/status").with(csrf()).with(comoUsuario(superAdmin))
                        .param("status", "EM_ANALISE").param("observacao", "Verificando"))
                .andExpect(status().is3xxRedirection());
        assertEquals(StatusSolicitacaoSuporte.EM_ANALISE, repository.findById(salva.getId()).orElseThrow().getStatus());
    }

    @Test
    void paginasPublicasDeSuporteEAcessoNegadoAbremSemLogin() throws Exception {
        mockMvc.perform(get("/suporte/solicitar")).andExpect(status().isOk());
        mockMvc.perform(get("/suporte/cancelamento")).andExpect(status().isOk());
        mockMvc.perform(get("/cadastro")).andExpect(status().isOk());
        mockMvc.perform(get("/acesso-negado").param("motivo", "INATIVO")).andExpect(status().isOk());
    }
}
