package com.fisiogestao.auth;

import com.fisiogestao.shared.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class UserMenuAccessServiceTest {

    @Mock
    private UserRoleMenuPermissionRepository permissionRepository;

    @InjectMocks
    private UserMenuAccessService userMenuAccessService;

    @Test
    void deveCriarPermissoesPadraoQuandoFuncaoNaoTemConfiguracao() {
        when(permissionRepository.findByRoleOrderByMenuKeyAsc(UserRole.RECEPCAO)).thenReturn(List.of());

        EnumSet<AppMenuItem> menus = userMenuAccessService.listarMenusLiberados(UserRole.RECEPCAO);

        assertEquals(
                EnumSet.of(
                        AppMenuItem.DASHBOARD,
                        AppMenuItem.AGENDA,
                        AppMenuItem.BLOQUEIOS,
                        AppMenuItem.SOLICITACOES,
                        AppMenuItem.PLANOS_SESSOES,
                        AppMenuItem.PACIENTES,
                        AppMenuItem.AGENDAMENTO_ONLINE
                ),
                menus
        );

        ArgumentCaptor<List<UserRoleMenuPermissionEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(permissionRepository).saveAll(captor.capture());
        assertEquals(menus.size(), captor.getValue().size());
    }

    @Test
    void sidebarAdminNaoExibeListaDeEsperaSeparada() {
        List<UserMenuAccessService.SidebarNavItem> itens = userMenuAccessService.montarSidebarNavItems(
                UserRole.ADMIN_CLINICA,
                "/solicitacoes"
        );

        List<String> labels = itens.stream()
                .filter(item -> !item.section())
                .map(UserMenuAccessService.SidebarNavItem::label)
                .toList();

        assertTrue(labels.contains("Solicitações"));
    }

    @Test
    void sidebarAdminExibeSubmenusFinanceiros() {
        List<UserMenuAccessService.SidebarNavItem> itens = userMenuAccessService.montarSidebarNavItems(
                UserRole.ADMIN_CLINICA,
                "/financeiro/contas-a-receber"
        );

        List<String> labels = itens.stream()
                .filter(item -> !item.section())
                .map(UserMenuAccessService.SidebarNavItem::label)
                .toList();

        assertTrue(labels.contains("Financeiro"));
        assertTrue(labels.contains("Contas a Receber"));
        assertTrue(labels.contains("Contas a Pagar"));
        // "Faturamento" foi unificado dentro da Central de Relatórios (aba "Faturamento detalhado")
        // e não é mais um item de sidebar separado; a permissão FATURAMENTO continua existindo
        // e controlando o acesso a essa aba.
        assertTrue(labels.stream().noneMatch(label -> label.equals("Faturamento")));
        assertTrue(labels.stream().anyMatch(label -> label.startsWith("Relat")));
    }

    @Test
    void sidebarAdminExibeBloqueiosAbaixoDeConfiguracaoClinica() {
        List<UserMenuAccessService.SidebarNavItem> itens = userMenuAccessService.montarSidebarNavItems(
                UserRole.ADMIN_CLINICA,
                "/bloqueios"
        );

        List<String> labels = itens.stream()
                .filter(item -> !item.section())
                .map(UserMenuAccessService.SidebarNavItem::label)
                .toList();

        int indiceConfigClinica = labels.stream()
                .filter(label -> label.startsWith("Config. Cl"))
                .findFirst()
                .map(labels::indexOf)
                .orElse(-1);
        int indiceBloqueios = labels.indexOf("Bloqueios");

        assertTrue(indiceConfigClinica >= 0);
        assertEquals(indiceConfigClinica + 1, indiceBloqueios);
    }

    @Test
    void perfilFinanceiroIncluiSubmenusFinanceirosPorPadrao() {
        when(permissionRepository.findByRoleOrderByMenuKeyAsc(UserRole.FINANCEIRO)).thenReturn(List.of());

        EnumSet<AppMenuItem> menus = userMenuAccessService.listarMenusLiberados(UserRole.FINANCEIRO);

        assertTrue(menus.contains(AppMenuItem.FINANCEIRO));
        assertTrue(menus.contains(AppMenuItem.CONTAS_RECEBER));
        assertTrue(menus.contains(AppMenuItem.CONTAS_PAGAR));
        assertTrue(menus.contains(AppMenuItem.FATURAMENTO));
    }

    @Test
    void deveTratarPermissaoLegadaDeCalendarioComoAgendaUnificada() {
        when(permissionRepository.findByRoleOrderByMenuKeyAsc(UserRole.RECEPCAO)).thenReturn(List.of(
                UserRoleMenuPermissionEntity.builder().role(UserRole.RECEPCAO).menuKey(AppMenuItem.CALENDARIO).build()
        ));

        EnumSet<AppMenuItem> menus = userMenuAccessService.listarMenusLiberados(UserRole.RECEPCAO);

        assertEquals(EnumSet.of(AppMenuItem.AGENDA), menus);
    }

    @Test
    void deveTratarPermissaoLegadaDeFaturamentoSemRelatoriosComoDependente() {
        when(permissionRepository.findByRoleOrderByMenuKeyAsc(UserRole.FINANCEIRO)).thenReturn(List.of(
                UserRoleMenuPermissionEntity.builder().role(UserRole.FINANCEIRO).menuKey(AppMenuItem.FATURAMENTO).build()
        ));

        EnumSet<AppMenuItem> menus = userMenuAccessService.listarMenusLiberados(UserRole.FINANCEIRO);

        assertTrue(menus.contains(AppMenuItem.FATURAMENTO));
        assertTrue(menus.contains(AppMenuItem.RELATORIOS));
    }

    @Test
    void salvarPermissoesAdicionaRelatoriosAutomaticamenteQuandoFaturamentoConcedido() {
        userMenuAccessService.salvarPermissoes(UserRole.FINANCEIRO, EnumSet.of(AppMenuItem.FATURAMENTO));

        ArgumentCaptor<List<UserRoleMenuPermissionEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(permissionRepository).saveAll(captor.capture());

        List<AppMenuItem> menusSalvos = captor.getValue().stream()
                .map(UserRoleMenuPermissionEntity::getMenuKey)
                .toList();

        assertTrue(menusSalvos.contains(AppMenuItem.FATURAMENTO));
        assertTrue(menusSalvos.contains(AppMenuItem.RELATORIOS));
    }

    @Test
    void deveImpedirSalvarFuncaoSemMenuInterno() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userMenuAccessService.salvarPermissoes(UserRole.FINANCEIRO, EnumSet.of(AppMenuItem.AGENDAMENTO_ONLINE))
        );

        assertEquals("Selecione ao menos um menu interno para Financeiro.", ex.getMessage());
        verify(permissionRepository, never()).saveAll(anyList());
    }

    @Test
    void rotaInternaDesconhecidaDeveFalharFechada() {
        assertEquals(false, userMenuAccessService.podeAcessar(UserRole.RECEPCAO, "/api/rota-nao-mapeada"));
    }

    @Test
    void APIsDePacientesEAgendamentosDevemResolverParaSeusMenus() {
        assertEquals(Optional.of(AppMenuItem.PACIENTES), AppMenuItem.fromPath("/api/pacientes/por-cpf"));
        assertEquals(Optional.of(AppMenuItem.AGENDA), AppMenuItem.fromPath("/api/agendamentos/recorrentes"));
    }
}
