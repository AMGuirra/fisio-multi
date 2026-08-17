package com.fisiogestao.config;

import com.fisiogestao.auth.InMemoryUserCatalog;
import com.fisiogestao.auth.UserMenuAccessService;
import com.fisiogestao.auth.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserMenuAccessInterceptorSecurityTest {

    @Mock
    private InMemoryUserCatalog userCatalog;

    @Mock
    private UserMenuAccessService accessService;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void apiSemPermissaoDeveResponder403EmVezDeRedirecionar() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("usuario-limitado", "N/A", List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(userCatalog.buscarCredenciais("usuario-limitado")).thenReturn(Optional.of(
                new InMemoryUserCatalog.SystemUserCredentials("usuario-limitado", "[HASH_REDACTED]", UserRole.RECEPCAO)
        ));
        when(accessService.podeAcessar(UserRole.RECEPCAO, "/api/pacientes/por-cpf")).thenReturn(false);
        when(accessService.resolverDestinoInicial(UserRole.RECEPCAO)).thenReturn("/dashboard");

        var interceptor = new UserMenuAccessInterceptor(userCatalog, accessService);
        var request = new MockHttpServletRequest("GET", "/api/pacientes/por-cpf");
        var response = new MockHttpServletResponse();
        HandlerMethod handler = new HandlerMethod(this, getClass().getDeclaredMethod("handlerFicticio"));

        assertFalse(interceptor.preHandle(request, response, handler));
        assertEquals(403, response.getStatus());
    }

    @SuppressWarnings("unused")
    private void handlerFicticio() {
    }
}
