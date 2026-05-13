package com.fisiogestao.portal;

import com.fisiogestao.agenda.SessaoEntity;

/**
 * Visao de uma sessao agendada no portal do paciente.
 * Agrega a entidade com dados pre-computados para o template,
 * evitando logica complexa no Thymeleaf.
 */
public record SessaoPortalView(
        SessaoEntity sessao,
        String calendarUrl,
        boolean cancelavel,
        int horasAntecedencia
) {
}
