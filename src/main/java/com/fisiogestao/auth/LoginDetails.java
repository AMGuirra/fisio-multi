package com.fisiogestao.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.io.Serial;
import java.util.Locale;

/** Detalhes do formulário de login: além de IP/sessão, o código (slug) da clínica informado. */
public class LoginDetails extends WebAuthenticationDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String PARAM_CLINICA = "clinica";
    private static final String SLUG_INVALIDO = "#invalido";

    private final String clinicaSlug;

    public LoginDetails(HttpServletRequest request) {
        super(request);
        this.clinicaSlug = normalizarSlug(request.getParameter(PARAM_CLINICA));
    }

    public String getClinicaSlug() {
        return clinicaSlug;
    }

    /**
     * Normaliza o código da clínica. Retorna {@code null} quando vazio e um marcador que nunca
     * corresponde a uma empresa quando o formato é inválido.
     */
    public static String normalizarSlug(String valor) {
        if (valor == null) {
            return null;
        }
        String slug = valor.trim().toLowerCase(Locale.ROOT);
        if (slug.isEmpty()) {
            return null;
        }
        if (slug.length() > 80 || !slug.matches("[a-z0-9-]+")) {
            return SLUG_INVALIDO;
        }
        return slug;
    }
}
