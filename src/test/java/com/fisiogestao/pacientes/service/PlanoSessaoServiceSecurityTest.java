package com.fisiogestao.pacientes.service;

import com.fisiogestao.pacientes.PlanoSessaoStatus;
import com.fisiogestao.pacientes.form.PlanoConsumoForm;
import com.fisiogestao.pacientes.form.PlanoSessaoForm;
import com.fisiogestao.shared.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Prova que os {@code @PreAuthorize} de PlanoSessaoService realmente funcionam.
 * Um teste puramente Mockito nunca passa pelo proxy AOP do Spring Security e
 * passaria mesmo se a anotação fosse removida — por isso este teste sobe o
 * contexto real da aplicação.
 */
@SpringBootTest
@ActiveProfiles("test")
class PlanoSessaoServiceSecurityTest {

    @Autowired
    private PlanoSessaoService planoSessaoService;

    @Test
    @WithMockUser(roles = "ASSESSORIA")
    void assessoriaNaoPodeCriarPlano() {
        assertThrows(AccessDeniedException.class,
                () -> planoSessaoService.criar(UUID.randomUUID(), formPlano()));
    }

    @Test
    @WithMockUser(roles = "RECEPCAO")
    void recepcaoPodeTentarCriarPlano() {
        assertThrows(BusinessException.class,
                () -> planoSessaoService.criar(UUID.randomUUID(), formPlano()));
    }

    @Test
    @WithMockUser(roles = "ASSESSORIA")
    void assessoriaNaoPodeCriarPlanoViaCrud() {
        assertThrows(AccessDeniedException.class,
                () -> planoSessaoService.criarViaCrud(formConsumo(), "Equipe"));
    }

    @Test
    @WithMockUser(roles = "FISIOTERAPEUTA")
    void fisioterapeutaPodeTentarCriarPlanoViaCrud() {
        assertThrows(BusinessException.class,
                () -> planoSessaoService.criarViaCrud(formConsumo(), "Equipe"));
    }

    @Test
    @WithMockUser(roles = "ASSESSORIA")
    void assessoriaNaoPodeCancelarPlano() {
        assertThrows(AccessDeniedException.class, () -> planoSessaoService.cancelar(UUID.randomUUID()));
    }

    @Test
    @WithMockUser(roles = "RECEPCAO")
    void recepcaoPodeTentarCancelarPlano() {
        assertThrows(BusinessException.class, () -> planoSessaoService.cancelar(UUID.randomUUID()));
    }

    @Test
    @WithMockUser(roles = "ASSESSORIA")
    void assessoriaNaoPodeExecutarReconciliacao() {
        assertThrows(AccessDeniedException.class, () -> planoSessaoService.executarReconciliacao(UUID.randomUUID()));
    }

    @Test
    @WithMockUser(roles = "FISIOTERAPEUTA")
    void fisioterapeutaNaoPodeExcluirPlano() {
        assertThrows(AccessDeniedException.class, () -> planoSessaoService.excluir(UUID.randomUUID()));
    }

    @Test
    @WithMockUser(roles = "FINANCEIRO")
    void financeiroNaoPodeExcluirPlano() {
        assertThrows(AccessDeniedException.class, () -> planoSessaoService.excluir(UUID.randomUUID()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPodeTentarExcluirPlano() {
        assertThrows(BusinessException.class, () -> planoSessaoService.excluir(UUID.randomUUID()));
    }

    private PlanoSessaoForm formPlano() {
        PlanoSessaoForm form = new PlanoSessaoForm();
        form.setNomePlano("Plano de teste");
        form.setSessoesContratadas(10);
        form.setValorTotal(new BigDecimal("500.00"));
        form.setDataInicio(LocalDate.now());
        form.setDataValidade(LocalDate.now().plusMonths(1));
        return form;
    }

    private PlanoConsumoForm formConsumo() {
        PlanoConsumoForm form = new PlanoConsumoForm();
        form.setClienteId(UUID.randomUUID());
        form.setNomePlano("Plano de teste");
        form.setSessoesContratadas(10);
        form.setSessoesConsumidas(0);
        form.setValorTotal(new BigDecimal("500.00"));
        form.setDataInicio(LocalDate.now());
        form.setStatus(PlanoSessaoStatus.ATIVO);
        return form;
    }
}
