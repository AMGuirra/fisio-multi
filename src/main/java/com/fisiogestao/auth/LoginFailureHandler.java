package com.fisiogestao.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Falha de login: credenciais inválidas voltam ao formulário com mensagem genérica;
 * credenciais válidas de clínica sem acesso vão para a tela explicativa de acesso negado.
 */
public class LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        if (exception instanceof EmpresaAcessoNegadoException negado) {
            response.sendRedirect(request.getContextPath() + "/acesso-negado?motivo=" + negado.getMotivo().name());
            return;
        }
        UriComponentsBuilder destino = UriComponentsBuilder.fromPath(request.getContextPath() + "/login")
                .queryParam("error");
        String slug = LoginDetails.normalizarSlug(request.getParameter(LoginDetails.PARAM_CLINICA));
        if (slug != null && !slug.startsWith("#")) {
            destino.queryParam(LoginDetails.PARAM_CLINICA, slug);
        }
        response.sendRedirect(destino.build().toUriString());
    }
}
