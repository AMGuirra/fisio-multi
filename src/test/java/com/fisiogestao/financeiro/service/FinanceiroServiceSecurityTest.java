package com.fisiogestao.financeiro.service;

import com.fisiogestao.financeiro.MetodoPagamento;
import com.fisiogestao.shared.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Prova que os {@code @PreAuthorize} de FinanceiroService realmente funcionam.
 * Um teste puramente Mockito nunca passa pelo proxy AOP do Spring Security e
 * passaria mesmo se a anotação fosse removida — por isso este teste sobe o
 * contexto real da aplicação.
 */
@SpringBootTest
@ActiveProfiles("test")
class FinanceiroServiceSecurityTest {

    @Autowired
    private FinanceiroService financeiroService;

    @Test
    @WithMockUser(roles = "FISIOTERAPEUTA")
    void fisioterapeutaNaoPodeCancelarLancamento() {
        assertThrows(AccessDeniedException.class, () -> financeiroService.cancelar(UUID.randomUUID()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPodeTentarCancelarLancamento() {
        assertThrows(BusinessException.class, () -> financeiroService.cancelar(UUID.randomUUID()));
    }

    @Test
    @WithMockUser(roles = "RECEPCAO")
    void recepcaoNaoPodeMarcarComoPago() {
        assertThrows(AccessDeniedException.class,
                () -> financeiroService.marcarComoPago(UUID.randomUUID(), MetodoPagamento.PIX));
    }

    @Test
    @WithMockUser(roles = "FINANCEIRO")
    void financeiroPodeTentarMarcarComoPago() {
        assertThrows(BusinessException.class,
                () -> financeiroService.marcarComoPago(UUID.randomUUID(), MetodoPagamento.PIX));
    }

    @Test
    @WithMockUser(roles = "ASSESSORIA")
    void assessoriaNaoPodeCancelarBaixa() {
        assertThrows(AccessDeniedException.class, () -> financeiroService.cancelarBaixa(UUID.randomUUID()));
    }

    @Test
    @WithMockUser(roles = "FISIOTERAPEUTA")
    void fisioterapeutaNaoPodeReabrirLancamento() {
        assertThrows(AccessDeniedException.class, () -> financeiroService.reabrir(UUID.randomUUID()));
    }

    @Test
    @WithMockUser(roles = "FISIOTERAPEUTA")
    void fisioterapeutaNaoPodeVerResumoFinanceiro() {
        assertThrows(AccessDeniedException.class,
                () -> financeiroService.calcularResumo(LocalDate.now().minusMonths(1), LocalDate.now()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPodeVerResumoFinanceiro() {
        assertDoesNotThrow(() -> financeiroService.calcularResumo(LocalDate.now().minusMonths(1), LocalDate.now()));
    }

    @Test
    @WithMockUser(roles = "RECEPCAO")
    void recepcaoNaoPodeListarContasAReceber() {
        assertThrows(AccessDeniedException.class, financeiroService::listarContasAReceber);
    }

    @Test
    @WithMockUser(roles = "RECEPCAO")
    void recepcaoNaoPodeListarContasAPagar() {
        assertThrows(AccessDeniedException.class, financeiroService::listarContasAPagar);
    }

    @Test
    @WithMockUser(roles = "RECEPCAO")
    void recepcaoNaoPodeGerarRelatorioFaturamento() {
        assertThrows(AccessDeniedException.class,
                () -> financeiroService.gerarRelatorioFaturamento(LocalDate.now().minusMonths(1), LocalDate.now()));
    }
}
