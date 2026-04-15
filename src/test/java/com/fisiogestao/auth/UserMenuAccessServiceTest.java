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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
                        AppMenuItem.PACOTES_SESSOES,
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
    void deveTratarPermissaoLegadaDeCalendarioComoAgendaUnificada() {
        when(permissionRepository.findByRoleOrderByMenuKeyAsc(UserRole.RECEPCAO)).thenReturn(List.of(
                UserRoleMenuPermissionEntity.builder().role(UserRole.RECEPCAO).menuKey(AppMenuItem.CALENDARIO).build()
        ));

        EnumSet<AppMenuItem> menus = userMenuAccessService.listarMenusLiberados(UserRole.RECEPCAO);

        assertEquals(EnumSet.of(AppMenuItem.AGENDA), menus);
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
}
