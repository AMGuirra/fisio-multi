package com.fisiogestao.gestor;

import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.form.AuditFilterForm;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.auth.UserRole;
import com.fisiogestao.auth.UsuarioSistemaEntity;
import com.fisiogestao.empresa.DiasUteisService;
import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.empresa.EmpresaStatus;
import com.fisiogestao.empresa.PeriodoTesteService;
import com.fisiogestao.gestor.form.EmpresaForm;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.support.MultiEmpresaTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Requisitos 16 e 17: superadmin define, prorroga, reduz e reinicia o teste em dias úteis. */
class GestorEmpresaTesteTest extends MultiEmpresaTestBase {

    @Autowired private EmpresaGestaoService empresaGestaoService;
    @Autowired private DiasUteisService diasUteisService;
    @Autowired private AuditService auditService;

    private UsuarioSistemaEntity superAdmin;

    @BeforeEach
    void autenticar() {
        superAdmin = usuarioRepository.findByEmpresaIdIsNullAndUsername("superadmin").orElseThrow();
        autenticarComo(superAdmin);
    }

    private LocalDate fim(EmpresaEntity empresa) {
        return empresa.getDataFimTeste().atZoneSameInstant(PeriodoTesteService.zonaPlataforma()).toLocalDate();
    }

    @Test
    void superadminDefineQualquerQuantidadeDeDiasUteisAoCriar() {
        EmpresaForm form = new EmpresaForm();
        form.setNome("Clínica Gestor " + System.nanoTime());
        form.setStatus(EmpresaStatus.TESTE);
        form.setQuantidadeDiasUteisTeste(42);
        EmpresaEntity criada = empresaGestaoService.criar(form);

        assertEquals(EmpresaStatus.TESTE, criada.getStatus());
        assertEquals(42, criada.getQuantidadeDiasUteisTeste());
        assertEquals(diasUteisService.calcularDataFinal(PeriodoTesteService.hoje(), 42), fim(criada));
    }

    @Test
    void superadminProrrogaReduzEReiniciaOTeste() {
        EmpresaEntity empresa = criarEmpresa("gestao-teste", EmpresaStatus.ATIVO);

        EmpresaEntity emTeste = empresaGestaoService.iniciarTeste(empresa.getId(), 10);
        LocalDate inicio = emTeste.getDataInicioTeste().atZoneSameInstant(PeriodoTesteService.zonaPlataforma()).toLocalDate();
        assertEquals(EmpresaStatus.TESTE, emTeste.getStatus());
        assertEquals(diasUteisService.calcularDataFinal(inicio, 10), fim(emTeste));

        EmpresaEntity prorrogada = empresaGestaoService.prorrogarTeste(empresa.getId(), 5);
        assertEquals(15, prorrogada.getQuantidadeDiasUteisTeste());
        assertEquals(diasUteisService.calcularDataFinal(inicio, 15), fim(prorrogada));

        EmpresaEntity reduzida = empresaGestaoService.alterarDiasTeste(empresa.getId(), 3);
        assertEquals(3, reduzida.getQuantidadeDiasUteisTeste());
        assertEquals(diasUteisService.calcularDataFinal(inicio, 3), fim(reduzida));

        LocalDate novaData = diasUteisService.calcularDataFinal(inicio, 20);
        EmpresaEntity manual = empresaGestaoService.definirDataFinalTeste(empresa.getId(), novaData);
        assertEquals(novaData, fim(manual));
        assertEquals(20, manual.getQuantidadeDiasUteisTeste());
        assertThrows(BusinessException.class, () -> empresaGestaoService.definirDataFinalTeste(empresa.getId(), inicio.minusDays(1)));

        EmpresaEntity reiniciada = empresaGestaoService.iniciarTeste(empresa.getId(), 1);
        assertEquals(1, reiniciada.getQuantidadeDiasUteisTeste());
        assertEquals(diasUteisService.calcularDataFinal(PeriodoTesteService.hoje(), 1), fim(reiniciada));

        // Toda alteração de prazo/status fica na auditoria da plataforma.
        AuditFilterForm filtro = new AuditFilterForm();
        filtro.setRegistroAfetado(empresa.getId().toString());
        var eventos = auditService.list(filtro);
        assertTrue(eventos.size() >= 5);
        assertTrue(eventos.stream().allMatch(e -> e.getModulo() == AuditModule.GESTOR && e.getEmpresaId() == null));
    }

    @Test
    void bloqueioEInativacaoExigemMotivoEAtivacaoVoltaALiberar() {
        EmpresaEntity empresa = criarEmpresa("bloqueio", EmpresaStatus.ATIVO);
        assertThrows(BusinessException.class, () -> empresaGestaoService.bloquear(empresa.getId(), " "));
        assertEquals(EmpresaStatus.BLOQUEADO, empresaGestaoService.bloquear(empresa.getId(), "Pendência").getStatus());
        assertEquals(EmpresaStatus.ATIVO, empresaGestaoService.ativar(empresa.getId(), null).getStatus());
        assertThrows(BusinessException.class, () -> empresaGestaoService.inativar(empresa.getId(), null));
    }

    @Test
    void usuarioDeClinicaNaoExecutaAcoesDoGestor() {
        EmpresaEntity empresa = criarEmpresa("sem-permissao", EmpresaStatus.ATIVO);
        autenticarComo(criarUsuario(empresa, "admin-local", UserRole.ADMIN_CLINICA));
        assertThrows(AccessDeniedException.class, () -> empresaGestaoService.iniciarTeste(empresa.getId(), 30));
    }
}
