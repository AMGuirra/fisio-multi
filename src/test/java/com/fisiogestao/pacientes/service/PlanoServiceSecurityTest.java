package com.fisiogestao.pacientes.service;

import com.fisiogestao.pacientes.dto.PlanoFormDTO;
import com.fisiogestao.shared.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import com.fisiogestao.support.WithUsuarioClinica;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Prova que os {@code @PreAuthorize} de PlanoService realmente funcionam.
 * Um teste puramente Mockito nunca passa pelo proxy AOP do Spring Security e
 * passaria mesmo se a anotação fosse removida — por isso este teste sobe o
 * contexto real da aplicação.
 */
@org.junit.jupiter.api.extension.ExtendWith(com.fisiogestao.support.EmpresaLegadaSetupExtension.class)
@SpringBootTest
@ActiveProfiles("test")
class PlanoServiceSecurityTest {

    @Autowired
    private PlanoService planoService;

    @Test
    @WithUsuarioClinica(roles = "ASSESSORIA")
    void assessoriaNaoPodeSalvarOuAtualizarPlano() {
        assertThrows(AccessDeniedException.class,
                () -> planoService.salvarOuAtualizar(UUID.randomUUID(), formPlano(), "Equipe"));
    }

    @Test
    @WithUsuarioClinica(roles = "RECEPCAO")
    void recepcaoPodeTentarSalvarOuAtualizarPlano() {
        assertThrows(BusinessException.class,
                () -> planoService.salvarOuAtualizar(UUID.randomUUID(), formPlano(), "Equipe"));
    }

    @Test
    @WithUsuarioClinica(roles = "ASSESSORIA")
    void assessoriaNaoPodeRenovarPlano() {
        assertThrows(AccessDeniedException.class,
                () -> planoService.renovarPlano(UUID.randomUUID(), formPlano(), "Equipe"));
    }

    @Test
    @WithUsuarioClinica(roles = "FISIOTERAPEUTA")
    void fisioterapeutaPodeTentarRenovarPlano() {
        assertThrows(BusinessException.class,
                () -> planoService.renovarPlano(UUID.randomUUID(), formPlano(), "Equipe"));
    }

    private PlanoFormDTO formPlano() {
        PlanoFormDTO form = new PlanoFormDTO();
        form.setTipoPlano("PLANO_10");
        form.setTotalSessoes(10);
        form.setValorTotal(new BigDecimal("500.00"));
        form.setInicioVigencia(LocalDate.now());
        form.setFimVigencia(LocalDate.now().plusMonths(1));
        return form;
    }
}
