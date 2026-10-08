package com.fisiogestao.cadastro;

import com.fisiogestao.auth.UserRole;
import com.fisiogestao.auth.UsuarioSistemaEntity;
import com.fisiogestao.email.EmailLogType;
import com.fisiogestao.email.service.EmailRequest;
import com.fisiogestao.empresa.ConfiguracaoPlataformaService;
import com.fisiogestao.empresa.DiasUteisService;
import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.empresa.EmpresaStatus;
import com.fisiogestao.empresa.PeriodoTesteService;
import com.fisiogestao.gestor.NotificacaoGestorRepository;
import com.fisiogestao.gestor.TipoNotificacaoGestor;
import com.fisiogestao.support.MultiEmpresaTestBase;
import com.fisiogestao.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Requisitos 15, 18 e 19: cadastro público cria empresa em TESTE, ADMIN_CLINICA, alerta e e-mail. */
class CadastroClinicaTest extends MultiEmpresaTestBase {

    @Autowired private ConfiguracaoPlataformaService configuracaoPlataformaService;
    @Autowired private NotificacaoGestorRepository notificacaoGestorRepository;
    @Autowired private DiasUteisService diasUteisService;

    @AfterEach
    void restaurarPadrao() {
        configuracaoPlataformaService.salvar(15, 3, null);
    }

    @Test
    void cadastroPublicoCriaEmpresaEmTesteComAdministradorEPrazoPadraoConfiguravel() throws Exception {
        configuracaoPlataformaService.salvar(7, 3, "gestor@plataforma.test");
        String email = "dona." + UUID.randomUUID().toString().substring(0, 6) + "@fisio.test";

        mockMvc.perform(post("/cadastro").with(csrf()).with(ipAleatorio())
                        .param("nomeClinica", "Fisio Vida Plena")
                        .param("nomeResponsavel", "Dona Responsável")
                        .param("cpf", "529.982.247-25")
                        .param("email", email)
                        .param("telefone", "(67) 99999-1234")
                        .param("senha", SENHA)
                        .param("confirmacaoSenha", SENHA)
                        .param("aceiteTermos", "true")
                        .param("observacoes", "Quero testar a agenda."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cadastro/sucesso"));

        EmpresaEntity empresa = empresaRepository.findAll().stream()
                .filter(e -> email.equals(e.getEmail()))
                .findFirst().orElseThrow();
        assertEquals(EmpresaStatus.TESTE, empresa.getStatus());
        assertEquals(7, empresa.getQuantidadeDiasUteisTeste());
        assertNotNull(empresa.getDataFimTeste());
        assertEquals(diasUteisService.calcularDataFinal(PeriodoTesteService.hoje(), 7),
                empresa.getDataFimTeste().atZoneSameInstant(PeriodoTesteService.zonaPlataforma()).toLocalDate());
        assertTrue(empresa.getSlug().startsWith("fisio-vida-plena"));

        List<UsuarioSistemaEntity> equipe = usuarioRepository.findByEmpresaIdOrderByNomeAsc(empresa.getId());
        assertEquals(1, equipe.size());
        assertEquals(UserRole.ADMIN_CLINICA, equipe.getFirst().getRole());
        assertEquals(email, equipe.getFirst().getEmail());

        // Configuração operacional criada dentro da nova empresa.
        assertTrue(TenantContext.executarComo(empresa.getId(),
                () -> configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc()).isPresent());

        // Alerta no Gestor (requisito 18)
        assertTrue(notificacaoGestorRepository.findByTipoAndEmpresaId(TipoNotificacaoGestor.NOVO_CADASTRO, empresa.getId())
                .stream().findAny().isPresent());

        // Tentativa de e-mail administrativo (requisito 19)
        ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(systemEmailService, atLeastOnce()).sendPlataforma(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(r -> r.type() == EmailLogType.NOTIFICACAO_ADMINISTRATIVA
                && r.to().contains("gestor@plataforma.test")));

        // Acesso liberado imediatamente, sem acesso ao Gestor.
        var login = login(email, SENHA, empresa.getSlug());
        assertEquals("/dashboard", login.getResponse().getRedirectedUrl());
        mockMvc.perform(get("/gestor").session(sessao(login))).andExpect(status().isForbidden());
    }

    @Test
    void cnpjDuplicadoRecebeMensagemGenericaSemRevelarCadastroExistente() throws Exception {
        String cnpj = cnpjValidoAleatorio();
        criarEmpresa("ja-existe", EmpresaStatus.ATIVO);
        EmpresaEntity existente = empresaRepository.findAll().getLast();
        existente.setCnpj(cnpj);
        empresaRepository.save(existente);

        mockMvc.perform(post("/cadastro").with(csrf()).with(ipAleatorio())
                        .param("nomeClinica", "Outra Clínica")
                        .param("nomeResponsavel", "Fulano")
                        .param("cnpj", cnpj)
                        .param("email", "outra@fisio.test")
                        .param("telefone", "67999991234")
                        .param("senha", SENHA)
                        .param("confirmacaoSenha", SENHA)
                        .param("aceiteTermos", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(CadastroClinicaService.MENSAGEM_NAO_CONCLUIDO)));
    }

    @Test
    void cadastroExigeAceiteDosTermosEDocumentoValido() throws Exception {
        long antes = empresaRepository.count();
        mockMvc.perform(post("/cadastro").with(csrf()).with(ipAleatorio())
                        .param("nomeClinica", "Sem termos")
                        .param("nomeResponsavel", "Fulano")
                        .param("cpf", "111.111.111-11")
                        .param("email", "semtermos@fisio.test")
                        .param("telefone", "67999991234")
                        .param("senha", SENHA)
                        .param("confirmacaoSenha", SENHA))
                .andExpect(status().isOk());
        assertEquals(antes, empresaRepository.count());
    }

    @Test
    void cadastroSemCsrfEhRecusado() throws Exception {
        mockMvc.perform(post("/cadastro").with(ipAleatorio()).param("nomeClinica", "X"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired"));
    }
}
